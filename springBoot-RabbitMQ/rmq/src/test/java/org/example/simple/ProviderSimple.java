package org.example.simple;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import org.example.RabbitConnection;
import org.junit.Test;

import java.io.IOException;

public class ProviderSimple {
    @Test
    public void sendMessage(){
        Connection connection= RabbitConnection.createConnect();
        try {
            Channel channel=connection.createChannel();//创建通道，主要利用这个通道来进行操作
            //下面对应的参数分别的含义是
            /*
                queue:指定队列的名字
                durable:是否持久化，如果true，那么我们的结果都将存储到磁盘中，否则，重启后便消失
                exclusive:是否独占队列，如果怕其它用户影响，可以独占
                autoDelete:队列内的消息用完了，那么这个空队列要不要删除
                arguments:额外的参数，是一个map类型的
             */
            //随便写个first的队列名
            channel.queueDeclare("first",true,false,false,null);
            //通道声明一个队列，表明当前通道对该队列的操作的基本设置，不代表通道只能使用这个队列，
            // 因此，一个通道可以声明多个队列
            //即使我们现在没有添加过first队列，他也会自动创建的

            //这是实际进行消息发布的方法

                channel.basicPublish("","first", MessageProperties.PERSISTENT_TEXT_PLAIN,"随便写点".getBytes());

            /*
                上述的参数，对应如下
                exchange:指定交换机，但我们现在还不需要
                routingKey:是一个路由键，这里主要用于指定队列名
                props:其它关于消息的设置,没有可以设置为null,此时我们的设置是要求发送的信息被持久化
                body:最后是消息内容，且类型为byte[]
             */

            RabbitConnection.closeChnnelAndconnection(channel,connection);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
