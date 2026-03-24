package com.furutabi;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PreviewController {

    @GetMapping("/preview/login")
    public String previewLogin() {
        return "auth/login";
    }

    @GetMapping("/preview/public/bridge.html")
    public String previewBridge() {
        return "public/bridge";
    }

    @GetMapping("/preview/public/index.html")
    public String previewPublicIndex() {
        return "public/index";
    }

    @GetMapping("/preview/auth/login.html")
    public String previewAuthLogin() {
        return "auth/login";
    }
}
