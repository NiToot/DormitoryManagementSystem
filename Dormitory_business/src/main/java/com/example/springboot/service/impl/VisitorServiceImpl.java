package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.Visitor;
import com.example.springboot.mapper.VisitorMapper;
import com.example.springboot.service.VisitorService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class VisitorServiceImpl extends ServiceImpl<VisitorMapper, Visitor> implements VisitorService {

    @Resource
    private VisitorMapper visitorMapper;
    @Resource private com.example.springboot.security.AccessScope scope;
    @Resource private org.springframework.jdbc.core.JdbcTemplate jdbc;

    /**
     * 访客添加
     */
    @Override
    public int addNewVisitor(Visitor visitor) {
        visitor.setId(null);
        if(!scope.admin()) {
            if(visitor.getDormBuildId()!=null)scope.ownBuilding(visitor.getDormBuildId());
            visitor.setDormBuildId(scope.building());
        }
        requireBuilding(visitor.getDormBuildId());
        int insert = visitorMapper.insert(visitor);
        return insert;
    }

    /**
     * 访客查询
     */
    @Override
    public Page find(Integer pageNum, Integer pageSize, String search) {
//        return visitorMapper.selectPage(new Page<>(pageNum, pageSize), new QueryWrapper<Visitor>().like("name", search));
        Page newPage = new Page<>(pageNum, pageSize);
        QueryWrapper<Visitor> qw = new QueryWrapper<>();
        qw.like("name", search);
        scope.rooms(qw);
        Page visitorPage = visitorMapper.selectPage(newPage, qw);
        return visitorPage;
    }

    /**
     * 访客信息更新
     */
    @Override
    public int updateNewVisitor(Visitor visitor) {
        Visitor old=visitorMapper.selectById(visitor.getId());scope.require(old!=null);scope.ownBuilding(old.getDormBuildId());
        if(!scope.admin())visitor.setDormBuildId(old.getDormBuildId());
        requireBuilding(visitor.getDormBuildId());
        int i = visitorMapper.updateById(visitor);
        return i;
    }

    /**
     * 访客删除
     */
    @Override
    public int deleteVisitor(Integer id) {
        Visitor old=visitorMapper.selectById(id);scope.require(old!=null);scope.ownBuilding(old.getDormBuildId());
        int i = visitorMapper.deleteById(id);
        return i;
    }

    private void requireBuilding(Integer id) {
        if(id==null || jdbc.queryForObject("SELECT COUNT(*) FROM dorm_build WHERE dormbuild_id=?",Integer.class,id)==0)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"请选择有效的来访楼宇");
    }
}
