package com.example.demo.routing;

import com.example.demo.DemoApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = DemoApplication.class)
@RunWith(SpringRunner.class)
public class ProviderRouting {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Test
    public void sendMessage(){
        rabbitTemplate.convertAndSend("routings","A_Key","一条routing消息");
    }
}
