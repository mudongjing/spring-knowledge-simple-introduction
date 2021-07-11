package com.example.demo.PublishSubscribe;

import com.example.demo.DemoApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = DemoApplication.class)
@RunWith(SpringRunner.class)
public class ProviderPublish {
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    public void sendMessage(){
        rabbitTemplate.convertAndSend("subscribe","","publish的内容");
    }
}
