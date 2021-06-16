package org.example.controller.filter;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FilterController {
    @GetMapping("/filter/**")
    public String filtero(){
        return "index";
    }
}
