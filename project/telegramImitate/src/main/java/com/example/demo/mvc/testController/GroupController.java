package com.example.demo.mvc.testController;

import com.example.demo.mybatis.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupController {
    @Autowired
    private GroupService groupService;

    @GetMapping("/group/create/{groupName}")
    public String cerateGroupItem(@PathVariable("groupName") String groupName){
        int i=groupService.insertItem(groupName);
        if(i>=0) return "success";
        else return "false";
    }
}
