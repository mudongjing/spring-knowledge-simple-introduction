package org.example.multiQueues.routing;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import org.example.RabbitConnection;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ConsumerRoutingExample {
   @SneakyThrows
    public void example(final String consumer_name, String[] routingkeys) {
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();
        channel.exchangeDeclare("Ex_direct","direct");
        String queueName = channel.queueDeclare().getQueue();
        Arrays.asList(routingkeys).forEach( s -> {
            try { channel.queueBind(queueName,"Ex_direct",s); }
            catch (IOException e) { e.printStackTrace(); }});

        channel.basicConsume(queueName,true,new DefaultConsumer(channel){
            @SneakyThrows
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                System.out.println(consumer_name+new String(body));
            }
        });

    }
}
