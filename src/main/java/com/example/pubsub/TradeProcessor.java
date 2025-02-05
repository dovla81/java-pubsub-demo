package com.example.pubsub;

import com.example.pubsub.exceptions.TradeGenerationException;
import com.example.pubsub.exceptions.TradeQueueException;
import com.example.pubsub.exceptions.TradeProcessingException;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.DoNotSend;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

public class TradeProcessor implements AutoCloseable, Application {
    private static final String CONFIG_FILE = "src/main/resources/fixconfig.cfg";
    private static final String FIX_DATA_DIR = "target/data/fix";
    private static final String FIX_LOG_DIR = "target/data/fix/log";
    
    private final ScheduledExecutorService executorService;
    private final LinkedBlockingQueue<Trade> tradeQueue;
    private volatile boolean running;
    private SocketAcceptor acceptor;
    private SessionID sessionId;
    
    public TradeProcessor() {
        this.executorService = Executors.newScheduledThreadPool(2);
        this.tradeQueue = new LinkedBlockingQueue<>();
        this.running = true;
        createDirectories();
        initializeFIX();
    }

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(FIX_DATA_DIR));
            Files.createDirectories(Paths.get(FIX_LOG_DIR));
        } catch (IOException e) {
            throw new TradeProcessingException("Failed to create FIX directories", e);
        }
    }

    private void initializeFIX() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                throw new FileNotFoundException("FIX configuration file not found: " + CONFIG_FILE);
            }

            SessionSettings settings = new SessionSettings(new FileInputStream(configFile));
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new FileLogFactory(settings);
            MessageFactory messageFactory = new DefaultMessageFactory();
            
            acceptor = new SocketAcceptor(
                    this, storeFactory, settings, logFactory, messageFactory);
            
            acceptor.start();
            
            // Store the first session ID for later use
            if (!acceptor.getSessions().isEmpty()) {
                sessionId = acceptor.getSessions().get(0);
            }
        } catch (ConfigError | FileNotFoundException e) {
            throw new TradeProcessingException("Failed to initialize FIX", e);
        }
    }

    public void start() {
        if (!running) {
            throw new TradeProcessingException("Cannot start: TradeProcessor is not running");
        }
        
        if (acceptor == null || !acceptor.isLoggedOn()) {
            throw new TradeProcessingException("Cannot start: FIX acceptor is not ready");
        }
        
        // Schedule trade processing every 5 seconds
        executorService.scheduleAtFixedRate(this::processTrades, 0, 5, TimeUnit.SECONDS);
    }

    private void processTrades() {
        if (!running) return;

        try {
            System.out.println("\n=== Processing trades at 5-second mark ===");
            Trade trade = tradeQueue.poll();
            
            if (trade != null) {
                System.out.printf("Processed trade: %s%n", trade);
            }
        } catch (Exception e) {
            throw new TradeProcessingException("Error processing trades", e);
        }
    }

    @Override
    public void close() {
        System.out.println("bye bye");
        running = false;
        
        // Stop accepting new trades
        if (acceptor != null) {
            try {
                // Logout all sessions gracefully
                for (SessionID sid : acceptor.getSessions()) {
                    Session.lookupSession(sid).logout("Application shutting down");
                }
                // Wait for logout to complete
                Thread.sleep(1000);
                acceptor.stop(true); // true = force stop if necessary
            } catch (Exception e) {
                System.err.println("Error during FIX shutdown: " + e.getMessage());
            }
        }

        // Shutdown executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("ExecutorService did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear any remaining trades
        tradeQueue.clear();
    }

    // QuickFIX/J Application interface methods
    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("Session created: " + sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Logon - " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Logout - " + sessionId);
    }

    @Override
    public void toAdmin(quickfix.Message message, SessionID sessionId) throws DoNotSend {
        // This method is called before an administrative message is sent to the counterparty
        // For basic FIX functionality, we can leave this empty as the default behavior is sufficient
    }

    @Override
    public void toApp(quickfix.Message message, SessionID sessionId) throws DoNotSend {
        // Validate outgoing application messages
        try {
            message.validate();
        } catch (Exception e) {
            throw new DoNotSend("Invalid outgoing message: " + e.getMessage());
        }
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        try {
            // Validate incoming message
            message.validate();
            
            if (message instanceof ExecutionReport) {
                ExecutionReport executionReport = (ExecutionReport) message;
                
                // Validate required fields
                if (!executionReport.hasSymbol() || !executionReport.hasLastPx() || !executionReport.hasLastQty()) {
                    throw new IncorrectTagValue("Missing required fields in ExecutionReport");
                }
                
                // Extract trade information from FIX message
                String symbol = executionReport.getSymbol().getValue();
                double price = executionReport.getLastPx().getValue();
                int quantity = executionReport.getLastQty().getValue();
                
                // Validate values
                if (price <= 0 || quantity <= 0) {
                    throw new IncorrectTagValue("Invalid price or quantity values");
                }
                
                // Create and queue the trade
                Trade trade = new Trade(symbol, price, quantity);
                if (!tradeQueue.offer(trade)) {
                    throw new TradeQueueException("Failed to add trade to queue - queue full");
                }
                
                System.out.printf("Received FIX trade: %s%n", trade);
            }
        } catch (Exception e) {
            if (e instanceof FieldNotFound || e instanceof IncorrectDataFormat || 
                e instanceof IncorrectTagValue || e instanceof UnsupportedMessageType) {
                throw e;
            }
            throw new TradeProcessingException("Error processing FIX message", e);
        }
    }

    public static void main(String[] args) {
        try (TradeProcessor processor = new TradeProcessor()) {
            System.out.println("Starting trade processor...");
            System.out.println("Listening for FIX messages on port 9876");
            System.out.println("Press Ctrl+C to exit");
            
            processor.start();
            
            // Keep the application running
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Trade processor stopped");
        } catch (TradeProcessingException e) {
            System.err.println("Fatal error in trade processing: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
            }
            Thread.currentThread().interrupt();
        }
    }
}
