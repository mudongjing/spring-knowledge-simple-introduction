package org.example.controller;

import org.example.bean.User;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Controller
public class webController {
    @InitBinder
    public void init(WebDataBinder binder){
        binder.addCustomFormatter(new DateFormatter());
    }
    @InitBinder("user")
    public void initBinderUser(WebDataBinder binder) {
        binder.setFieldDefaultPrefix("user.");
    }

//    @ModelAttribute("date")  Date date
    @ResponseBody
    @RequestMapping("/user/now")
    public Date userinit1(HttpServletRequest request){
        Date date =new Date();
        return date;
    }
    @ResponseBody
    @RequestMapping("/user/{date}")
    public Date userinit(Date date){
        return date;
    }
    @ResponseBody
    @RequestMapping("/user")
    public User userinit11(@ModelAttribute("user") User user){

        return user;
    }
}
