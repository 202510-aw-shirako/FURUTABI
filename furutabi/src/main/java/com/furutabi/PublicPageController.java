package com.furutabi;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicPageController {

    private static final String TOUR_EXTERNAL_URL = "https://example.com/furutabi-tour";

    @GetMapping({ "/", "/bridge", "/bridge.html" })
    public String bridge() {
        return "public/bridge";
    }

    @GetMapping({ "/index", "/index.html" })
    public String index() {
        return "public/index";
    }

    @GetMapping({ "/about", "/about.html" })
    public String about() {
        return "public/about";
    }

    @GetMapping({ "/faq", "/faq.html" })
    public String faq() {
        return "public/faq";
    }

    @GetMapping({ "/gate", "/gate.html" })
    public String gate() {
        return "public/gate";
    }

    @GetMapping({ "/gate-entry", "/gate-entry.html" })
    public String gateEntry() {
        return "public/gate-entry";
    }

    @GetMapping({ "/local", "/local.html" })
    public String local() {
        return "public/local";
    }

    @GetMapping({ "/story", "/story.html" })
    public String story() {
        return "public/story";
    }

    @GetMapping({ "/notices", "/notices.html" })
    public String notices() {
        return "public/notices";
    }

    @GetMapping({ "/notice", "/notice.html" })
    public String notice() {
        return "public/notice";
    }

    @GetMapping({ "/okatte-entry", "/okatte-entry.html" })
    public String okatteEntry() {
        return "public/okatte-entry";
    }

    @GetMapping({ "/terms", "/terms.html" })
    public String terms() {
        return "public/terms";
    }

    @GetMapping({ "/privacy", "/privacy.html" })
    public String privacy() {
        return "public/privacy";
    }

    @GetMapping({ "/contact", "/contact.html" })
    public String contact() {
        return "public/contact";
    }

    @GetMapping({ "/contact-complete", "/contact-complete.html" })
    public String contactComplete() {
        return "public/contact-complete";
    }

    @GetMapping({ "/map", "/map.html" })
    public String map() {
        return "redirect:/index.html#top-footprints";
    }

    @GetMapping({ "/safety", "/safety.html" })
    public String safety() {
        return "redirect:/contact.html";
    }

    @GetMapping({ "/safety-complete", "/safety-complete.html" })
    public String safetyComplete() {
        return "redirect:/contact-complete.html";
    }

    @GetMapping({ "/tour", "/tour.html" })
    public String tour() {
        return "redirect:" + TOUR_EXTERNAL_URL;
    }
}
