package org.example.multiQueues;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.SneakyThrows;
import org.example.RabbitConnection;
import org.junit.Test;

public class ProviderMultiqueues {
    @SneakyThrows
    @Test
    public void sendmessage(){
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();

        //这里指明对应的一个交换机，如果不存在，他会自动创建一个
        //后面的fanout是指明交换机的类型，前面就提及过，交换机有着不同的类型
        /*
            direct:最简单也是默认的，在传输消息时，我们之前是以routingKey参数指定队列名，
                   但实际这个参数没这么简单，在队列绑定时，它可以用于作为绑定键，以后的匹配可以用到
            topic:用来匹配多个队列，且队列名符合`x.y`的格式，即内部用`.`分隔开,
                  关键在于routingKey此时就变成已成匹配模式，`*`匹配任意一个单词，`#`匹配0个或多个
                  如`#.suibian.*.*`
            fanout:相当于广播，直接将消息发送给绑定的所有队列,此时routingKey是无用的
            headers:类似topic的匹配方式，只是这里的匹配是消息头
                    在通道声明时，有一个arguments的参数，那里可以用map类型指定各种变量和对应的值，就是header,
                    而在发送消息时，另一个方法
                    basicPublish(String exchange, String routingKey, boolean mandatory,
                    BasicProperties props, byte[] body)
                    其中的BasicProperties就对应着header,
                    于是，这就要求消息的头与对应的队列的arguments指定的头是相同的才行
         */
        //此外，rabbitmq本身就已经为我们的虚拟主机创建了对应的fanout类型交换机，但我们这里就任性地创建一个
        channel.exchangeDeclare("newEx","fanout");
        String queueName = channel.queueDeclare().getQueue();//这是一个临时队列
        //当完成任务后，就自动删除，也就是autoDelete为true
        System.out.println(queueName);//随机生成一个队列，
        // 往这里输入的消息，之后其它绑定到该交换机的队列均会收到消息
        channel.queueBind(queueName,"newEx","");
        channel.basicPublish("newEx","",null,"一条发布的消息".getBytes());
        RabbitConnection.closeChnnelAndconnection(channel,connection);
    }
}
