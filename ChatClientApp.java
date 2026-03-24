package Bai_2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public class ChatClientApp extends Application {

    private TextArea chatArea;
    private TextField nameField;
    private TextField messageField;
    private Button connectButton;
    private Button sendButton;

    private WebSocket webSocket;
    private boolean connected = false;

    @Override
    public void start(Stage stage) {
        Label nameLabel = new Label("Tên:");
        nameField = new TextField();
        nameField.setPromptText("Nhập tên của bạn");

        connectButton = new Button("Kết nối");
        connectButton.setOnAction(e -> connectToServer());

        HBox topBox = new HBox(10, nameLabel, nameField, connectButton);
        topBox.setPadding(new Insets(10));

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        messageField = new TextField();
        messageField.setPromptText("Nhập tin nhắn...");
        messageField.setPrefWidth(300);

        sendButton = new Button("Gửi");
        sendButton.setOnAction(e -> sendMessage());

        messageField.setOnAction(e -> sendMessage());

        HBox bottomBox = new HBox(10, messageField, sendButton);
        bottomBox.setPadding(new Insets(10));

        VBox root = new VBox(10, topBox, chatArea, bottomBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 500, 400);
        stage.setTitle("Chat Local - JavaFX WebSocket");
        stage.setScene(scene);
        stage.show();
    }

    private void connectToServer() {
        if (connected) {
            appendMessage("Đã kết nối");
            return;
        }

        String username = nameField.getText().trim();
        if (username.isEmpty()) {
            appendMessage(" nhập tên trước khi kết nối.");
            return;
        }

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:8080"), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        ChatClientApp.this.webSocket = webSocket;
                        connected = true;
                        appendMessage("Kết nối thành công tới server");
                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        appendMessage(data.toString());
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        connected = false;
                        appendMessage("Đã ngắt kết nối");
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        connected = false;
                        appendMessage("Lỗi kết nối: " + error.getMessage());
                    }
                });
    }

    private void sendMessage() {
        if (!connected || webSocket == null) {
            appendMessage("Bạn chưa kết nối tới server.");
            return;
        }

        String username = nameField.getText().trim();
        String message = messageField.getText().trim();

        if (username.isEmpty() || message.isEmpty()) {
            return;
        }

        String fullMessage = username + ": " + message;
        webSocket.sendText(fullMessage, true);
        messageField.clear();
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> chatArea.appendText(message + "\n"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
