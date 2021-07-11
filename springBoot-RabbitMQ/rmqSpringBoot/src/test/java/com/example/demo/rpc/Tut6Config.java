package com.example.demo.rpc;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class Tut6Config {//这里作为设置类
	@Bean
	public DirectExchange exchange() {//指定交换机的名字
		return new DirectExchange("tut.rpc");
	}
	@Bean
	public Queue queue() {//指定请求队列
		return new Queue("tut.rpc.requests");
	}
	@Bean
	public Binding binding(DirectExchange exchange, Queue queue) {
		return BindingBuilder.bind(queue).to(exchange).with("rpc");
		//将交换机与请求队列绑定在一起
	}
}