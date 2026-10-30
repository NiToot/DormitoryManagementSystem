package com.example.springboot.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Component
@ConditionalOnProperty(name="app.security.migrate-legacy-passwords",havingValue="true")
public class LegacyPasswordMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public LegacyPasswordMigration(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override @Transactional public void run(ApplicationArguments args) {
        BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(12);
        for(String table:new String[]{"admin","student","dorm_manager"}) {
            for(Map<String,Object> row:jdbc.queryForList("SELECT username,password FROM "+table)) {
                String previous=(String)row.get("password");
                if(previous==null || previous.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"))continue;
                if(previous.isEmpty() || previous.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
                    throw new IllegalStateException("Legacy password cannot be safely migrated; reset the affected account offline");
                jdbc.update("UPDATE "+table+" SET password=?,auth_version=auth_version+1 WHERE username=? AND password=?",
                        encoder.encode(previous),row.get("username"),previous);
            }
        }
    }
}
