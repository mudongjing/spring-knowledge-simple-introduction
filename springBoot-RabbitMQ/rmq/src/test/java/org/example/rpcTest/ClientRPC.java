package org.example.rpcTest;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.SneakyThrows;
import org.example.RabbitConnection;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ClientRPC {
    //指明请求队列的名字
    private String requestQueueName = "rpc_queue";
    @Test
    @SneakyThrows
    public void call(){//实际使用时，这个是作为主函数的调用方法，内部包含消息的参数，这里是方便测试
        String message="请求服务消息";
        String result=null;
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();
        final String corrId = UUID.randomUUID().toString();//为消息准备一个唯一值，随便那什么方法生成，只要保证唯一就行
        String replyQueueName = channel.queueDeclare().getQueue();//用于作为回调队列的名字

        /*
        属性设置的常用内容：
            delivery_mode（投递模式）：将消息标记为持久的（值为2）或暂存的（除了2之外的其他任何值）。第二篇教程里接触过这个属性，记得吧？
            content_type（内容类型）:用来描述编码的 mime-type。例如在实际使用中常常使用 application/json 来描述 JOSN 编码类型。
            reply_to（回复目标）：通常用来命名回调队列。
            correlation_id（关联标识）：用来将RPC的响应和请求关联起来。
        */
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)//指定消息的唯一值
                .replyTo(replyQueueName)//指定结果进入的回调队列
                .build();
        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));
        //用于接收结果,容量是一个
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        //消费回调队列中的结果
        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8")); }}, consumerTag -> {});
        result = response.take();
        channel.basicCancel(ctag);//取消队列的订阅关系
        RabbitConnection.closeChnnelAndconnection(channel,connection);
        System.out.println(result);
    }
}
