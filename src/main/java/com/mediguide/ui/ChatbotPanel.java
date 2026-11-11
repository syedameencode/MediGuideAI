package com.mediguide.ui;

import com.mediguide.ai.MediGuideChatEngine;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.concurrent.CompletableFuture;

public class ChatbotPanel extends VBox {

    private VBox chatHistory;
    private TextField inputField;
    private ScrollPane scrollPane;
    private final MediGuideChatEngine chatEngine = new MediGuideChatEngine();

    public ChatbotPanel() {
        initUI();
        addBotMessage("Hello! 👋 I am MediGuide AI Assistant. I can help you with:\n\n"
                + "• Register new patients — type 'new patient'\n"
                + "• Search patients — type 'search <name>'\n"
                + "• List all patients — type 'list patients'\n"
                + "• View patient details — type 'patient #ID'\n"
                + "• Medical info — ask about BP, glucose, BMI, etc.\n"
                + "• Navigation help — 'how to enter vitals'\n\n"
                + "Type 'help' for all commands!");
    }

    private void initUI() {
        setSpacing(10);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        setPrefWidth(300);
        setMaxHeight(500);

        Label header = new Label("\uD83E\uDD16 AI Assistant");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");

        chatHistory = new VBox(8);
        chatHistory.setPadding(new Insets(5));
        
        scrollPane = new ScrollPane(chatHistory);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f8fafc; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Auto-scroll to bottom when new messages are added
        chatHistory.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollPane.setVvalue(1.0);
        });

        HBox inputBox = new HBox(5);
        inputField = new TextField();
        inputField.setPromptText("Ask AI...");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> sendMessage());
        
        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold;");
        sendBtn.setOnAction(e -> sendMessage());

        inputBox.getChildren().addAll(inputField, sendBtn);

        getChildren().addAll(header, scrollPane, inputBox);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        inputField.clear();
        addUserMessage(text);
        
        Label typingLabel = addBotMessage("Thinking...");

        // Process asynchronously to keep UI responsive
        CompletableFuture.supplyAsync(() -> chatEngine.processMessage(text))
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        chatHistory.getChildren().remove(typingLabel);
                        addBotMessage(response);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        chatHistory.getChildren().remove(typingLabel);
                        addBotMessage("⚠️ Error: " + ex.getMessage());
                    });
                    return null;
                });
    }

    public void analyzeContext(String promptContext) {
        addUserMessage("Analyze these results...");
        Label typingLabel = addBotMessage("Analyzing...");

        CompletableFuture.supplyAsync(() -> chatEngine.processAnalysis(promptContext))
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        chatHistory.getChildren().remove(typingLabel);
                        addBotMessage(response);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        chatHistory.getChildren().remove(typingLabel);
                        addBotMessage("⚠️ Analysis error: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void addUserMessage(String text) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-padding: 8; -fx-background-radius: 8;");
        
        HBox row = new HBox(msg);
        row.setPadding(new Insets(2, 2, 2, 20));
        row.setStyle("-fx-alignment: center-right;");
        chatHistory.getChildren().add(row);
    }

    private Label addBotMessage(String text) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-padding: 8; -fx-background-radius: 8;");
        
        HBox row = new HBox(msg);
        row.setPadding(new Insets(2, 20, 2, 2));
        row.setStyle("-fx-alignment: center-left;");
        chatHistory.getChildren().add(row);
        return msg;
    }
}
