package com.example.demo.mvc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserRedisController {
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/redis/get/{key}")
    public Object get(@PathVariable("key") String key){

        return redisTemplate.opsForValue().get(key);
    }
    @PostMapping("redis/set/{key}/{value}")
    public String set(@PathVariable("key") String key,@PathVariable("value") String value){
        redisTemplate.opsForValue().set(key,value);
        return key+" :"+value;
    }

//    public static void main(String[] args) {
//        String s="12,3,4,5";
//        String[] ss=s.split(",");
//        Integer[] a=new Integer[ss.length];
//        for(int i=0;i<ss.length;i++){
//            a[i]=Integer.parseInt(ss[i]);
//            System.out.println(a[i]+a[i].getClass().getName());
//        }
//
//    }
}