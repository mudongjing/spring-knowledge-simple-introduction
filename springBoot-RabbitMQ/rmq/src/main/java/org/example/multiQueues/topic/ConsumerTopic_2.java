package org.example.multiQueues.topic;

public class ConsumerTopic_2 {
    public static void main(String[] args){
        new ConsumerTopicExample().example("consumerTopic_2",
                new String[]{"*.#"});
    }
}
