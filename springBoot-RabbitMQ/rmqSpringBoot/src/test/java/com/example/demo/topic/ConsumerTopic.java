package com.example.demo.topic;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumerTopic {
    @RabbitListener(bindings = @QueueBinding(value=@Queue,
            exchange = @Exchange(value="topics",type="topic"),key={"A_Key.*"}))
    public void consume(String message){
        System.out.println("consumetopic--"+message);
    }
}
