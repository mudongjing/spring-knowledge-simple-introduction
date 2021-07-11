package org.example.multiQueues.multiConsumers;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import org.example.RabbitConnection;

import java.io.IOException;

public class ConsumerMulQueExample {
    @SneakyThrows
    public void example(final String consumer_name){
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();

        //这里将绑定指定的交换机，其实这里也可以继续使用channel.exchangeDeclare("newEx","fanout");
        //这个方法则侧重于多个交换机的场景，
        /*
            destination:指定消息目的地的交换机，就是我们此时创建的
            source:就是消息来源对应的交换机，此时也就是目的地自己
            routingKey:此时还是没用的参数
         */
        channel.exchangeBind("newEx","newEx","");
        //channel.exchangeDeclare("newEx","fanout");
        String queueName = channel.queueDeclare().getQueue();
        System.out.println(queueName);//不同消费者都拥有一个自己独特的队列，
        // 但每个队列都会收到相同的消息，供对应的消费者使用
        //如果不使用这种随机队列的方式，读者则需要额外为不同消费者创建对应不同的队列
        channel.queueBind(queueName,"newEx","");

        //消费也是老样子，
        //读者可能会说了，这样子好像没什么区别，就加了个交换机，还没什么用
        //实际上，这一操作，官网上称之为 订阅/发布 模式，
        // 我们虽然只发送了一条消息，都是如果多个消费者建立了此类连接，这一条消息可以被多个消费者消费
        channel.basicConsume(queueName,true,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println(consumer_name+new String(body)+"\n---------");
            }
        });

        //RabbitConnection.closeChnnelAndconnection(channel,connection);
    }
}
