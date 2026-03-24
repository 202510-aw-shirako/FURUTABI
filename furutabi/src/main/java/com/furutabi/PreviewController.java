package com.furutabi;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PreviewController {

    @GetMapping("/preview/login")
    public String previewLogin() {
        return "auth/login";
    }
}
