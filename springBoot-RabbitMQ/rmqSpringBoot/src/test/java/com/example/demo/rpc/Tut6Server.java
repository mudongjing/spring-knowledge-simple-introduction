package com.example.demo.rpc;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class Tut6Server {
	//这里就是一直监听着指定的请求队列
	@RabbitListener(queues = "tut.rpc.requests")// @SendTo("tut.rpc.replies") used when the client doesn't set replyTo.
	//这里的返回类型也是随意的
	public String call(String message) {//自动获取消息的内容，这里也可以指定其它类型
		String result=responseCall(message);
		return result;
	}
	private  String responseCall(String message){//对应负责服务的方法
		String reslut="你已经来过服务器了--"+message+"!!!";
		return reslut;
	}
}