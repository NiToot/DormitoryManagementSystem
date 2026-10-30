package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.Student;
import com.example.springboot.mapper.StudentMapper;
import com.example.springboot.service.StudentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    /**
     * 注入DAO层对象
     */
    @Resource
    private StudentMapper studentMapper;
    @Resource private com.example.springboot.security.Accounts accounts;
    @Resource private com.example.springboot.security.AccessScope scope;
    @Resource private com.example.springboot.service.AccommodationGuard accommodation;
    @Resource private org.springframework.jdbc.core.JdbcTemplate jdbc;

    /**
     * 学生登陆
     */
    @Override
    public Student stuLogin(String username, String password) {
        QueryWrapper<Student> qw = new QueryWrapper<>();
        qw.eq("username", username);
        Student student = studentMapper.selectOne(qw);
        if (student != null && accounts.matches(password, student.getPassword())) {
            return student;
        } else {
            return null;
        }
    }

    /**
     * 学生新增
     */
    @Override
    public int addNewStudent(Student student) {
        student.setPassword(accounts.encode(student.getPassword()));
        student.setAvatar(null);
        int insert = studentMapper.insert(student);
        return insert;
    }

    /**
     * 分页查询学生
     */
    @Override
    public Page find(Integer pageNum, Integer pageSize, String search) {
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<Student> qw = new QueryWrapper<>();
        qw.like("name", search);
        scope.students(qw);
        Page studentPage = studentMapper.selectPage(page, qw);
        return studentPage;
    }

    /**
     * 更新学生信息
     */
    @Override
    public int updateNewStudent(Student student) {
        scope.studentRecord(student.getUsername());
        if (student.getPassword()!=null && !student.getPassword().isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"请通过独立密码接口修改密码");
        student.setPassword(null);
        student.setAvatar(null);
        int i = studentMapper.updateById(student);
        return i;
    }

    /**
     * 删除学生信息
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public int deleteStudent(String username) {
        accommodation.lock();
        com.example.springboot.service.AccommodationGuard.conflict(jdbc.queryForObject("SELECT COUNT(*) FROM dorm_room WHERE first_bed=? OR second_bed=? OR third_bed=? OR fourth_bed=?",Integer.class,username,username,username,username)==0,"学生尚未退宿，不能删除账号");
        com.example.springboot.service.AccommodationGuard.conflict(jdbc.queryForObject("SELECT COUNT(*) FROM adjust_room WHERE username=? AND state='未处理'",Integer.class,username)==0,"学生有待处理调宿，不能删除账号");
        int i = studentMapper.deleteById(username);
        return i;
    }


    /**
     * 主页顶部：学生统计
     */
    @Override
    public int stuNum() {
        QueryWrapper<Student> qw = new QueryWrapper<>();
        qw.isNotNull("username");
        scope.students(qw);
        int stuNum = Math.toIntExact(studentMapper.selectCount(qw));
        return stuNum;
    }

    /**
     * 床位信息，查询该学生信息
     */
    @Override
    public Student stuInfo(String username) {
        scope.studentRecord(username);
        QueryWrapper<Student> qw = new QueryWrapper<>();
        qw.eq("username", username);
        Student student = studentMapper.selectOne(qw);
        return student;
    }
}
