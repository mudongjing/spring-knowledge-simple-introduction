package com.example.demo.rpc;


import com.example.demo.DemoApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = DemoApplication.class)
@RunWith(SpringRunner.class)
public class Tut6Client {
	@Autowired
	private RabbitTemplate template;
	@Autowired
	private DirectExchange exchange;
	@Test
	//@Scheduled(fixedDelay = 1000, initialDelay = 500) ，类似与定时器可以按指定间隔启动
	public void send() {
		String response = (String) template.
				convertSendAndReceive(exchange.getName(),
						"rpc", //这里就对应着设置类中绑定队列时附带的路由值，相当于使用Routing模式
						"一条远程命令");
		System.out.println(response);
	}
}