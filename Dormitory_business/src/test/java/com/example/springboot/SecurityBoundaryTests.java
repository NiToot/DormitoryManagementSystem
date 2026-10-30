package com.example.springboot;

import com.example.springboot.security.Accounts;
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

@SpringBootTest(properties="app.upload-dir=../.runtime/test-uploads")
@AutoConfigureMockMvc
@Transactional
class SecurityBoundaryTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired Accounts accounts;

    @BeforeEach void fixtures() {
        assertEquals("dormitory_demo",jdbc.queryForObject("SELECT DATABASE()",String.class));
        String hash=jdbc.queryForObject("SELECT password FROM student WHERE username='20230001'",String.class);
        assertTrue(hash.startsWith("$2"),"Run the explicit legacy password migration before testing");
        for(String id:new String[]{"99990001","99990002"})
            jdbc.update("INSERT INTO student(username,password,name,age) VALUES(?,?,?,20)",id,hash,"同名测试学生");
        jdbc.update("INSERT INTO dorm_room(dormroom_id,dormbuild_id,floor_num,current_capacity,first_bed) VALUES(9001,1,9,1,'99990001'),(9002,2,9,1,'99990002')");
        jdbc.update("INSERT INTO repair(id,repairer,student_username,dormbuild_id,dormroom_id,title) VALUES(901,'同名测试学生','99990001',1,9001,'scope-a'),(902,'同名测试学生','99990002',2,9002,'scope-b'),(903,'同名测试学生',NULL,1,9001,'ambiguous')");
        jdbc.update("INSERT INTO visitor(id,name,dormbuild_id) VALUES(901,'scope-a',1),(902,'scope-b',2)");
        jdbc.update("INSERT INTO adjust_room(id,username,name,currentroom_id,currentbed_id,towardsroom_id,towardsbed_id) VALUES(901,'99990001','同名测试学生',9001,1,1002,1),(902,'99990002','同名测试学生',9002,1,2001,1)");
    }

    @Test void anonymousCannotReadStudents() throws Exception {
        for(String path:new String[]{"/stu/find","/auth/me","/repair/find","/files/initAvatar/test.png"})
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
    @Test void loginAndWritesRequireCsrf() throws Exception {
        mvc.perform(post("/stu/login").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/auth/profile").session(login(mvc,"stu","20230001"))
                .contentType("application/json").content("{\"name\":\"伪造\"}"))
                .andExpect(status().isForbidden());
        write(mvc,put("/auth/profile"),"{\"name\":\"伪造\"}",null).andExpect(status().isUnauthorized());
    }
    @Test void loginNeverReturnsPasswordAndPlaintextIsNotAccepted() throws Exception {
        MockHttpSession session=login(mvc,"stu","20230001");
        mvc.perform(get("/auth/me").session(session)).andExpect(jsonPath("$.data.password").doesNotExist());
        jdbc.update("UPDATE student SET password='Demo123456' WHERE username='99990001'");
        write(mvc,post("/stu/login"),"{\"username\":\"99990001\",\"password\":\"Demo123456\"}",null)
                .andExpect(status().isUnauthorized());
    }
    @Test void studentCannotReadManagerListOrCreateAccounts() throws Exception {
        MockHttpSession student=login(mvc,"stu","20230001");
        mvc.perform(get("/dormManager/find").session(student)).andExpect(status().isForbidden());
        write(mvc,post("/stu/add"),"{\"username\":\"injected\",\"password\":\"Password123\"}",student)
                .andExpect(status().isForbidden());
        mvc.perform(get("/unlisted/security-route").session(student)).andExpect(status().isForbidden());
    }
    @Test void studentCannotReadAnotherStudentsRoomAndRoommatesAreMasked() throws Exception {
        MockHttpSession student=login(mvc,"stu","20230001");
        mvc.perform(get("/room/getMyRoom/20230002").session(student)).andExpect(status().isForbidden());
        mvc.perform(get("/stu/exist/20230002").session(student)).andExpect(status().isForbidden());
        mvc.perform(get("/room/getMyRoom/20230001").session(student))
                .andExpect(jsonPath("$.data.firstBed").value("20230001"))
                .andExpect(jsonPath("$.data.secondBed").value("已入住"));
        mvc.perform(get("/room/checkRoomExist/1001").session(student)).andExpect(jsonPath("$.data").isEmpty());
    }
    @Test void sameNameStudentsCannotSeeEachOthersRepairsOrAdjustments() throws Exception {
        MockHttpSession student=login(mvc,"stu","99990001");
        for(String path:new String[]{"/repair/find","/repair/find/同名测试学生","/adjustRoom/find"})
            mvc.perform(get(path).session(student)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].id").value(901));
    }
    @Test void managerListsOnlyTheirBuildingAndPasswordsAreHidden() throws Exception {
        MockHttpSession manager=login(mvc,"dormManager","dorm01");
        mvc.perform(get("/stu/find").param("search","同名测试学生").session(manager))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("99990001"))
                .andExpect(jsonPath("$.data.records[0].password").doesNotExist());
        for(String path:new String[]{"/room/find","/building/find","/visitor/find","/repair/find"}) {
            String body=mvc.perform(get(path).param("pageSize","100").session(manager)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode records=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).path("data").path("records");
            for(com.fasterxml.jackson.databind.JsonNode row:records) assertEquals(1,row.path("dormBuildId").asInt());
            assertFalse(body.contains("ambiguous"));
        }
        mvc.perform(get("/dormManager/find").session(manager)).andExpect(status().isForbidden());
        mvc.perform(get("/stu/exist/99990002").session(manager)).andExpect(status().isForbidden());
    }
    @Test void managerCannotWriteOtherBuildingById() throws Exception {
        MockHttpSession manager=login(mvc,"dormManager","dorm01");
        write(mvc,put("/stu/update"),"{\"username\":\"99990002\",\"name\":\"越权\"}",manager).andExpect(status().isForbidden());
        write(mvc,put("/room/update"),"{\"dormRoomId\":9002,\"dormBuildId\":2}",manager).andExpect(status().isForbidden());
        write(mvc,delete("/repair/delete/902"),"{}",manager).andExpect(status().isForbidden());
        write(mvc,delete("/visitor/delete/902"),"{}",manager).andExpect(status().isForbidden());
        write(mvc,delete("/adjustRoom/delete/902"),"{}",manager).andExpect(status().isForbidden());
        write(mvc,delete("/room/delete/9001"),"{}",manager).andExpect(status().isForbidden());
        write(mvc,delete("/room/delete/first_bed/9002/0"),"{}",manager).andExpect(status().isForbidden());
        write(mvc,delete("/room/delete/password/9001/0"),"{}",manager).andExpect(status().isForbidden());
        write(mvc,post("/visitor/add"),"{\"visitorName\":\"越权\",\"dormBuildId\":2}",manager).andExpect(status().isForbidden());
        assertEquals("同名测试学生",jdbc.queryForObject("SELECT name FROM student WHERE username='99990002'",String.class));
    }
    @Test void studentCannotForgeOwnershipWhenApplying() throws Exception {
        MockHttpSession student=login(mvc,"stu","99990001");
        write(mvc,post("/repair/add"),"{\"studentUsername\":\"99990002\",\"dormRoomId\":9002,\"title\":\"伪造\"}",student)
                .andExpect(status().isForbidden());
        write(mvc,post("/adjustRoom/add"),"{\"username\":\"99990002\",\"towardsRoomId\":1002,\"towardsBedId\":1}",student)
                .andExpect(status().isForbidden());
        write(mvc,put("/adjustRoom/update/false"),"{\"id\":902,\"towardsRoomId\":1002,\"towardsBedId\":1,\"state\":\"未处理\"}",student)
                .andExpect(status().isForbidden());
    }
    @Test void profileOnlyChangesCurrentUsersAllowedFields() throws Exception {
        MockHttpSession student=login(mvc,"stu","99990001");
        for(String body:new String[]{"{\"username\":\"99990002\",\"name\":\"越权\"}","{\"password\":\"NewPass123\"}","{\"identity\":\"admin\"}","{\"avatar\":\"other.png\"}"})
            write(mvc,put("/auth/profile"),body,student).andExpect(status().isBadRequest());
        write(mvc,put("/auth/profile"),"{\"name\":\"本人修改\",\"phoneNum\":\"01234567890\"}",student)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("99990001"));
        assertEquals("本人修改",jdbc.queryForObject("SELECT name FROM student WHERE username='99990001'",String.class));
        assertEquals("同名测试学生",jdbc.queryForObject("SELECT name FROM student WHERE username='99990002'",String.class));
    }
    @Test void passwordChangeRequiresOldPasswordAndRevokesAllOldSessions() throws Exception {
        MockHttpSession first=login(mvc,"stu","99990001"), second=login(mvc,"stu","99990001");
        write(mvc,post("/auth/password"),"{\"currentPassword\":\"wrong\",\"newPassword\":\"NewPass123\"}",first).andExpect(status().isBadRequest());
        write(mvc,post("/auth/password"),"{\"currentPassword\":\"Demo123456\",\"newPassword\":\"short\"}",first).andExpect(status().isBadRequest());
        write(mvc,post("/auth/password"),"{\"currentPassword\":\"Demo123456\",\"newPassword\":\"NewPass123\"}",first).andExpect(status().isOk());
        assertTrue(first.isInvalid());
        String hash=jdbc.queryForObject("SELECT password FROM student WHERE username='99990001'",String.class);
        assertNotEquals("NewPass123",hash); assertTrue(accounts.matches("NewPass123",hash));
        mvc.perform(get("/auth/me").session(second)).andExpect(status().isUnauthorized());
        assertTrue(second.isInvalid());
        write(mvc,post("/stu/login"),"{\"username\":\"99990001\",\"password\":\"Demo123456\"}",null).andExpect(status().isUnauthorized());
        login(mvc,"stu","99990001","NewPass123");
    }
    @Test void deletedAccountLosesSession() throws Exception {
        MockHttpSession student=login(mvc,"stu","99990001");
        jdbc.update("DELETE FROM student WHERE username='99990001'");
        mvc.perform(get("/auth/me").session(student)).andExpect(status().isUnauthorized());
    }
    @Test void loginRotatesSessionAndLogoutCannotBeTriggeredByGet() throws Exception {
        MockHttpSession previous=login(mvc,"stu","99990001"); String id=previous.getId();
        MockHttpSession next=(MockHttpSession)write(mvc,post("/stu/login"),"{\"username\":\"99990001\",\"password\":\"Demo123456\"}",previous)
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
        assertTrue(previous.isInvalid()); assertNotEquals(id,next.getId());
        mvc.perform(get("/main/signOut").session(next)).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/auth/me").session(next)).andExpect(status().isOk());
        write(mvc,post("/auth/logout"),"{}",next).andExpect(status().isOk()); assertTrue(next.isInvalid());
    }
    @Test void managementCannotChangePasswordsThroughProfileRoutes() throws Exception {
        write(mvc,put("/stu/update"),"{\"username\":\"99990001\",\"password\":\"UnsafePass123\"}",login(mvc,"admin","admin01"))
                .andExpect(status().isBadRequest());
        assertTrue(accounts.matches("Demo123456",jdbc.queryForObject("SELECT password FROM student WHERE username='99990001'",String.class)));
    }
    @Test void createdAccountUsesBcryptAndCreatedRepairUsesSessionOwnership() throws Exception {
        write(mvc,post("/stu/add"),"{\"username\":\"99990003\",\"name\":\"新增测试学生\",\"password\":\"Demo123456\",\"age\":20}",login(mvc,"admin","admin01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0"));
        String hash=jdbc.queryForObject("SELECT password FROM student WHERE username='99990003'",String.class);
        assertNotEquals("Demo123456",hash);assertTrue(accounts.matches("Demo123456",hash));
        login(mvc,"stu","99990003");
        write(mvc,post("/repair/add"),"{\"repairer\":\"伪造姓名\",\"title\":\"new-owned\",\"content\":\"test\",\"state\":\"完成\",\"dormBuildId\":2}",login(mvc,"stu","99990001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0"));
        java.util.Map<String,Object> row=jdbc.queryForMap("SELECT * FROM repair WHERE title='new-owned'");
        assertEquals("99990001",row.get("student_username"));assertEquals(1,row.get("dormbuild_id"));
        assertEquals("同名测试学生",row.get("repairer"));assertEquals("未完成",row.get("state"));
        jdbc.update("DELETE FROM adjust_room WHERE id=901");
        write(mvc,post("/adjustRoom/add"),"{\"username\":\"99990001\",\"currentRoomId\":9002,\"currentBedId\":4,\"towardsRoomId\":1002,\"towardsBedId\":1,\"state\":\"通过\"}",login(mvc,"stu","99990001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0"));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM adjust_room WHERE username='99990001' AND currentroom_id=9001 AND currentbed_id=1 AND state='未处理'",Integer.class));
        write(mvc,post("/visitor/add"),"{\"visitorName\":\"本楼访客\"}",login(mvc,"dormManager","dorm01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0"));
        assertEquals(1,jdbc.queryForObject("SELECT dormbuild_id FROM visitor WHERE name='本楼访客'",Integer.class));
    }
    @Test void avatarIsBoundToCurrentUserAndLegacyWriteRouteIsDenied() throws Exception {
        MockHttpSession first=login(mvc,"stu","99990001"),other=login(mvc,"stu","99990002");
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
        org.springframework.mock.web.MockMultipartFile image=new org.springframework.mock.web.MockMultipartFile("file","avatar.png","image/png",bytes.toByteArray());
        String body=mvc.perform(csrf(mvc,multipart("/files/avatar").file(image).session(first)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String filename=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("data").asText();
        try {
            assertEquals(filename,jdbc.queryForObject("SELECT avatar FROM student WHERE username='99990001'",String.class));
            assertNull(jdbc.queryForObject("SELECT avatar FROM student WHERE username='99990002'",String.class));
            mvc.perform(get("/files/initAvatar/"+filename).session(first)).andExpect(status().isOk());
            mvc.perform(get("/files/initAvatar/"+filename).session(other)).andExpect(status().isForbidden());
            write(mvc,post("/files/uploadAvatar"),"{}",first).andExpect(status().isForbidden());
            mvc.perform(csrf(mvc,multipart("/files/avatar").file(new org.springframework.mock.web.MockMultipartFile("file","evil.png","image/png","not an image".getBytes())).session(first)))
                    .andExpect(status().isBadRequest());
        } finally { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("../.runtime/test-uploads").resolve(filename)); }
    }
    @Test void explicitLegacyPasswordMigrationIsIdempotent() throws Exception {
        jdbc.update("UPDATE student SET password='Demo123456' WHERE username='99990001'");
        com.example.springboot.security.LegacyPasswordMigration migration=new com.example.springboot.security.LegacyPasswordMigration(jdbc);
        migration.run(new org.springframework.boot.DefaultApplicationArguments());
        String hash=jdbc.queryForObject("SELECT password FROM student WHERE username='99990001'",String.class);
        Long version=jdbc.queryForObject("SELECT auth_version FROM student WHERE username='99990001'",Long.class);
        assertTrue(accounts.matches("Demo123456",hash));
        migration.run(new org.springframework.boot.DefaultApplicationArguments());
        assertEquals(hash,jdbc.queryForObject("SELECT password FROM student WHERE username='99990001'",String.class));
        assertEquals(version,jdbc.queryForObject("SELECT auth_version FROM student WHERE username='99990001'",Long.class));
    }
}
