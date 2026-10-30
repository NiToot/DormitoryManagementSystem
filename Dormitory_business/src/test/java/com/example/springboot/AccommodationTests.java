package com.example.springboot;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.example.springboot.SecurityClient.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class AccommodationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    MockHttpSession admin;
    @BeforeEach void fixture() throws Exception {
        assertEquals("dormitory_demo",jdbc.queryForObject("SELECT DATABASE()",String.class));
        jdbc.update("INSERT INTO student(username,password,name,age) SELECT '99980001',password,'住宿测试甲',20 FROM student WHERE username='20230001'");
        jdbc.update("INSERT INTO student(username,password,name,age) SELECT '99980002',password,'住宿测试乙',20 FROM student WHERE username='20230001'");
        jdbc.update("INSERT INTO dorm_room(dormroom_id,dormbuild_id,floor_num,max_capacity,current_capacity,first_bed) VALUES(9800,1,9,4,1,'99980001'),(9801,1,9,4,0,NULL)");
        admin=login(mvc,"admin","admin01");
    }
    String room(String first,String second,int count,long version) {
        return "{\"dormRoomId\":9801,\"dormBuildId\":1,\"floorNum\":9,\"maxCapacity\":4,\"currentCapacity\":"+count+",\"firstBed\":\""+first+"\",\"secondBed\":"+(second==null?"null":"\""+second+"\"")+",\"version\":"+version+"}";
    }
    @Test void studentCannotOccupyTwoRooms() throws Exception {
        write(mvc,put("/room/update"),room("99980001",null,1,0),admin).andExpect(status().isConflict());
        assertNull(jdbc.queryForObject("SELECT first_bed FROM dorm_room WHERE dormroom_id=9801",String.class));
    }
    @Test void studentCannotOccupyTwoBedsInSameRoom() throws Exception {
        write(mvc,put("/room/update"),room("99980002","99980002",2,0),admin).andExpect(status().isConflict());
    }
    @Test void capacityIsDerivedFromBeds() throws Exception {
        write(mvc,put("/room/update"),room("99980002",null,4,0),admin).andExpect(status().isOk());
        assertEquals(1,jdbc.queryForObject("SELECT current_capacity FROM dorm_room WHERE dormroom_id=9801",Integer.class));
    }
    @Test void occupiedRoomCannotBeDeleted() throws Exception {
        write(mvc,delete("/room/delete/9800").param("version","0"),"{}",admin).andExpect(status().isConflict());
    }
    @Test void staleRoomSnapshotCannotOverwriteNewOccupancy() throws Exception {
        write(mvc,put("/room/update"),room("99980002",null,1,0),admin).andExpect(status().isOk());
        write(mvc,put("/room/update"),room("99980002",null,1,0),admin).andExpect(status().isConflict());
    }
    @Test void studentCanHaveOnlyOnePendingAdjustment() throws Exception {
        MockHttpSession student=login(mvc,"stu","99980001");
        String body="{\"towardsRoomId\":9801,\"towardsBedId\":1}";
        write(mvc,post("/adjustRoom/add"),body,student).andExpect(status().isOk());
        write(mvc,post("/adjustRoom/add"),body,student).andExpect(status().isConflict());
    }
    @Test void occupiedBedCannotBeOverwrittenByNormalUpdate() throws Exception {
        String replacement=room("99980002",null,1,0).replace("9801","9800");
        write(mvc,put("/room/update"),replacement,admin).andExpect(status().isConflict());
        assertEquals("99980001",jdbc.queryForObject("SELECT first_bed FROM dorm_room WHERE dormroom_id=9800",String.class));
    }
    @Test void checkoutIgnoresClientCountAndRepeatedCheckoutCannotGoNegative() throws Exception {
        write(mvc,delete("/room/delete/first_bed/9800/999").param("version","0"),"{}",admin).andExpect(status().isOk());
        assertEquals(0,jdbc.queryForObject("SELECT current_capacity FROM dorm_room WHERE dormroom_id=9800",Integer.class));
        write(mvc,delete("/room/delete/first_bed/9800/999").param("version","0"),"{}",admin).andExpect(status().isConflict());
        write(mvc,delete("/room/delete/first_bed/9800/999").param("version","1"),"{}",admin).andExpect(status().isConflict());
        assertEquals(0,jdbc.queryForObject("SELECT current_capacity FROM dorm_room WHERE dormroom_id=9800",Integer.class));
    }
    @Test void nonexistentStudentAndCaseVariantDuplicatesAreRejected() throws Exception {
        write(mvc,put("/room/update"),room("not-a-student",null,1,0),admin).andExpect(status().isConflict());
        jdbc.update("INSERT INTO student(username,password,name,age) SELECT 'RoomCase',password,'大小写测试',20 FROM student WHERE username='20230001'");
        write(mvc,put("/room/update"),room("RoomCase","roomcase",2,0),admin).andExpect(status().isConflict());
    }
    @Test void occupiedStudentAndReferencedBuildingCannotBeDeleted() throws Exception {
        write(mvc,delete("/stu/delete/99980001"),"{}",admin).andExpect(status().isConflict());
        Integer building=jdbc.queryForObject("SELECT id FROM dorm_build WHERE dormbuild_id=1",Integer.class);
        write(mvc,delete("/building/delete/"+building),"{}",admin).andExpect(status().isConflict());
    }
    @Test void missingVersionAndOutOfRangeBedAreRejected() throws Exception {
        write(mvc,put("/room/update"),room("99980002",null,1,0).replace(",\"version\":0",""),admin).andExpect(status().isConflict());
        jdbc.update("UPDATE dorm_room SET max_capacity=1 WHERE dormroom_id=9801");
        write(mvc,put("/room/update"),"{\"dormRoomId\":9801,\"dormBuildId\":1,\"floorNum\":9,\"maxCapacity\":1,\"version\":0,\"secondBed\":\"99980002\"}",admin).andExpect(status().isConflict());
    }
}
