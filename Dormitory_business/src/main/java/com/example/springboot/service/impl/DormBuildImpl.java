package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.DormBuild;
import com.example.springboot.mapper.DormBuildMapper;
import com.example.springboot.service.DormBuildService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


@Service
public class DormBuildImpl extends ServiceImpl<DormBuildMapper, DormBuild> implements DormBuildService {


    /**
     * 注入DAO层对象
     */
    @Resource
    private DormBuildMapper dormBuildMapper;
    @Resource private com.example.springboot.security.AccessScope scope;
    @Resource private com.example.springboot.service.AccommodationGuard accommodation;
    @Resource private org.springframework.jdbc.core.JdbcTemplate jdbc;

    /**
     * 楼宇添加
     */
    @Override
    public int addNewBuilding(DormBuild dormBuild) {
        int insert = dormBuildMapper.insert(dormBuild);
        return insert;
    }

    /**
     * 楼宇查找
     */
    @Override
    public Page find(Integer pageNum, Integer pageSize, String search) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<DormBuild> qw = new QueryWrapper<>();
        qw.like("DormBuild_id", search);
        scope.rooms(qw);
        Page buildingPage = dormBuildMapper.selectPage(page, qw);
        return buildingPage;
    }

    /**
     * 楼宇信息更新
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public int updateNewBuilding(DormBuild dormBuild) {
        accommodation.lock();DormBuild old=dormBuildMapper.selectById(dormBuild.getId());
        com.example.springboot.service.AccommodationGuard.conflict(old!=null,"楼宇不存在");
        if(old.getDormBuildId()!=dormBuild.getDormBuildId())requireNoReferences(old.getDormBuildId());
        int i = dormBuildMapper.updateById(dormBuild);
        return i;
    }

    /**
     * 楼宇删除
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public int deleteBuilding(Integer id) {
        accommodation.lock();DormBuild old=dormBuildMapper.selectById(id);
        com.example.springboot.service.AccommodationGuard.conflict(old!=null,"楼宇不存在");requireNoReferences(old.getDormBuildId());
        int i = dormBuildMapper.deleteById(id);
        return i;
    }

    private void requireNoReferences(int building) {
        for(String table:new String[]{"dorm_room","dorm_manager","repair","visitor"})
            com.example.springboot.service.AccommodationGuard.conflict(jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE dormbuild_id=?",Integer.class,building)==0,"楼宇仍有关联记录，不能删除或变更编号");
    }

    /**
     * 首页 获取建筑名称
     */
    @Override
    public List<DormBuild> getBuildingId() {
        QueryWrapper<DormBuild> qw = new QueryWrapper<>();
        qw.select("dormbuild_id");
        scope.rooms(qw);
        qw.orderByAsc("dormbuild_id");
        List<DormBuild> dormBuilds = dormBuildMapper.selectList(qw);
        return dormBuilds;
    }

}
