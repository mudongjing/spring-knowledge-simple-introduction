package org.example.simple;

import com.rabbitmq.client.*;
import org.example.RabbitConnection;
import org.junit.Test;

import java.io.IOException;

public class Consumertest {
    @Test
    public void consumeMessage(){
        Connection connection= RabbitConnection.createConnect();
        try {
            Channel channel=connection.createChannel();
            //对应的队列声明设置需要和我们生产者对该队列的设置相同
            channel.queueDeclare("first",true,false,
                    false,null);
            //下面的方法就是进行消息消费的
            /*
                对应的参数依次为，
                queue:指明对应的队列
                autoAck:是否要自动确认，若是true,当提取消息时，该消息传送到套接字缓冲区，队列就认为消息成功发送给消费者了
                        ，此时队列就会删除它。如果设为false,则消费者会发送会确认消息，队列才会删除，否则消息将处于Unacked状态。
                callback:回调函数，类型是个Consumer.
             */
            channel.basicConsume("first",true,new DefaultConsumer(channel){
                //用一个匿名内部类作为参数
                //其中body就是我们获取到的消息，这里只是简单打印出来
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    System.out.println(new String(body+"\n---------"));
                }
            });
            RabbitConnection.closeChnnelAndconnection(channel,connection);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
