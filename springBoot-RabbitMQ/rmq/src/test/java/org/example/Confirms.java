package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BooleanSupplier;

public class Confirms {
    static final int MESSAGE_COUNT = 50_000;
    private static Connection connection=RabbitConnection.createConnect();
    @Test
    public  void publishMessagesIndividually() throws Exception {//仅发布一条消息
        Channel channel = connection.createChannel();
        String queue = UUID.randomUUID().toString();
        channel.queueDeclare(queue, false, false, true, null);
        channel.confirmSelect();//声明为发布者确认模式
        long start = System.nanoTime();//记录时间而已
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String body = String.valueOf(i);
            channel.basicPublish("", queue, null, body.getBytes());

            //等待直到自最后一次调用以来发布的所有消息已经被回应，这里包括成功和失败（当然不是不回应的失败）;
            // //如果在给定的时间内为回应，则抛异常
            channel.waitForConfirmsOrDie(5_000);
        }
        long end = System.nanoTime();//记录一下整个的时间而已
        System.out.format("Published %,d messages individually in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
    }
    @Test
    public void publishMessagesInBatch() throws Exception {//发布一个批次的消息
        Channel channel = connection.createChannel();
        String queue = UUID.randomUUID().toString();
        channel.queueDeclare(queue, false, false, true, null);
        channel.confirmSelect();
        int batchSize = 100;//表明一个批次的消息量为100
        int outstandingMessageCount = 0;
        long start = System.nanoTime();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String body = String.valueOf(i);
            channel.basicPublish("", queue, null, body.getBytes());
            outstandingMessageCount++;
            if (outstandingMessageCount == batchSize) {//达到一个批次的量了
                channel.waitForConfirmsOrDie(5_000);//等待之前的消息有回应
                outstandingMessageCount = 0;
            }
        }
        if (outstandingMessageCount > 0) {//剩余的不够一批次
            channel.waitForConfirmsOrDie(5_000);
        }
        long end = System.nanoTime();
        System.out.format("Published %,d messages in batch in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
    }
    @Test
    public void handlePublishConfirmsAsynchronously() throws Exception {//异步
        Channel channel = connection.createChannel();
        String queue = UUID.randomUUID().toString();
        channel.queueDeclare(queue, false, false, true, null);
        channel.confirmSelect();
        ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();//这里new的是一个跳表，不知道的读者可以了解一下（当然与下面的代码没有具体关系）
        ConfirmCallback cleanOutstandingConfirms = (sequenceNumber, multiple) -> {
            if (multiple) {//这里表示可以删除多个
                //headMap是指返回表中对应键值小于sequenceNumber的记录，后面的参数为 inclusive，如果为true，则表示可以是小于等于。
                ConcurrentNavigableMap<Long, String> confirmed = outstandingConfirms.headMap(sequenceNumber, true);
                confirmed.clear();
            } else { //这里表示只能删除一个
                outstandingConfirms.remove(sequenceNumber);
            }
        };

        /*
            两个参数均是ConfirmCallback类型，
            前者为 ackCallback：意味成功，就直接删除原有的消息了
            后者为 nackCallback：意味失败，则做一些表示，如下面输出哪个消息失败了，再执行删除
         */
        //到这里，基本为后面的具体操作，做了规划
        channel.addConfirmListener(cleanOutstandingConfirms, (sequenceNumber, multiple) -> {
            String body = outstandingConfirms.get(sequenceNumber);
            System.err.format(
                    "Message with body %s has been nack-ed. Sequence number: %d, multiple: %b%n",
                    body, sequenceNumber, multiple
            );
            cleanOutstandingConfirms.handle(sequenceNumber, multiple);
        });
        long start = System.nanoTime();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String body = String.valueOf(i);
            outstandingConfirms.put(channel.getNextPublishSeqNo(), body);
            channel.basicPublish("", queue, null, body.getBytes());
        }

        //如果60秒后，消息没有全部得到回应，就抛异常
        if (!waitUntil(Duration.ofSeconds(60), () -> outstandingConfirms.isEmpty())) {
            throw new IllegalStateException("All messages could not be confirmed in 60 seconds");
        }
        long end = System.nanoTime();
        System.out.format("Published %,d messages and handled confirms asynchronously in %,d ms%n", MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
    }

    public boolean waitUntil(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        int waited = 0;
        //不到60秒，而且消息还有剩余（只要有剩余，就说明消息没有全部得到回应），
        while (!condition.getAsBoolean() && waited < timeout.toMillis()) {//这里对应的是60秒的毫秒数
            Thread.sleep(100L);// 避免频繁循环，本来异步操作就是想慢慢等的，不急的
            waited = +100;//当等待的时间超过了60秒，就跳出循环
        }
        return condition.getAsBoolean();
    }

}
