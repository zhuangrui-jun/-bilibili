package com.zr.bili.controller;

import com.zr.bili.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class testController {
    private final EmailService emailService;
    @GetMapping("/test")
    public void test(){
        emailService.sendCodeEmail("2268995210@qq.com","123456");
    }
}
