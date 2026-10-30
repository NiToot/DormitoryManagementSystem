package com.example.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import javax.servlet.http.Cookie;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

final class SecurityClient {
    static MockHttpServletRequestBuilder csrf(MockMvc mvc, MockHttpServletRequestBuilder request) throws Exception {
        JsonNode data = new ObjectMapper().readTree(mvc.perform(get("/auth/csrf")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("data");
        String token = data.get("token").asText();
        return request.cookie(new Cookie("XSRF-TOKEN", token)).header(data.get("headerName").asText(), token);
    }
    static ResultActions write(MockMvc mvc, MockHttpServletRequestBuilder request, String body, MockHttpSession session) throws Exception {
        request = csrf(mvc, request).contentType("application/json").content(body);
        if(session != null) request.session(session);
        return mvc.perform(request);
    }
    static MockHttpSession login(MockMvc mvc, String role, String username) throws Exception {
        return login(mvc, role, username, "Demo123456");
    }
    static MockHttpSession login(MockMvc mvc, String role, String username, String password) throws Exception {
        return (MockHttpSession)write(mvc, post("/"+role+"/login"),
                new ObjectMapper().writeValueAsString(java.util.Map.of("username",username,"password",password)), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.password").doesNotExist()).andReturn().getRequest().getSession(false);
    }
}
