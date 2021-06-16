package org.example.controller.upload;

import org.example.bean.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class fileController {
    @GetMapping("/file")
    public String file(){
        return "upload/file";
    }
    @GetMapping("/download")
    public String down(){
        return "download/download";
    }
    @GetMapping("/param")
    public String fanhui(User user, Model model){
        model.addAttribute(user);
        return "index";
    }
    @GetMapping("/param/{name}/{age}")
    public String fanhui1(User user, Model model){
        model.addAttribute(user);
        return "index";
    }
}
