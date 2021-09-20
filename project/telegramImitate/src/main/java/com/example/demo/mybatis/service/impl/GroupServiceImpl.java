package com.example.demo.mybatis.service.impl;

import com.example.demo.mybatis.mapper.GroupMapper;
import com.example.demo.mybatis.service.GroupService;
import com.example.demo.pojo.Group;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class GroupServiceImpl implements GroupService {
    @Resource
    private GroupMapper groupMapper;
    @Override
    public int insertItem(String name) {
        Group group=new Group(null,name,2,"ff","groupMessage");
        return groupMapper.insertGroupItem(group);
    }
}
