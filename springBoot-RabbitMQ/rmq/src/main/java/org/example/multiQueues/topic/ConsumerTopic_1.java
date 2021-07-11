package org.example.multiQueues.topic;

public class ConsumerTopic_1 {
    public static void main(String[] args){
        new ConsumerTopicExample().example("consumerTopic_1",
                new String[]{"Atopic.*"});
    }
}
