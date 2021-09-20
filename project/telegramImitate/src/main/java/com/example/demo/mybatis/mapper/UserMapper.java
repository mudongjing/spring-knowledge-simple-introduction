package com.example.demo.mybatis.mapper;

import com.example.demo.pojo.User;
import com.example.demo.pojo.expand.UserAndFriends;
import com.example.demo.pojo.expand.UserMessage;
import tk.mybatis.mapper.common.Mapper;

import java.util.Set;

public interface UserMapper extends Mapper<User> {
    int createMessageTable(String tableName);
    int insertUserItem(User user);
    // 用户创建与其它用户关联的消息表
    int createForFriends(UserAndFriends userAndFriends);
    Set judgeForTable(String tableName);
    int insertUserMessage(UserMessage userMessage);
}
