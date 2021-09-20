package com.example.demo.mybatis.service;

import com.example.demo.pojo.Message;
import com.example.demo.pojo.User;
import com.example.demo.pojo.expand.UserAndFriends;
import com.example.demo.pojo.expand.UserMessage;

public interface UserService {
    public void add(User user);
    public User query(Integer id);
    public int create(String name);
    public int createMessageTableForFriends(UserAndFriends userAndFriends);
    public int judgeForTable(String tableName);
    public int insertMessageForFriends(UserMessage userMessage);
}
