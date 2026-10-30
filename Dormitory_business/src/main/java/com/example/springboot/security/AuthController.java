package com.example.springboot.security;

import com.example.springboot.common.Result;
import com.example.springboot.entity.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import javax.servlet.http.*;
import java.util.*;

@RestController
public class AuthController {
    private final Accounts accounts;
    private final CookieCsrfTokenRepository csrf;
    public AuthController(Accounts accounts, CookieCsrfTokenRepository csrf) { this.accounts=accounts; this.csrf=csrf; }
    @GetMapping("/auth/csrf") public Result<?> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken)request.getAttribute(CsrfToken.class.getName());
        return Result.success(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));
    }
    @PostMapping({"/admin/login","/dormManager/login","/stu/login"})
    public Result<?> login(@RequestBody User input, HttpServletRequest request, HttpServletResponse response) {
        String role = request.getRequestURI().substring(request.getContextPath().length()).split("/")[1];
        SessionUser user = accounts.authenticate(role,input.getUsername(),input.getPassword());
        HttpSession previous = request.getSession(false); if (previous != null) previous.invalidate();
        String authority = role.equals("admin") ? "ADMIN" : role.equals("stu") ? "STUDENT" : "MANAGER";
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user,null,Collections.singleton(new SimpleGrantedAuthority("ROLE_"+authority))));
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context);
        csrf.saveToken(null,request,response);
        return Result.success(accounts.profile(user));
    }
    @GetMapping("/auth/me") public Result<?> me() { return Result.success(accounts.profile()); }
    @PutMapping("/auth/profile") public Result<?> profile(@RequestBody Map<String,Object> body) {
        accounts.updateProfile(body); return Result.success(accounts.profile());
    }
    @PostMapping("/auth/password") public Result<?> password(@RequestBody Map<String,String> body, HttpServletRequest request, HttpServletResponse response) {
        accounts.changePassword(body.get("currentPassword"),body.get("newPassword"));
        return logout(request,response);
    }
    @PostMapping("/auth/logout") public Result<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext(); HttpSession session=request.getSession(false); if(session!=null)session.invalidate();
        csrf.saveToken(null,request,response);
        Cookie cookie=new Cookie("JSESSIONID",""); cookie.setPath("/"); cookie.setHttpOnly(true); cookie.setMaxAge(0); response.addCookie(cookie);
        return Result.success();
    }
}
