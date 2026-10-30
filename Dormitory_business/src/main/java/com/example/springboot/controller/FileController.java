package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.security.Accounts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;

@RestController @RequestMapping("/files")
public class FileController {
    private final Accounts accounts;
    private final Path directory;
    public FileController(Accounts accounts,@Value("${app.upload-dir:./data/uploads}") String directory) {
        this.accounts=accounts;this.directory=Paths.get(directory).toAbsolutePath().normalize();
    }
    @PostMapping("/avatar") public Result<?> avatar(@RequestParam("file") MultipartFile file) throws IOException {
        if(file.isEmpty() || file.getSize()>2*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"头像大小需在2MB以内");
        BufferedImage image;
        try(java.io.InputStream input=file.getInputStream()) { image=ImageIO.read(input); }
        if(image==null || image.getWidth()>4096 || image.getHeight()>4096)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请上传有效的小尺寸图片");
        Files.createDirectories(directory);
        String filename=UUID.randomUUID().toString().replace("-","")+".png";
        Path target=directory.resolve(filename);
        ImageIO.write(image,"png",target.toFile());
        try { accounts.updateAvatar(filename); } catch(RuntimeException e) {Files.deleteIfExists(target);throw e;}
        return Result.success(filename);
    }
    @GetMapping("/initAvatar/{filename}") public Result<?> avatar(@PathVariable String filename) throws IOException {
        if(!filename.matches("[a-f0-9]{32}\\.png") || !filename.equals(accounts.profile().get("avatar")))throw new AccessDeniedException("头像不属于当前用户");
        Path target=directory.resolve(filename).normalize();
        if(!target.startsWith(directory) || !Files.isRegularFile(target))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"头像不存在");
        return Result.success(Result.success(Base64.getEncoder().encodeToString(Files.readAllBytes(target))));
    }
}
