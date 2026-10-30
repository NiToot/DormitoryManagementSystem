package com.example.springboot.common;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.*;
import java.util.Arrays;
@Configuration
public class CorsConfig {
 @Bean public CorsConfigurationSource corsConfigurationSource(@Value("${app.allowed-origins:http://127.0.0.1:8080,http://localhost:8080}") String origins){
  CorsConfiguration config=new CorsConfiguration();
  config.setAllowedOrigins(Arrays.asList(origins.split(",")));config.setAllowCredentials(true);
  config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
  config.setAllowedHeaders(Arrays.asList("Content-Type","X-XSRF-TOKEN","Accept","X-Requested-With"));
  UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",config);return source;
 }
}
