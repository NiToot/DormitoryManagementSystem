package com.example.springboot.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.springboot.entity.DormRoom;
import com.example.springboot.mapper.DormRoomMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AccessScope {
    private final Accounts accounts;
    private final JdbcTemplate jdbc;
    private final DormRoomMapper rooms;
    public AccessScope(Accounts accounts,JdbcTemplate jdbc,DormRoomMapper rooms) { this.accounts=accounts;this.jdbc=jdbc;this.rooms=rooms; }
    public SessionUser user() { return accounts.current(); }
    public boolean admin() { return user().role.equals("admin"); }
    public boolean student() { return user().role.equals("stu"); }
    public Integer building() {
        if(!user().role.equals("dormManager"))throw new AccessDeniedException("不是宿管");
        return ((Number)accounts.find("dormManager",user().username).get("dormbuild_id")).intValue();
    }
    public void require(boolean allowed) { if(!allowed)throw new AccessDeniedException("无权操作该数据"); }
    public void ownBuilding(Integer id) { if(!admin())require(id!=null && id.equals(building())); }
    public <T> void rooms(QueryWrapper<T> wrapper) { if(!admin() && !student())wrapper.eq("dormbuild_id",building()); }
    public <T> void students(QueryWrapper<T> wrapper) {
        if(student())wrapper.eq("username",user().username);
        else if(!admin())wrapper.and(q->q.inSql("username","SELECT first_bed FROM dorm_room WHERE dormbuild_id="+building())
                .or().inSql("username","SELECT second_bed FROM dorm_room WHERE dormbuild_id="+building())
                .or().inSql("username","SELECT third_bed FROM dorm_room WHERE dormbuild_id="+building())
                .or().inSql("username","SELECT fourth_bed FROM dorm_room WHERE dormbuild_id="+building()));
    }
    public void studentRecord(String username) {
        if(admin())return;
        if(student()){require(user().username.equals(username));return;}
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM dorm_room WHERE dormbuild_id=? AND (? IN (first_bed,second_bed,third_bed,fourth_bed))",Integer.class,building(),username);
        require(count!=null && count>0);
    }
    public DormRoom room(Integer id) {
        DormRoom room=rooms.selectById(id);
        require(room!=null);
        if(!admin())ownBuilding(room.getDormBuildId());
        return room;
    }
    public DormRoom myRoom() {
        String username=user().username;
        return rooms.selectOne(new QueryWrapper<DormRoom>().eq("first_bed",username).or().eq("second_bed",username).or().eq("third_bed",username).or().eq("fourth_bed",username));
    }
    public <T> void repairs(QueryWrapper<T> wrapper) {
        if(student())wrapper.eq("student_username",user().username);
        else if(!admin())wrapper.eq("dormbuild_id",building()).isNotNull("student_username");
    }
    public <T> void adjustments(QueryWrapper<T> wrapper) {
        if(student())wrapper.eq("username",user().username);
        else if(!admin())wrapper.inSql("currentroom_id","SELECT dormroom_id FROM dorm_room WHERE dormbuild_id="+building());
    }
}
