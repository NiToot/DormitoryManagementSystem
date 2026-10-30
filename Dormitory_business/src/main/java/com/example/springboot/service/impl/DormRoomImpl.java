package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.JudgeBedName;
import com.example.springboot.entity.DormRoom;
import com.example.springboot.mapper.DormRoomMapper;
import com.example.springboot.service.DormRoomService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import com.example.springboot.service.AccommodationGuard;
import com.example.springboot.security.AccessScope;
import org.springframework.transaction.annotation.*;
import java.util.*;
import static com.example.springboot.service.AccommodationGuard.*;


@Service
public class DormRoomImpl extends ServiceImpl<DormRoomMapper, DormRoom> implements DormRoomService {

    @Resource
    private DormRoomMapper dormRoomMapper;
    @Resource private AccessScope scope;
    @Resource private AccommodationGuard guard;

    /**
     * 首页顶部：空宿舍统计
     */
    @Override
    public int notFullRoom() {
        QueryWrapper<DormRoom> qw = new QueryWrapper<>();
        qw.apply("current_capacity < max_capacity");
        scope.rooms(qw);
        int notFullRoomNum = Math.toIntExact(dormRoomMapper.selectCount(qw));
        return notFullRoomNum;
    }

    /**
     * 添加房间
     */
    @Override
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public int addNewRoom(DormRoom room) {
        guard.lock();room.setVersion(0L);guard.validate(room,Collections.emptyList());
        return dormRoomMapper.insert(room);
    }

