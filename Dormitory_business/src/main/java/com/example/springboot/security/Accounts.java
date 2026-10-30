package com.example.springboot.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class Accounts {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    public Accounts(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public String table(String role) {
        switch (role) {
            case "admin": return "admin";
            case "stu": return "student";
            case "dormManager": return "dorm_manager";
            default: throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效身份");
        }
    }
    public Map<String,Object> find(String role, String username) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM " + table(role) + " WHERE username=?", username);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public SessionUser authenticate(String role, String username, String password) {
        Map<String,Object> row = username == null ? null : find(role, username);
        if (row == null || !matches(password, (String)row.get("password"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return new SessionUser(role, username, ((Number)row.get("auth_version")).longValue());
    }
    public boolean matches(String raw, String encoded) {
        return raw != null && raw.getBytes(StandardCharsets.UTF_8).length <= 72 && encoded != null
                && encoded.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}") && encoder.matches(raw, encoded);
    }
    public String encode(String password) {
        if (password == null || password.length() < 8 || password.length() > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码需为8至64个字符，UTF-8编码不超过72字节");
        }
        return encoder.encode(password);
    }
    public SessionUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SessionUser))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        return (SessionUser)auth.getPrincipal();
    }
    public boolean valid(SessionUser user) {
        Map<String,Object> row = find(user.role, user.username);
        return row != null && ((Number)row.get("auth_version")).longValue() == user.version;
    }
    public Map<String,Object> profile() { return profile(current()); }
    public Map<String,Object> profile(SessionUser user) {
        Map<String,Object> row = find(user.role, user.username);
        if (row == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号已失效");
        Map<String,Object> result = new LinkedHashMap<>();
        for (String field : Arrays.asList("username", "name", "age", "gender", "email", "avatar")) result.put(field, row.get(field));
        result.put("phoneNum", row.get("phone_num"));
        result.put("identity", user.role);
        if (user.role.equals("dormManager")) result.put("dormBuildId", row.get("dormbuild_id"));
        return result;
    }
    public void updateProfile(Map<String,Object> body) {
        Set<String> allowed = new HashSet<>(Arrays.asList("name", "age", "gender", "phoneNum", "email"));
        if (body.isEmpty() || !allowed.containsAll(body.keySet()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "个人资料包含不允许修改的字段");
        if (body.containsKey("name") && (body.get("name") == null || body.get("name").toString().trim().isEmpty() || body.get("name").toString().length() > 64))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "姓名格式错误");
        if (body.containsKey("age") && (!(body.get("age") instanceof Number) || ((Number)body.get("age")).intValue() < 1 || ((Number)body.get("age")).intValue() > 100))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "年龄范围1至100");
        List<String> assignments = new ArrayList<>(); List<Object> args = new ArrayList<>();
        for (Map.Entry<String,Object> entry : body.entrySet()) {
            if (entry.getValue() != null && entry.getValue().toString().length() > 128)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "资料字段过长");
            assignments.add((entry.getKey().equals("phoneNum") ? "phone_num" : entry.getKey()) + "=?"); args.add(entry.getValue());
        }
        SessionUser user = current(); args.add(user.username);
        jdbc.update("UPDATE " + table(user.role) + " SET " + String.join(",", assignments) + " WHERE username=?", args.toArray());
    }
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        SessionUser user = current(); Map<String,Object> row = find(user.role, user.username);
        String hash = (String)row.get("password");
        if (!matches(oldPassword, hash)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码错误");
        if (matches(newPassword, hash)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能与原密码相同");
        int count = jdbc.update("UPDATE " + table(user.role) + " SET password=?,auth_version=auth_version+1 WHERE username=? AND password=?",
                encode(newPassword), user.username, hash);
        if (count != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "密码已发生变化，请重新登录");
    }
    public void updateAvatar(String filename) {
        SessionUser user = current();
        jdbc.update("UPDATE " + table(user.role) + " SET avatar=? WHERE username=?", filename, user.username);
    }
}
