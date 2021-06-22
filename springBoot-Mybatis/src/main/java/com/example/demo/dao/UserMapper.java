package com.example.demo.dao;

import com.example.demo.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
public interface UserMapper {
    User selectUser(String name);
    List<User> selectAll();
    void addUser(User user);
}