    /**
     * 查找房间
     */
    @Override
    public Page find(Integer pageNum, Integer pageSize, String search) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<DormRoom> qw = new QueryWrapper<>();
        qw.like("Dormroom_id", search);
        scope.rooms(qw);
        Page roomPage = dormRoomMapper.selectPage(page, qw);
        return roomPage;
    }

    /**
     * 更新房间
     */
    @Override
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public int updateNewRoom(DormRoom room) {
        guard.lock();DormRoom old=scope.room(room.getDormRoomId());
        scope.require(scope.admin() || old.getDormBuildId()==room.getDormBuildId());
        version(room.getVersion(),old.getVersion());
        guard.validate(room,Collections.singleton(room.getDormRoomId()));
        for(int i=1;i<=4;i++) {
            String before=bed(old,i),after=bed(room,i);
            conflict(before==null || Objects.equals(before,after),"已占床位不能直接替换，请先退宿或办理调宿");
            if(after!=null && !scope.admin())scope.studentRecord(after);
        }
        if(old.getDormBuildId()!=room.getDormBuildId()) {
            conflict(AccommodationGuard.count(old)==0,"已入住房间不能改变所属楼宇");guard.requireNoPendingRoom(old.getDormRoomId());
        }
        guard.persistRoom(room);return 1;
    }

    /**
     * 删除房间
     */
    @Override
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public int deleteRoom(Integer id,Long version) {
        guard.lock();DormRoom room=scope.room(id);
        version(version,room.getVersion());
        conflict(AccommodationGuard.count(room)==0,"已入住房间不能删除");
        guard.requireNoPendingRoom(id);return dormRoomMapper.deleteById(id);
    }

    /**
     * 删除床位上的学生信息
     */
    @Override
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public int deleteBedInfo(String bedName,Integer id,int ignoredCount,Long version) {
        guard.lock();List<String> names=Arrays.asList("first_bed","second_bed","third_bed","fourth_bed");scope.require(names.contains(bedName));
        DormRoom room=scope.room(id);version(version,room.getVersion());int number=names.indexOf(bedName)+1;
        conflict(bed(room,number)!=null,"该床位已空，请刷新后重试");
        bed(room,number,null);guard.persistRoom(room);return 1;
    }

    /**
     * 床位信息，查询该学生是否已由床位
     */
    @Override
    public DormRoom judgeHadBed(String username) {
        QueryWrapper qw = new QueryWrapper();
        qw.eq("first_bed", username);
        qw.or();
        qw.eq("second_bed", username);
        qw.or();
        qw.eq("third_bed", username);
        qw.or();
        qw.eq("fourth_bed", username);
        DormRoom dormRoom = dormRoomMapper.selectOne(qw);
        return dormRoom;
    }

    /**
     * 主页 住宿人数
     */
    @Override
    public Long selectHaveRoomStuNum() {
        QueryWrapper<DormRoom> qw1 = new QueryWrapper<>();
        scope.rooms(qw1);
        qw1.isNotNull("first_bed");
        Long first_bed = dormRoomMapper.selectCount(qw1);

        QueryWrapper<DormRoom> qw2 = new QueryWrapper<>();
        scope.rooms(qw2);
        qw2.isNotNull("second_bed");
        Long second_bed = dormRoomMapper.selectCount(qw2);

        QueryWrapper<DormRoom> qw3 = new QueryWrapper<>();
        scope.rooms(qw3);
        qw3.isNotNull("third_bed");
        Long third_bed = dormRoomMapper.selectCount(qw3);

        QueryWrapper<DormRoom> qw4 = new QueryWrapper<>();
        scope.rooms(qw4);
        qw4.isNotNull("fourth_bed");
        Long fourth_bed = dormRoomMapper.selectCount(qw4);

        Long count = first_bed + second_bed + third_bed + fourth_bed;
        return count;
    }

    /**
     * 获取每栋宿舍学生总人数
     */
    @Override
    public Long getEachBuildingStuNum(int dormBuildId) {

        QueryWrapper<DormRoom> qw1 = new QueryWrapper<>();
        qw1.eq("dormbuild_id", dormBuildId);
        scope.rooms(qw1);
        qw1.isNotNull("first_bed");
        Long first_bed = dormRoomMapper.selectCount(qw1);

        QueryWrapper<DormRoom> qw2 = new QueryWrapper<>();
        qw2.eq("dormbuild_id", dormBuildId);
        scope.rooms(qw2);
        qw2.isNotNull("second_bed");
        Long second_bed = dormRoomMapper.selectCount(qw2);

        QueryWrapper<DormRoom> qw3 = new QueryWrapper<>();
        qw3.eq("dormbuild_id", dormBuildId);
        scope.rooms(qw3);
        qw3.isNotNull("third_bed");
        Long third_bed = dormRoomMapper.selectCount(qw3);

        QueryWrapper<DormRoom> qw4 = new QueryWrapper<>();
        qw4.eq("dormbuild_id", dormBuildId);
        scope.rooms(qw4);
        qw4.isNotNull("fourth_bed");
        Long fourth_bed = dormRoomMapper.selectCount(qw4);

        Long count = first_bed + second_bed + third_bed + fourth_bed;

        return count;
    }

    /**
     * 检查该房间是否满了
     */
    @Override
    public DormRoom checkRoomState(Integer dormRoomId) {
        QueryWrapper qw = new QueryWrapper();
        qw.eq("dormroom_id", dormRoomId);
        qw.apply("current_capacity < max_capacity");
        DormRoom dormRoom = dormRoomMapper.selectOne(qw);
        return dormRoom;
    }

    /**
     * 检查该房间是否存在
     */
    @Override
    public DormRoom checkRoomExist(Integer dormRoomId) {
        DormRoom dormRoom = dormRoomMapper.selectById(dormRoomId);
        return dormRoom;
    }


    /**
     * 检查床位是否有人
     */
    @Override
    public DormRoom checkBedState(Integer dormRoomId, int bedNum) {
        QueryWrapper qw = new QueryWrapper();
        qw.eq("dormroom_id", dormRoomId);
        qw.isNull(JudgeBedName.getBedName(bedNum));
        qw.ge("max_capacity",bedNum);
        DormRoom dormRoom = dormRoomMapper.selectOne(qw);
        return dormRoom;
    }


}
