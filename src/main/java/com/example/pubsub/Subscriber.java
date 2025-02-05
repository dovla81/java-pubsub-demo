package com.example.pubsub;

public interface Subscriber {
    void onMessage(Message message);
    String getTopic();
}
