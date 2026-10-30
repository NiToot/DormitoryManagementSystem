package com.example.springboot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class SpringbootApplicationTests {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void requireDedicatedDemoSchema() {
        assertEquals("dormitory_demo", jdbc.queryForObject("SELECT DATABASE()", String.class),
                "Initialize database/init.sql in the dedicated local demo schema before testing");
    }

    @Test
    void contextLoads() {
        assertEquals(10, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE()", Integer.class));
    }

    @Test
    void threeRolesCanLoginAndRestoreTheirSession() throws Exception {
        String[][] accounts = {{"admin", "admin01"}, {"dormManager", "dorm01"}, {"stu", "20230001"}};
        for (String[] account : accounts) {
            MockHttpSession session = login(account[0], account[1]);
            mvc.perform(get("/main/loadIdentity").session(session))
                    .andExpect(jsonPath("$.code").value("0"))
                    .andExpect(jsonPath("$.data").value(account[0]));
            mvc.perform(get("/main/loadUserInfo").session(session))
                    .andExpect(jsonPath("$.data.username").value(account[1]));
        }
    }

    @Test
    void incorrectPasswordIsRejected() throws Exception {
        SecurityClient.write(mvc, post("/stu/login"),
                "{\"username\":\"20230001\",\"password\":\"incorrect\"}", null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanReadEveryManagementPage() throws Exception {
        MockHttpSession session = login("admin", "admin01");
        for (String path : new String[]{"/stu/find", "/dormManager/find", "/building/find", "/room/find",
                "/notice/find", "/repair/find", "/adjustRoom/find", "/visitor/find"}) {
            mvc.perform(get(path).session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("0"))
                    .andExpect(jsonPath("$.data.records").isArray());
        }
    }

    @Test
    void studentCanReadTheirRoomAndLogout() throws Exception {
        MockHttpSession session = login("stu", "20230001");
        mvc.perform(get("/room/getMyRoom/20230001").session(session))
                .andExpect(jsonPath("$.data.dormRoomId").value(1001))
                .andExpect(jsonPath("$.data.firstBed").value("20230001"));
        SecurityClient.write(mvc, post("/auth/logout"), "{}", session).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
        mvc.perform(get("/main/loadUserInfo")).andExpect(status().isUnauthorized());
    }

    private MockHttpSession login(String role, String username) throws Exception {
        return SecurityClient.login(mvc, role, username);
    }

}
