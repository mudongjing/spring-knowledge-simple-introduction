package com.example.demo.workqueues;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queuesToDeclare = @Queue(value="work",durable = "false",autoDelete = "false"))
public class ConsumerWork {
    @RabbitHandler
    public void consume1(String message){
        System.out.println("work消费者1"+message);
    }
    @RabbitHandler
    public void consume2(String message){
        System.out.println("work消费者2"+message);
    }
}
