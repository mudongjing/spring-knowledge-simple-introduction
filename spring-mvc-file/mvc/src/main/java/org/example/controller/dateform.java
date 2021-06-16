package org.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class dateform {
    @GetMapping("test/binder")
    public String  inti(){
        return "binder/init";
    }
}
