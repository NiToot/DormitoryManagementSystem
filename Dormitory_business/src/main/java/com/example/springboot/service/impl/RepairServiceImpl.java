package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.Repair;
import com.example.springboot.mapper.RepairMapper;
import com.example.springboot.service.RepairService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;


@Service
public class RepairServiceImpl extends ServiceImpl<RepairMapper, Repair> implements RepairService {

    /**
     * 注入DAO层对象
     */
    @Resource
    private RepairMapper repairMapper;
    @Resource private com.example.springboot.security.AccessScope scope;
    @Resource private com.example.springboot.security.Accounts accounts;

    /**
     * 添加订单
     */
    @Override
    public int addNewOrder(Repair repair) {
        scope.require(scope.student());
        com.example.springboot.entity.DormRoom room=scope.myRoom();
        scope.require(room!=null);
        if(repair.getStudentUsername()!=null)scope.require(repair.getStudentUsername().equals(scope.user().username));
        scope.require(repair.getDormRoomId()==0 || repair.getDormRoomId()==room.getDormRoomId());
        repair.setId(null);repair.setStudentUsername(scope.user().username);repair.setRepairer((String)accounts.profile().get("name"));
        repair.setDormBuildId(room.getDormBuildId());repair.setDormRoomId(room.getDormRoomId());
        repair.setState("未完成");repair.setOrderBuildTime(java.time.LocalDateTime.now().toString());repair.setOrderFinishTime(null);
        int insert = repairMapper.insert(repair);
        return insert;
    }

    /**
     * 查找订单
     */
    @Override
    public Page find(Integer pageNum, Integer pageSize, String search) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<Repair> qw = new QueryWrapper<>();
        qw.like("title", search);
        scope.repairs(qw);
        Page orderPage = repairMapper.selectPage(page, qw);
        return orderPage;
    }

    @Override
    public Page individualFind(Integer pageNum, Integer pageSize, String search, String name) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<Repair> qw = new QueryWrapper<>();
        qw.like("title", search);
        scope.repairs(qw);
        if(!scope.student())qw.eq("repairer", name);
        Page orderPage = repairMapper.selectPage(page, qw);
        return orderPage;
    }

    /**
     * 更新订单
     */
    @Override
    public int updateNewOrder(Repair repair) {
        Repair old=repairMapper.selectById(repair.getId());scope.require(old!=null);
        if(!scope.admin()){scope.ownBuilding(old.getDormBuildId());scope.require(old.getStudentUsername()!=null);}
        repair.setStudentUsername(old.getStudentUsername());repair.setRepairer(old.getRepairer());repair.setDormBuildId(old.getDormBuildId());repair.setDormRoomId(old.getDormRoomId());
        int i = repairMapper.updateById(repair);
        Assert.notNull(i, "保修单为空");
        return i;
    }

    /**
     * 删除订单
     */
    @Override
    public int deleteOrder(Integer id) {
        Repair old=repairMapper.selectById(id);scope.require(old!=null);
        if(!scope.admin()){scope.ownBuilding(old.getDormBuildId());scope.require(old.getStudentUsername()!=null);}
        int i = repairMapper.deleteById(id);
        Assert.notNull(i, "保修单为空");
        return i;
    }

    /**
     * 首页顶部：报修统计
     */
    @Override
    public int showOrderNum() {
        QueryWrapper<Repair> qw = new QueryWrapper<>();
        scope.repairs(qw);
        int orderCount = Math.toIntExact(repairMapper.selectCount(qw));
        return orderCount;
    }
}
