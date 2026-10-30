package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.*;
import com.example.springboot.mapper.*;
import com.example.springboot.security.*;
import com.example.springboot.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import javax.annotation.Resource;
import java.util.*;
import static com.example.springboot.service.AccommodationGuard.*;

@Service
public class AdjustRoomServiceImpl extends ServiceImpl<AdjustRoomMapper,AdjustRoom> implements AdjustRoomService {
    @Resource private AdjustRoomMapper requests;
    @Resource private DormRoomMapper rooms;
    @Resource private AccessScope scope;
    @Resource private Accounts accounts;
    @Resource private AccommodationGuard guard;

    @Override @Transactional(isolation=Isolation.READ_COMMITTED)
    public int addApply(AdjustRoom request) {
        guard.lock();scope.require(scope.student());
        if(request.getUsername()!=null)scope.require(request.getUsername().equals(scope.user().username));
        conflict(requests.selectCount(new QueryWrapper<AdjustRoom>().eq("username",scope.user().username).eq("state","未处理"))==0,"已有待处理调宿申请，请编辑原申请");
        DormRoom current=scope.myRoom();conflict(current!=null,"当前没有有效床位");
        request.setId(null);request.setVersion(0L);request.setUsername(scope.user().username);
        request.setName((String)accounts.profile().get("name"));request.setCurrentRoomId(current.getDormRoomId());
        for(int i=1;i<=4;i++)if(request.getUsername().equals(bed(current,i)))request.setCurrentBedId(i);
        source(request);target(request);
        request.setState("未处理");request.setApplyTime(java.time.LocalDateTime.now().toString());request.setFinishTime(null);
        return requests.insert(request);
    }
    @Override public Page find(Integer page,Integer size,String search) {
        QueryWrapper<AdjustRoom> query=new QueryWrapper<>();query.like("username",search);scope.adjustments(query);
        return requests.selectPage(new Page<>(page,size),query);
    }
    @Override @Transactional(isolation=Isolation.READ_COMMITTED)
    public int updateApply(AdjustRoom input,Boolean approved) {
        guard.lock();AdjustRoom stored=requests.selectById(input.getId());scope.require(stored!=null);
        if(scope.student())scope.require(scope.user().username.equals(stored.getUsername()) && !approved && "未处理".equals(input.getState()));
        else {
            scope.room(stored.getCurrentRoomId());scope.room(stored.getTowardsRoomId());
            scope.require((approved && "通过".equals(input.getState())) || (!approved && "驳回".equals(input.getState())));
        }
        version(input.getVersion(),stored.getVersion());conflict("未处理".equals(stored.getState()),"申请已处理，请刷新后重试");
        if(scope.student()) {
            source(stored);stored.setTowardsRoomId(input.getTowardsRoomId());stored.setTowardsBedId(input.getTowardsBedId());target(stored);
        } else {
            if(approved) {
                DormRoom from=source(stored),to=target(stored);
                if(from.getDormRoomId().equals(to.getDormRoomId()))to=from;
                bed(from,stored.getCurrentBedId(),null);bed(to,stored.getTowardsBedId(),stored.getUsername());
                List<Integer> changed=Arrays.asList(from.getDormRoomId(),to.getDormRoomId());
                guard.validate(from,changed);if(from!=to)guard.validate(to,changed);
                guard.persistRoom(from);if(from!=to)guard.persistRoom(to);
            }
            stored.setState(input.getState());stored.setFinishTime(java.time.LocalDateTime.now().toString());
        }
        stored.setVersion(stored.getVersion()+1);
        conflict(requests.updateById(stored)==1,"申请写入失败");return 1;
    }
    @Override @Transactional(isolation=Isolation.READ_COMMITTED)
    public int deleteAdjustment(Integer id,Long expected) {
        guard.lock();AdjustRoom stored=requests.selectById(id);scope.require(stored!=null);
        scope.room(stored.getCurrentRoomId());scope.room(stored.getTowardsRoomId());version(expected,stored.getVersion());
        conflict("未处理".equals(stored.getState()),"已审批申请保留用于追溯，不能删除");
        return requests.deleteById(id);
    }
    private DormRoom source(AdjustRoom request) {
        DormRoom room=rooms.selectById(request.getCurrentRoomId());
        conflict(room!=null && request.getCurrentBedId()>=1 && request.getCurrentBedId()<=4,"原床位已失效");
        conflict(request.getUsername().equals(bed(room,request.getCurrentBedId())),"原床位已变化，请重新申请");
        guard.validate(room,Collections.singleton(room.getDormRoomId()));return room;
    }
    private DormRoom target(AdjustRoom request) {
        DormRoom room=rooms.selectById(request.getTowardsRoomId());int n=request.getTowardsBedId();
        conflict(room!=null && n>=1 && n<=room.getMaxCapacity() && n<=4,"目标房间或床位无效");
        conflict(bed(room,n)==null,"目标床位已被占用，请刷新后重试");
        guard.validate(room,Collections.singleton(room.getDormRoomId()));return room;
    }
}
