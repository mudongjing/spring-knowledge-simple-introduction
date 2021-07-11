package com.example.demo.routing;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumerRouting {
    @RabbitListener(bindings = {@QueueBinding(value=@Queue,
            exchange = @Exchange(value="routings"),//因为默认type就是direct，这里不需要再写
            key = {"A_Key"})})
    public void consume(String message){
        System.out.println("Routing消费者--"+message);
    }
}
