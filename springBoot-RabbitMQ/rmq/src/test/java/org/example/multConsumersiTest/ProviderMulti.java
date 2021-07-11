package org.example.multConsumersiTest;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import lombok.SneakyThrows;
import org.example.RabbitConnection;
import org.junit.Test;

public class ProviderMulti {
    @SneakyThrows
    @Test
    public void sendMessage(){
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();
        channel.queueDeclare("first",true,false,
                false,null);
        for (int i=0;i<20;i++){
            channel.basicPublish("","first",
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    new String("随便写点"+i).getBytes());
            Thread.sleep(1200);
        }
        RabbitConnection.closeChnnelAndconnection(channel,connection);

    }
}
