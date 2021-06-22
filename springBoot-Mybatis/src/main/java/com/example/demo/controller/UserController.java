package com.example.demo.controller;

import com.example.demo.pojo.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UserController {
    @Autowired
    private UserService userService;
    @GetMapping("/json/{name}")
    @ResponseBody
    public User json(@PathVariable String name){

        User user= new User();
        user=userService.selectUser(name);

        return user;
    }
}
