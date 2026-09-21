package br.com.sintonia.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({"/login", "/room", "/room/**"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
