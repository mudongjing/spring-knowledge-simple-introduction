package org.example.controller.filter;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class alter {
    @GetMapping("/filters")
    public String filters(){
        String str=null;
        str="login";
        return str;
    }
}
