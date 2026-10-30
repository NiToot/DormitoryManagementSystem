package com.example.springboot.service;

import com.example.springboot.mapper.DormRoomMapper;
import com.example.springboot.entity.DormRoom;
import org.springframework.boot.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.util.Collections;

@Component
public class AccommodationPreflight implements ApplicationRunner {
    private final AccommodationGuard guard;
    private final DormRoomMapper rooms;
    private final JdbcTemplate jdbc;
    public AccommodationPreflight(AccommodationGuard guard,DormRoomMapper rooms,JdbcTemplate jdbc) {this.guard=guard;this.rooms=rooms;this.jdbc=jdbc;}
    @Override @Transactional(isolation=Isolation.READ_COMMITTED)
    public void run(ApplicationArguments args) {
        guard.lock();
        for(DormRoom room:rooms.selectList(null)) {
            int stored=room.getCurrentCapacity();guard.validate(room,Collections.singleton(room.getDormRoomId()));
            AccommodationGuard.conflict(stored==room.getCurrentCapacity(),"历史房间人数不一致，请停机核实并修正");
        }
        AccommodationGuard.conflict(jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT username FROM adjust_room WHERE state='未处理' GROUP BY username HAVING COUNT(*)>1) duplicated",Integer.class)==0,"历史数据存在重复待处理申请，请人工核实");
    }
}
