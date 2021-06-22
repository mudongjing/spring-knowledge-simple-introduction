package com.example.demo.controller;

import com.example.demo.pojo.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class GsonController {
    @Autowired
    private UserService userService;
    @GetMapping("/th")
    public String jsonall(Model model){
        List<User> userList=new ArrayList<>();
        userList=userService.findAll();
        model.addAttribute("userList",userList);
        return "jsonall";
    }
    @GetMapping("/ch/insert")
    public String jsoninsert(Model model){
        User user=new User();
        model.addAttribute("myuser",user);
        return "json";
    }
    @GetMapping("/th/{name}")
    public String json(@PathVariable String name, Model model,RedirectAttributes redirectAttributes){
        User user=null;
        user= userService.selectUser(name);
        if (user!=null){
            model.addAttribute("myuser",user);
            return "json";
        }else{
            redirectAttributes.addFlashAttribute("message","不存在对应的记录！");
            redirectAttributes.addFlashAttribute("user_name",name);
            return "redirect:/ch/insert";
        }

    }
    @PostMapping(value = "/insertUser",produces ="application/json")
    public String addUser( User user , RedirectAttributes redirectAttributes){
        if(user.getUserId()!=null && user.getUserName()!=null){
            userService.addUser(user);
            redirectAttributes.addFlashAttribute("message","成功！");
            redirectAttributes.addFlashAttribute("userobject",user.getId()+","
                    +user.getUserName());
        }else{
            redirectAttributes.addFlashAttribute("message","非常失败");
            redirectAttributes.addFlashAttribute("userobject",user.getId()+","
                    +user.getUserName());
        }

        return "redirect:/th";


    }
}
