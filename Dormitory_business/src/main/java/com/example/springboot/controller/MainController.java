package com.example.springboot.controller;
import com.example.springboot.common.Result;
import com.example.springboot.security.Accounts;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/main")
public class MainController {
 private final Accounts accounts;
 public MainController(Accounts accounts){this.accounts=accounts;}
 @GetMapping("/loadIdentity") public Result<?> identity(){return Result.success(accounts.current().role);}
 @GetMapping("/loadUserInfo") public Result<?> user(){return Result.success(accounts.profile());}
 @GetMapping("/signOut") public Result<?> oldLogout(){throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED,"请使用POST /auth/logout");}
}
