package org.example.multiQueues.topic;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import org.example.RabbitConnection;

import java.io.IOException;
import java.util.Arrays;

public class ConsumerTopicExample {
    @SneakyThrows
    public void example(final String consumer_name,String[] routes){
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();
        channel.exchangeDeclare("Ex_topic","topic");
        String queueName=channel.queueDeclare().getQueue();
        Arrays.asList(routes).forEach(s -> {
            try { channel.queueBind(queueName,"Ex_topic",s); }
            catch (IOException e) { e.printStackTrace(); } });

        channel.basicConsume(queueName,true,new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println(consumer_name+new String(body));
            }
        });
    }
}
