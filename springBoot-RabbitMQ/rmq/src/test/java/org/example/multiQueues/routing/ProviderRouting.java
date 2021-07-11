package org.example.multiQueues.routing;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.SneakyThrows;
import org.example.RabbitConnection;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class ProviderRouting {
    @SneakyThrows
    @Test
    public void sendMessage(){
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();
        channel.exchangeDeclare("Ex_direct","direct");
        String[] routingkey={"A_Key","B_Key"};//这里我们随便定义一个键值
        Arrays.asList(routingkey).forEach(s -> {
            try { channel.basicPublish("Ex_direct",s,null,
                        new String("指定了一个键值 "+s).getBytes());
            } catch (IOException e) { e.printStackTrace(); } });


        RabbitConnection.closeChnnelAndconnection(channel,connection);
    }
}
