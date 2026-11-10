package com.example.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.example.springboot.SecurityClient.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RichTextBoundaryTests {
    private static final String HTML = "<p><strong>维修说明</strong></p><img src=x onerror=alert(1)>"
            + "<script>alert(2)</script><a href='javascript:alert(3)'>链接</a>";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach void requireDemoDatabase() {
        assertEquals("dormitory_demo", jdbc.queryForObject("SELECT DATABASE()", String.class));
    }

    @Test void noticeCreateStoresOnlySafeFormatting() throws Exception {
        MockHttpSession admin = login(mvc, "admin", "admin01");
        write(mvc, post("/notice/add"), body(Map.of("title", "富文本回归", "content", HTML,
                "author", "演示管理员", "releaseTime", "2026-11-10 08:00:00")), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0"));
        assertSafe(jdbc.queryForObject("SELECT content FROM notice WHERE title='富文本回归'", String.class));
    }

    @Test void noticeUpdateCannotReintroduceActiveHtml() throws Exception {
        write(mvc, put("/notice/update"), body(Map.of("id", 1, "content", HTML)), login(mvc, "admin", "admin01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0"));
        assertSafe(jdbc.queryForObject("SELECT content FROM notice WHERE id=1", String.class));
    }

    @Test void legacyNoticesAreSanitizedOnBothReadRoutesWithoutRewritingDatabase() throws Exception {
        jdbc.update("UPDATE notice SET content=? WHERE id=1", HTML);
        MockHttpSession admin = login(mvc, "admin", "admin01");
        assertSafe(readContent("/notice/find", admin, true));
        assertSafe(readContent("/notice/homePageNotice", login(mvc, "stu", "20230001"), false));
        assertEquals(HTML, jdbc.queryForObject("SELECT content FROM notice WHERE id=1", String.class));
    }

    @Test void studentRepairCannotStoreActiveHtml() throws Exception {
        write(mvc, post("/repair/add"), body(Map.of("title", "报修富文本回归", "content", HTML)),
                login(mvc, "stu", "20230001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0"));
        assertSafe(jdbc.queryForObject("SELECT content FROM repair WHERE title='报修富文本回归'", String.class));
    }

    @Test void repairUpdateCannotReintroduceActiveHtml() throws Exception {
        write(mvc, put("/repair/update"), body(Map.of("id", 1, "content", HTML)), login(mvc, "admin", "admin01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0"));
        assertSafe(jdbc.queryForObject("SELECT content FROM repair WHERE id=1", String.class));
    }

    @Test void legacyRepairsAreSanitizedOnListAndPersonalRoutes() throws Exception {
        jdbc.update("UPDATE repair SET content=? WHERE id=1", HTML);
        assertSafe(readContent("/repair/find", login(mvc, "admin", "admin01"), true));
        assertSafe(readContent("/repair/find/本人", login(mvc, "stu", "20230001"), true));
        assertEquals(HTML, jdbc.queryForObject("SELECT content FROM repair WHERE id=1", String.class));
    }

    @Test void omittedRepairContentPreservesExistingText() throws Exception {
        String original = jdbc.queryForObject("SELECT content FROM repair WHERE id=1", String.class);
        write(mvc, put("/repair/update"), body(Map.of("id", 1, "state", "已完成")), login(mvc, "admin", "admin01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0"));
        assertEquals(original, jdbc.queryForObject("SELECT content FROM repair WHERE id=1", String.class));
    }

    @Test void oversizedContentIsRejectedWithoutSaving() throws Exception {
        String original = jdbc.queryForObject("SELECT content FROM notice WHERE id=1", String.class);
        write(mvc, put("/notice/update"), body(Map.of("id", 1, "content", "x".repeat(20001))),
                login(mvc, "admin", "admin01")).andExpect(status().isBadRequest());
        assertEquals(original, jdbc.queryForObject("SELECT content FROM notice WHERE id=1", String.class));
    }

    private String body(Object value) throws Exception { return json.writeValueAsString(value); }

    private String readContent(String route, MockHttpSession session, boolean paged) throws Exception {
        JsonNode data = json.readTree(mvc.perform(get(route).session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("data");
        for (JsonNode record : paged ? data.get("records") : data) {
            if (record.get("id").asInt() == 1) return record.get("content").asText();
        }
        throw new AssertionError("Expected demonstration record 1");
    }

    private void assertSafe(String content) {
        assertTrue(content.contains("<strong>维修说明</strong>"), content);
        assertFalse(content.contains("<script"), content);
        assertFalse(content.contains("onerror"), content);
        assertFalse(content.contains("javascript:"), content);
        assertFalse(content.contains("<img"), content);
    }
}
