package com.example.demo.helloWorld;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queuesToDeclare = @Queue(value = "hello",durable = "true"))//表示这是一个消费者，并指明消费的队列名
public class ConsumerHello {
    @RabbitHandler
    public void consumer(String message){
        System.out.println("消费者 "+message);
    }
}
