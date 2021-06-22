package com.example.demo.service;

import com.example.demo.pojo.User;

import java.util.List;

public interface UserService {
    User selectUser(String name);
    List<User> findAll();
    void addUser(User user);
}
