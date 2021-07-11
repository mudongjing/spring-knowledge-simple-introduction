package com.example.demo.workqueues;

import com.example.demo.DemoApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.stream.IntStream;

@SpringBootTest(classes = DemoApplication.class)
@RunWith(SpringRunner.class)
public class ProviderWork {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Test
    public void sendMessage(){
        IntStream.rangeClosed(0, 10).boxed().forEach(i->rabbitTemplate.convertAndSend("work","work模式内容"+i));
    }
}
