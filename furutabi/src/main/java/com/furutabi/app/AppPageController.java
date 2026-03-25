package com.furutabi.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
public class AppPageController {

    @GetMapping("/home")
    public String home() {
        return "app/home";
    }

    @GetMapping("/local-member-home")
    public String localMemberHome() {
        return "app/local-member-home";
    }

    @GetMapping("/mypage")
    public String mypage() {
        return "app/mypage";
    }
}
