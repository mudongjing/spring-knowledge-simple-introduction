package com.example.demo.mybatis.service.impl;

import com.example.demo.mybatis.mapper.UserMapper;
import com.example.demo.mybatis.service.UserService;
import com.example.demo.pojo.Message;
import com.example.demo.pojo.User;
import com.example.demo.pojo.expand.UserAndFriends;
import com.example.demo.pojo.expand.UserMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Resource
    private UserMapper userMapper;


    @Override
    public void add(User user) {
        userMapper.insert(user);
    }

    @Override
    public User query(Integer id) {
        String userKey="user:"+id;
        Object object=redisTemplate.opsForValue().get(userKey);
        if(object==null){
            synchronized (this.getClass()){
                if((object=redisTemplate.opsForValue().get(userKey)) ==null){
                    log.debug("==== 走数据库查询");
                    User user=userMapper.selectByPrimaryKey(id);
                    redisTemplate.opsForValue().set(userKey,user);
                    return user;
                }else{
                    log.debug("来自redis缓存(同步代码块) 》》》》》》》》》》》");
                    User user=new Gson().fromJson(object.toString(),User.class);
                    return user;
                }
            }
        }else{
            log.debug("来自redis缓存》》》》》》》》》》》");
            User user=new Gson().fromJson(object.toString(),User.class);
            return user;
        }
    }

    @Override
    public int create(String name) {
        return userMapper.createMessageTable(name);
    }

    @Override
    public int createMessageTableForFriends(UserAndFriends userAndFriends) {
        return userMapper.createForFriends(userAndFriends);
    }

    @Override
    public int judgeForTable(String tableName) {
        return userMapper.judgeForTable(tableName).size();
    }

    @Override //完成该函数，实现向表中插入消息，同时判断消息是否为大文本
    public int insertMessageForFriends(UserMessage userMessage) {
        return 0;
    }
}
