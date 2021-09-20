package com.example.demo.mvc.controller;

import com.example.demo.mybatis.service.GroupMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.sql.Time;

@RestController
public class GroupMessageController {
    @Autowired
    private GroupMessageService groupMessageService;

    @GetMapping("message/create/{name}")
    public String createMessageTable(@PathVariable("name") String name){
        int i=groupMessageService.createMessageTable(name);
        if(i>=0) return "success";
        else return "false";
    }
    @PostMapping("message/{creator}/{group}")
    public String insertMessage(@PathVariable("creator") Integer creator,
                                @PathVariable("group") Integer group,
                                @RequestParam("date")Date date,
                                @RequestParam("time")Time time,
                                @RequestParam("content") String content){
        System.out.println(System.currentTimeMillis());
        String messageTable="message_"+group;
        int i=groupMessageService.insertMessageItem(messageTable,content,creator,date,time);
        if(i>0) return "消息发送成功";
        else return "消息发送失败";
    }
}
