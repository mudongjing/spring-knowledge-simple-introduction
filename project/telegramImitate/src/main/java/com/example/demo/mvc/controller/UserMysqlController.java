package com.example.demo.mvc.controller;

import com.example.demo.mybatis.service.UserService;
import com.example.demo.pojo.User;
import com.example.demo.pojo.expand.UserAndFriends;
import com.example.demo.pojo.expand.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.sql.Time;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class UserMysqlController {
    @Autowired
    private UserService userService;

    @PostMapping("/sql/add")
    public String addUser(User user){
        userService.add(user);
        return user.getUserName()+": id "+user.getUserId();
    }
    @GetMapping("/sql/get/{id}")
    public User query(@PathVariable("id") Integer id){
        ExecutorService es=Executors.newFixedThreadPool(200);
        return userService.query(id);
    }
    @GetMapping("sql/create/{tableName}")
    public String createTable(@PathVariable("tableName") String tableName){
        if(userService.create(tableName)>=0){
            return "success";
        }else{
            return "false";
        }
    }
    @GetMapping("user/{userId}/{friendId}/{content}")
    public String insertMessage(@PathVariable("userId") Integer userId,
                            @PathVariable("friendId") Integer friendId,
                            @PathVariable("content")String content){
        Long t=System.currentTimeMillis();
        String tableName1="user_message_"+userId+"__"+friendId;
        String tableName2="user_message_"+friendId+"__"+userId;
        String tableName=null;
        int judge1=userService.judgeForTable(tableName1);
        int judge2=userService.judgeForTable(tableName2);
        if(userService.judgeForTable(tableName1)>0) tableName=tableName1;
        if(userService.judgeForTable(tableName2)>0) tableName=tableName2;
        if(tableName==null){
            UserAndFriends userAndFriends=new UserAndFriends(userId,friendId);
            userService.createMessageTableForFriends(userAndFriends);
            tableName=tableName1;
        }
        Date date=new Date(t);
        Time time=new Time(t);
        Short type=new Short("0");
        UserMessage userMessage=new UserMessage(tableName,null,content,type,userId,date,time);
        int i=userService.insertMessageForFriends(userMessage);
        if(i>0) return "消息插入成功";
        else return "消息插入失败";
    }

}
