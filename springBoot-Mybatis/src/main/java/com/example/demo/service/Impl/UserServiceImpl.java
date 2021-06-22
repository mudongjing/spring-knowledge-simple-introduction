package com.example.demo.service.Impl;

import com.example.demo.dao.UserMapper;
import com.example.demo.pojo.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private UserMapper userMapper;
    @Autowired
    public void setUserMapper(UserMapper userMapper){
        this.userMapper=userMapper;
    }
    @Override
    public User selectUser(String name) {
        User user=new User();
        user=userMapper.selectUser(name);
        return user;
    }

    @Override
    public void addUser(User user) {
        userMapper.addUser(user);
    }

    @Override
    public List<User> findAll() {
        List<User> userList=new ArrayList<>();
        userList=userMapper.selectAll();
        return userList;
    }
}
