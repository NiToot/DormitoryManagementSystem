package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.Admin;
import com.example.springboot.mapper.AdminMapper;
import com.example.springboot.service.AdminService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    /**
     * 注入DAO层对象
     */
    @Resource
    private AdminMapper adminMapper;
    @Resource private com.example.springboot.security.Accounts accounts;
    @Resource private com.example.springboot.security.AccessScope scope;

    /**
     * 管理员登录
     */
    @Override
    public Admin adminLogin(String username, String password) {
        QueryWrapper<Admin> qw = new QueryWrapper<>();
        qw.eq("username", username);
        Admin admin = adminMapper.selectOne(qw);
        if (admin != null && accounts.matches(password, admin.getPassword())) {
            return admin;
        } else {
            return null;
        }
    }

    /**
     * 管理员信息更新
     */
    @Override
    public int updateAdmin(Admin admin) {
        if (admin.getPassword()!=null && !admin.getPassword().isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"请通过独立密码接口修改密码");
        admin.setPassword(null);
        admin.setAvatar(null);
        int i = adminMapper.updateById(admin);
        return i;
    }

}
