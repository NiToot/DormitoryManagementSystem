package com.example.springboot.security;

import com.example.springboot.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

@Configuration
public class SecurityConfig {
    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status); response.setContentType("application/json;charset=UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), Result.error(String.valueOf(status), message));
    }
    @Bean public CookieCsrfTokenRepository csrfRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/"); return repository;
    }
    @Bean public SecurityFilterChain security(HttpSecurity http, Accounts accounts, CookieCsrfTokenRepository csrf) throws Exception {
        http.cors().and().csrf().csrfTokenRepository(csrf).and()
            .formLogin().disable().httpBasic().disable().logout().disable().requestCache().disable()
            .exceptionHandling().authenticationEntryPoint((req,res,e) -> error(res,401,"请先登录或会话已失效"))
            .accessDeniedHandler((req,res,e) -> error(res,403,"权限不足或CSRF校验失败")).and()
            .authorizeRequests()
            .antMatchers(HttpMethod.GET,"/auth/csrf").permitAll()
            .antMatchers(HttpMethod.POST,"/admin/login","/stu/login","/dormManager/login").permitAll()
            .antMatchers("/auth/**","/main/loadIdentity","/main/loadUserInfo","/main/signOut").authenticated()
            .antMatchers("/admin/**","/dormManager/**").hasRole("ADMIN")
            .antMatchers(HttpMethod.POST,"/stu/add","/room/add","/building/add","/notice/add").hasRole("ADMIN")
            .antMatchers(HttpMethod.DELETE,"/stu/**","/room/delete/{id}","/building/**","/notice/**").hasRole("ADMIN")
            .antMatchers(HttpMethod.PUT,"/building/**","/notice/**").hasRole("ADMIN")
            .antMatchers(HttpMethod.GET,"/stu/find","/room/find","/building/find","/notice/find").hasAnyRole("ADMIN","MANAGER")
            .antMatchers(HttpMethod.PUT,"/stu/update","/room/update","/repair/update").hasAnyRole("ADMIN","MANAGER")
            .antMatchers(HttpMethod.DELETE,"/repair/**","/room/delete/**").hasAnyRole("ADMIN","MANAGER")
            .antMatchers("/visitor/**").hasAnyRole("ADMIN","MANAGER")
            .antMatchers(HttpMethod.POST,"/repair/add","/adjustRoom/add").hasRole("STUDENT")
            .antMatchers("/repair/find","/repair/find/*","/repair/orderNum","/adjustRoom/find","/adjustRoom/update/*").authenticated()
            .antMatchers(HttpMethod.DELETE,"/adjustRoom/**").hasAnyRole("ADMIN","MANAGER")
            .antMatchers(HttpMethod.GET,"/notice/homePageNotice","/stu/stuNum","/stu/exist/*","/building/getBuildingName","/room/noFullRoom","/room/judgeHadBed/*","/room/selectHaveRoomStuNum","/room/getEachBuildingStuNum/*","/room/getMyRoom/*","/room/checkRoomState/*","/room/checkBedState/*/*","/room/checkRoomExist/*","/files/initAvatar/*").authenticated()
            .antMatchers(HttpMethod.POST,"/files/avatar").authenticated()
            .anyRequest().denyAll();
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                Object principal = SecurityContextHolder.getContext().getAuthentication() == null ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (principal instanceof SessionUser && !accounts.valid((SessionUser)principal)) {
                    SecurityContextHolder.clearContext();
                    HttpSession session = request.getSession(false); if (session != null) session.invalidate();
                    error(response,401,"账号或密码已变更，请重新登录"); return;
                }
                chain.doFilter(request,response);
            }
        }, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
