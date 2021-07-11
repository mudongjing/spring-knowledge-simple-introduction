package com.example.demo.PublishSubscribe;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumerSubscribe {
    @RabbitListener(bindings = {
            @QueueBinding(value=@Queue,exchange = @Exchange(value="subscribe",type="fanout"))
    })
    public void consume(String message){
        System.out.println("consume--"+message);
    }
}
