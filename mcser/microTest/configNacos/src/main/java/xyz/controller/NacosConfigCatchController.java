package xyz.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
@RefreshScope //能够让从对应配置文件中利用@value获取的内容动态刷新
public class NacosConfigCatchController {
    @Value("${user.name}")
    private String userName;//如果没有@RefreshScope，一次获取后，对应的改动将无法获得
    @Value("${user.password}")
    private String userPassword;

    @RequestMapping("/get")
    public String config(){
        return "用户名"+userName+"_ 密码:  "+userPassword;
    }
}
