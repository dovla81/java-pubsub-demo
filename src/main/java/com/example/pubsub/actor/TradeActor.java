package com.example.pubsub.actor;

import com.example.pubsub.Trade;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class TradeActor implements Runnable {
    protected final BlockingQueue<Trade> inbox;
    protected volatile boolean running;
    protected TradeActor nextActor;

    public TradeActor() {
        this.inbox = new LinkedBlockingQueue<>();
        this.running = true;
    }

    public void send(Trade trade) {
        inbox.offer(trade);
    }

    public void setNextActor(TradeActor nextActor) {
        this.nextActor = nextActor;
    }

    public void stop() {
        running = false;
    }

    protected void forward(Trade trade) {
        if (nextActor != null) {
            nextActor.send(trade);
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Trade trade = inbox.take();
                process(trade);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    protected abstract void process(Trade trade);
}
