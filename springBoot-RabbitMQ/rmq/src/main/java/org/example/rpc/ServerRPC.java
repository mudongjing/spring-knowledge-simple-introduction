package org.example.rpc;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import org.example.RabbitConnection;

public class ServerRPC {
    //这里是我们将要使用的请求队列的名字
    private static final String RPC_QUEUE_NAME = "rpc_queue";
    //首先定义一个用于提供服务的方法，当然可以多定义几个，只要客户端传送的消息能够指定把不同的服务即可
    private static String responseCall(String message){
        String reslut="你已经来过服务器了"+message+"!!!";
        return reslut;
    }
    @SneakyThrows
    public static void main(String[] args){
        Connection connection= RabbitConnection.createConnect();
        Channel channel=connection.createChannel();
        channel.queueDeclare(RPC_QUEUE_NAME,false,false,false,null);
        //channel.queuePurge(RPC_QUEUE_NAME); // 如果该请求队列是已有的，可能内部有其它剩余的消息，可以使用该方法清理
        channel.basicQos(1);//指明服务器每次能传递的消息最大数量，如果是0反而代表没有限制
        Object monitor=new Object();//用与后面的同步锁
        //之前我们进行消费的 basicConsume都是使用的DefaultConsumer,这里也可以使用
        //但是官方的示例代码使用了 DeliverCallback ，我们也尝试一下
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {//这是用lambda的方式实现一个接口，接口必须只有一个方法
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())//指明该请求对应的唯一值，方便客户端判断结果对应的是哪个请求
                    .build();
            String response = "";
            try {
                String message = new String(delivery.getBody(), "UTF-8");//获取请求内容
                response += responseCall(message);//完成服务
            } catch (RuntimeException e) { System.out.println(" [.] " + e.toString());
            } finally {
                //接下来就是将结果发送到指定的队列中
                /*
                    对应的参数是，
                    exchange:这里我们没有设置交换机，
                    routingKey:这里的getReplyTo()返回客户端传送过来的回调队列的名字
                    BasicProperties:这里就是指明结果的各种信息
                    body[]:消息主体
                 */
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                //手动发送确认，因为我们的请求队列没有设置为自动确认。
                /*
                    deliveryTag；对应的是消息是否确实获取到
                    mutiple:对应的是是否用于确认多条消息的确认，这里的false表明一条一条地确认
                 */
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                // RabbitMq的消费者工作线程 通知  RPC 服务器 拥有者的线程
                synchronized (monitor) { monitor.notify(); }
            }
        };
        //消费请求队列中的消息
        channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> { }));
        // 等待并时刻准备着消费客户端发来的消息.
        while (true) {
            synchronized (monitor) {
                try { monitor.wait(); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
    }
}
