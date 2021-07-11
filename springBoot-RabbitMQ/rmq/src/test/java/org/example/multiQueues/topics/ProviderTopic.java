package org.example.multiQueues.topics;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.SneakyThrows;
import org.example.RabbitConnection;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;


public class ProviderTopic {
    @SneakyThrows
    @Test
    public void sendMessage(){
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();
        channel.exchangeDeclare("Ex_topic","topic");//随便写个交换机名
        String[] routekeys={"Atopic.Btopic","Atopic.Btopic.Ctopic"};//随便写
        //同样这里只需要发送到交换机即可
        Arrays.asList(routekeys).forEach(s-> {
            try { channel.basicPublish("Ex_topic",s,null,
                        new String("使用了topic--"+s).getBytes());
            } catch (IOException e) { e.printStackTrace(); } });


        RabbitConnection.closeChnnelAndconnection(channel,connection);
    }
}
