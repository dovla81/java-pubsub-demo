package com.example.pubsub;

public class TopicSubscriber implements Subscriber {
    private final String name;
    private final String topic;

    public TopicSubscriber(String name, String topic) {
        this.name = name;
        this.topic = topic;
    }

    @Override
    public void onMessage(Message message) {
        System.out.printf("Subscriber %s received message on topic %s: %s%n",
                name, topic, message.getContent());
    }

    @Override
    public String getTopic() {
        return topic;
    }
}
