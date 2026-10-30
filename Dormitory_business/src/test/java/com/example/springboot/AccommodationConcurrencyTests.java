package com.example.springboot;

import com.example.springboot.entity.DormRoom;
import com.example.springboot.service.AccommodationGuard;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static com.example.springboot.SecurityClient.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.argThat;

@SpringBootTest @AutoConfigureMockMvc
class AccommodationConcurrencyTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @SpyBean AccommodationGuard guard;
    @Autowired com.example.springboot.service.AccommodationPreflight preflight;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    MockHttpSession admin,anotherAdmin;
    @BeforeEach void fixtures() throws Exception {
        assertEquals("dormitory_demo",jdbc.queryForObject("SELECT DATABASE()",String.class));
        for(int n=1;n<=4;n++)jdbc.update("INSERT INTO student(username,password,name,age) SELECT ?,password,?,20 FROM student WHERE username='20230001'","9997000"+n,"并发测试"+n);
        jdbc.update("INSERT INTO dorm_room(dormroom_id,dormbuild_id,floor_num,max_capacity,current_capacity,first_bed) VALUES(9700,1,9,4,1,'99970001'),(9701,1,9,4,1,'99970002'),(9702,1,9,4,0,NULL),(9703,1,9,4,0,NULL)");
        admin=login(mvc,"admin","admin01");anotherAdmin=login(mvc,"admin","admin01");
    }
    @AfterEach void cleanup() {
        reset(guard);
        jdbc.update("DELETE FROM adjust_room WHERE username IN ('99970001','99970002','99970003','99970004')");
        jdbc.update("DELETE FROM dorm_room WHERE dormroom_id IN (9700,9701,9702,9703)");
        jdbc.update("DELETE FROM student WHERE username IN ('99970001','99970002','99970003','99970004')");
    }
    void application(int id,String username,int from,int to,int bed) {
        jdbc.update("INSERT INTO adjust_room(id,username,name,currentroom_id,currentbed_id,towardsroom_id,towardsbed_id,state) VALUES(?,?,?, ?,1,?,?,'未处理')",id,username,"并发测试",from,to,bed);
    }
    int approve(int id,MockHttpSession session) throws Exception {
        return write(mvc,put("/adjustRoom/update/true"),"{\"id\":"+id+",\"version\":0,\"state\":\"通过\"}",session).andReturn().getResponse().getStatus();
    }
    String assign(int room,String student) {
        return "{\"dormRoomId\":"+room+",\"dormBuildId\":1,\"floorNum\":9,\"maxCapacity\":4,\"version\":0,\"firstBed\":\""+student+"\"}";
    }
    void race(Callable<Integer> first,Callable<Integer> second) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
        List<Future<Integer>> futures=new ArrayList<>();
        try {
            for(Callable<Integer> work:Arrays.asList(first,second))futures.add(pool.submit(()->{ready.countDown();assertTrue(start.await(5,TimeUnit.SECONDS));return work.call();}));
            assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
            List<Integer> outcomes=Arrays.asList(futures.get(0).get(20,TimeUnit.SECONDS),futures.get(1).get(20,TimeUnit.SECONDS));
            Collections.sort(outcomes);assertEquals(Arrays.asList(200,409),outcomes);
        } finally {start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(25,TimeUnit.SECONDS));}
    }
    int count(String sql) {return jdbc.queryForObject(sql,Integer.class);}
    void assertOneOccupancyPerStudent() {
        assertEquals(0,count("SELECT COUNT(*) FROM (SELECT username FROM (SELECT first_bed username FROM dorm_room UNION ALL SELECT second_bed FROM dorm_room UNION ALL SELECT third_bed FROM dorm_room UNION ALL SELECT fourth_bed FROM dorm_room) beds WHERE username IS NOT NULL GROUP BY username HAVING COUNT(*)>1) duplicates"));
        assertEquals(0,count("SELECT COUNT(*) FROM dorm_room WHERE current_capacity<>(first_bed IS NOT NULL)+(second_bed IS NOT NULL)+(third_bed IS NOT NULL)+(fourth_bed IS NOT NULL)"));
    }
    @Test void concurrentAssignmentsOfSameStudentToDifferentRoomsHaveOneWinner() throws Exception {
        race(()->write(mvc,put("/room/update"),assign(9702,"99970003"),admin).andReturn().getResponse().getStatus(),
                ()->write(mvc,put("/room/update"),assign(9703,"99970003"),anotherAdmin).andReturn().getResponse().getStatus());
        assertEquals(1,count("SELECT COUNT(*) FROM dorm_room WHERE first_bed='99970003'"));assertOneOccupancyPerStudent();
    }
    @Test void concurrentAssignmentsOfDifferentStudentsToSameBedHaveOneWinner() throws Exception {
        race(()->write(mvc,put("/room/update"),assign(9702,"99970003"),admin).andReturn().getResponse().getStatus(),
                ()->write(mvc,put("/room/update"),assign(9702,"99970004"),anotherAdmin).andReturn().getResponse().getStatus());
        assertEquals(1,count("SELECT current_capacity FROM dorm_room WHERE dormroom_id=9702"));assertOneOccupancyPerStudent();
    }
    @Test void concurrentApprovalOfSameRequestMovesStudentOnce() throws Exception {
        application(9710,"99970001",9700,9702,1);
        race(()->approve(9710,admin),()->approve(9710,anotherAdmin));
        assertNull(jdbc.queryForObject("SELECT first_bed FROM dorm_room WHERE dormroom_id=9700",String.class));
        assertEquals("99970001",jdbc.queryForObject("SELECT first_bed FROM dorm_room WHERE dormroom_id=9702",String.class));
        assertEquals(1,count("SELECT version FROM adjust_room WHERE id=9710"));assertOneOccupancyPerStudent();
    }
    @Test void concurrentApprovalsCompetingForSameBedPreserveLosersOriginalBed() throws Exception {
        application(9710,"99970001",9700,9702,1);application(9711,"99970002",9701,9702,1);
        race(()->approve(9710,admin),()->approve(9711,anotherAdmin));
        assertEquals(1,count("SELECT COUNT(*) FROM adjust_room WHERE id IN (9710,9711) AND state='通过'"));
        assertEquals(1,count("SELECT COUNT(*) FROM dorm_room WHERE dormroom_id IN (9700,9701) AND first_bed IS NOT NULL"));assertOneOccupancyPerStudent();
    }
    @Test void concurrentApprovalAndStudentEditUseSameRequestVersion() throws Exception {
        application(9710,"99970001",9700,9702,1);MockHttpSession student=login(mvc,"stu","99970001");
        race(()->approve(9710,admin),()->write(mvc,put("/adjustRoom/update/false"),"{\"id\":9710,\"version\":0,\"state\":\"未处理\",\"towardsRoomId\":9703,\"towardsBedId\":1}",student).andReturn().getResponse().getStatus());
        assertEquals(1,count("SELECT version FROM adjust_room WHERE id=9710"));
        String state=jdbc.queryForObject("SELECT state FROM adjust_room WHERE id=9710",String.class);
        if(state.equals("通过"))assertEquals("99970001",jdbc.queryForObject("SELECT first_bed FROM dorm_room WHERE dormroom_id=9702",String.class));
        else {assertEquals(9703,count("SELECT towardsroom_id FROM adjust_room WHERE id=9710"));assertEquals("99970001",jdbc.queryForObject("SELECT first_bed FROM dorm_room WHERE dormroom_id=9700",String.class));}
        assertOneOccupancyPerStudent();
    }
    @Test void concurrentPendingApplicationsAreSerialized() throws Exception {
        MockHttpSession first=login(mvc,"stu","99970001"),second=login(mvc,"stu","99970001");
        race(()->write(mvc,post("/adjustRoom/add"),"{\"towardsRoomId\":9702,\"towardsBedId\":1}",first).andReturn().getResponse().getStatus(),
                ()->write(mvc,post("/adjustRoom/add"),"{\"towardsRoomId\":9703,\"towardsBedId\":1}",second).andReturn().getResponse().getStatus());
        assertEquals(1,count("SELECT COUNT(*) FROM adjust_room WHERE username='99970001' AND state='未处理'"));
    }
    @Test void failureAfterBothRoomWritesRollsBackRoomsAndRequest() throws Exception {
        application(9710,"99970001",9700,9702,1);
        doAnswer(call->{call.callRealMethod();throw new org.springframework.dao.DataAccessResourceFailureException("injected after target write");})
                .when(guard).persistRoom(argThat(room->room!=null && room.getDormRoomId()==9702));
        assertThrows(org.springframework.web.util.NestedServletException.class,()->approve(9710,admin));
        assertEquals("99970001",jdbc.queryForObject("SELECT first_bed FROM dorm_room WHERE dormroom_id=9700",String.class));
        assertNull(jdbc.queryForObject("SELECT first_bed FROM dorm_room WHERE dormroom_id=9702",String.class));
        assertEquals(0,count("SELECT version FROM dorm_room WHERE dormroom_id=9700"));assertEquals(0,count("SELECT version FROM dorm_room WHERE dormroom_id=9702"));
        assertEquals("未处理",jdbc.queryForObject("SELECT state FROM adjust_room WHERE id=9710",String.class));assertOneOccupancyPerStudent();
    }
    @Test void sameRoomTransferMovesBedWithoutChangingCount() throws Exception {
        application(9710,"99970001",9700,9700,2);assertEquals(200,approve(9710,admin));
        assertNull(jdbc.queryForObject("SELECT first_bed FROM dorm_room WHERE dormroom_id=9700",String.class));
        assertEquals("99970001",jdbc.queryForObject("SELECT second_bed FROM dorm_room WHERE dormroom_id=9700",String.class));
        assertEquals(1,count("SELECT current_capacity FROM dorm_room WHERE dormroom_id=9700"));assertOneOccupancyPerStudent();
    }
    @Test void staleSourceBedAndInvalidCapacityNeverPartiallyMoveStudent() throws Exception {
        application(9710,"99970001",9700,9702,2);jdbc.update("UPDATE dorm_room SET max_capacity=1 WHERE dormroom_id=9702");
        assertEquals(409,approve(9710,admin));
        jdbc.update("UPDATE dorm_room SET max_capacity=4 WHERE dormroom_id=9702");jdbc.update("UPDATE dorm_room SET first_bed='99970004' WHERE dormroom_id=9700");
        assertEquals(409,approve(9710,admin));assertEquals(0,count("SELECT current_capacity FROM dorm_room WHERE dormroom_id=9702"));
    }
    @Test void historicalDuplicateOccupancyFailsPreflightWithoutDeletingRecords() {
        jdbc.update("UPDATE dorm_room SET first_bed='99970001',current_capacity=1 WHERE dormroom_id=9702");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->preflight.run(new org.springframework.boot.DefaultApplicationArguments()));
        assertEquals(2,count("SELECT COUNT(*) FROM dorm_room WHERE first_bed='99970001'"));
    }
    @Test void independentDatabaseTransactionBlocksApiWriterUntilCommit() throws Exception {
        CountDownLatch acquired=new CountDownLatch(1),release=new CountDownLatch(1),writerEntered=new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(call->{if(calls.incrementAndGet()>1)writerEntered.countDown();return call.callRealMethod();}).when(guard).lock();
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<?> holder=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->{
                guard.lock();acquired.countDown();try {assertTrue(release.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}return null;
            }));
            assertTrue(acquired.await(5,TimeUnit.SECONDS));
            Future<Integer> writer=pool.submit(()->write(mvc,put("/room/update"),assign(9702,"99970003"),admin).andReturn().getResponse().getStatus());
            assertTrue(writerEntered.await(5,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->writer.get(250,TimeUnit.MILLISECONDS));
            release.countDown();holder.get(5,TimeUnit.SECONDS);assertEquals(200,writer.get(5,TimeUnit.SECONDS));assertOneOccupancyPerStudent();
        } finally {release.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
    }
}
