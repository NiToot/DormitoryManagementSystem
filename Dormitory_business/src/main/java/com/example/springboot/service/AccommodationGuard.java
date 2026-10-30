package com.example.springboot.service;

import com.example.springboot.entity.DormRoom;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Component
public class AccommodationGuard {
    private final JdbcTemplate jdbc;
    public AccommodationGuard(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public void lock() {
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Accommodation writes require a transaction");
        // One database row coordinates all application instances, and is released on commit/rollback.
        jdbc.queryForObject("SELECT id FROM accommodation_lock WHERE id=1 FOR UPDATE",Integer.class);
    }
    public static void conflict(boolean condition,String message) {
        if(!condition)throw new ResponseStatusException(HttpStatus.CONFLICT,message);
    }
    public static void version(Long expected,Long actual) {
        conflict(expected!=null && expected.equals(actual),"记录已变化或缺少版本，请刷新后重试");
    }
    public static String bed(DormRoom room,int number) {
        switch(number) {case 1:return room.getFirstBed();case 2:return room.getSecondBed();case 3:return room.getThirdBed();case 4:return room.getFourthBed();default:throw new IllegalArgumentException("Invalid bed");}
    }
    public static void bed(DormRoom room,int number,String value) {
        switch(number) {case 1:room.setFirstBed(value);break;case 2:room.setSecondBed(value);break;case 3:room.setThirdBed(value);break;case 4:room.setFourthBed(value);break;default:throw new IllegalArgumentException("Invalid bed");}
    }
    public static int count(DormRoom room) {int n=0;for(int i=1;i<=4;i++)if(bed(room,i)!=null)n++;return n;}
    public void validate(DormRoom room,Collection<Integer> excludedRooms) {
        if(room.getDormRoomId()==null || room.getDormRoomId()<1 || room.getFloorNum()<1 || room.getMaxCapacity()<1 || room.getMaxCapacity()>4)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"房间、楼层和容量参数无效，容量须为1至4");
        conflict(jdbc.queryForObject("SELECT COUNT(*) FROM dorm_build WHERE dormbuild_id=?",Integer.class,room.getDormBuildId())==1,"楼宇不存在");
        Set<String> students=new HashSet<>();
        for(int i=1;i<=4;i++) {
            String username=bed(room,i);
            if(username==null || username.trim().isEmpty()){bed(room,i,null);continue;}
            List<String> canonical=jdbc.queryForList("SELECT username FROM student WHERE username=?",String.class,username.trim());
            conflict(canonical.size()==1,"床位学生不存在");username=canonical.get(0);bed(room,i,username);
            conflict(i<=room.getMaxCapacity(),"床位超出房间容量");
            conflict(students.add(username),"同一学生不能重复入住");
            List<Object> args=new ArrayList<>(Arrays.asList(username,username,username,username));args.addAll(excludedRooms);
            String exclusion=excludedRooms.isEmpty()?"":" AND dormroom_id NOT IN ("+String.join(",",Collections.nCopies(excludedRooms.size(),"?"))+")";
            Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM dorm_room WHERE (first_bed=? OR second_bed=? OR third_bed=? OR fourth_bed=?)"+exclusion,Integer.class,args.toArray());
            conflict(count==0,"该学生已有床位，不能重复入住");
        }
        room.setCurrentCapacity(count(room));
    }
    public void persistRoom(DormRoom room) {
        int changed=jdbc.update("UPDATE dorm_room SET dormbuild_id=?,floor_num=?,max_capacity=?,current_capacity=?,first_bed=?,second_bed=?,third_bed=?,fourth_bed=?,version=version+1 WHERE dormroom_id=? AND version=?",
                room.getDormBuildId(),room.getFloorNum(),room.getMaxCapacity(),count(room),room.getFirstBed(),room.getSecondBed(),room.getThirdBed(),room.getFourthBed(),room.getDormRoomId(),room.getVersion());
        conflict(changed==1,"房间已变化，请刷新后重试");room.setVersion(room.getVersion()+1);
    }
    public void requireNoPendingRoom(int roomId) {
        conflict(jdbc.queryForObject("SELECT COUNT(*) FROM adjust_room WHERE state='未处理' AND (currentroom_id=? OR towardsroom_id=?)",Integer.class,roomId,roomId)==0,"房间存在待处理调宿，不能删除或改变楼宇");
    }
}
