package com.example.springboot.security;

import com.example.springboot.common.Result;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class) public ResponseEntity<?> concurrent() {
        return ResponseEntity.status(409).body(Result.error("409","并发操作繁忙，请刷新后重试"));
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatus()).body(Result.error(String.valueOf(e.getStatus().value()),e.getReason()));
    }
    @ExceptionHandler(AccessDeniedException.class) public ResponseEntity<?> denied() {
        return ResponseEntity.status(403).body(Result.error("403","无权操作该数据"));
    }
    @ExceptionHandler(DuplicateKeyException.class) public ResponseEntity<?> duplicate() {
        return ResponseEntity.status(409).body(Result.error("409","记录已存在"));
    }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> invalid() {
        return ResponseEntity.badRequest().body(Result.error("400","请求参数无效"));
    }
}
