package org.example.multiconsumers;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import org.example.RabbitConnection;

import java.io.IOException;

public class ConsumerExample {
    @SneakyThrows//使用lombok代替实现异常处理,主要是完成try catch操作
    public void example(final String consumer_name,final int sleep_time){
        Connection connection= RabbitConnection.createConnect();
        final Channel channel=connection.createChannel();
        channel.queueDeclare("first",true,false,
                false,null);
        channel.basicQos(1);//要求一个通道依此只能处理一条消息
        channel.basicConsume("first",false,new DefaultConsumer(channel){
            @SneakyThrows
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                System.out.println(consumer_name+"---"+
                                   new String(body)+"\n---------");
                //进行手动确认，如果没有这个语句，前面的消息则会一直处于为为确认状态
                channel.basicAck(envelope.getDeliveryTag(),false);
                /*
                    上述的参数对应如下，
                    deliveryTag:指明消息标识，以确认具体是哪个消息
                    multiple:是否允许依此处理多条消息的确认
                 */
                Thread.sleep(sleep_time);
            }
        });
        //RabbitConnection.closeChnnelAndconnection(channel,connection);
    }
}
