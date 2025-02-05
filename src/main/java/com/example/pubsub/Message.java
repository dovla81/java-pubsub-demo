package com.example.pubsub;

public class Message {
    private final String content;
    private final String topic;

    public Message(String content, String topic) {
        this.content = content;
        this.topic = topic;
    }

    public String getContent() {
        return content;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public String toString() {
        return "Message{topic='" + topic + "', content='" + content + "'}";
    }
}
