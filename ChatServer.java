package Bai_2;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private static final int PORT = 8080;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("WebSocket Server đang chạy tại ws://localhost:" + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                new Thread(client).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String message) {
        for (ClientHandler client : clients) {
            try {
                client.sendMessage(message);
            } catch (Exception e) {
                System.out.println("Lỗi gửi tin nhắn đến client.");
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private InputStream input;
        private OutputStream output;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();

                doHandshake(input, output);
                System.out.println("Một client đã kết nối.");

                while (true) {
                    String message = readMessage(input);
                    if (message == null) {
                        break;
                    }
                    System.out.println("Nhận: " + message);
                    broadcast(message);
                }
            } catch (Exception e) {
                System.out.println("Client đã ngắt kết nối.");
            } finally {
                clients.remove(this);
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void doHandshake(InputStream in, OutputStream out) throws Exception {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            String webSocketKey = null;

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Sec-WebSocket-Key:")) {
                    webSocketKey = line.substring("Sec-WebSocket-Key:".length()).trim();
                }
            }

            if (webSocketKey == null) {
                throw new RuntimeException("Không tìm thấy Sec-WebSocket-Key");
            }

            String acceptKey = generateAcceptKey(webSocketKey);

            String response =
                    "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        private String generateAcceptKey(String key) throws Exception {
            String magicString = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(magicString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }

        private String readMessage(InputStream in) throws IOException {
            int b1 = in.read();
            if (b1 == -1) return null;

            int b2 = in.read();
            if (b2 == -1) return null;

            boolean masked = (b2 & 0x80) != 0;
            int payloadLength = b2 & 0x7F;

            if (payloadLength == 126) {
                payloadLength = (in.read() << 8) | in.read();
            } else if (payloadLength == 127) {
                throw new IOException("Payload quá lớn");
            }

            byte[] maskingKey = new byte[4];
            if (masked) {
                for (int i = 0; i < 4; i++) {
                    maskingKey[i] = (byte) in.read();
                }
            }

            byte[] payload = new byte[payloadLength];
            int totalRead = 0;
            while (totalRead < payloadLength) {
                int read = in.read(payload, totalRead, payloadLength - totalRead);
                if (read == -1) return null;
                totalRead += read;
            }

            if (masked) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
                }
            }

            return new String(payload, StandardCharsets.UTF_8);
        }

        public void sendMessage(String message) throws IOException {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream frame = new ByteArrayOutputStream();

            frame.write(0x81);

            if (data.length <= 125) {
                frame.write(data.length);
            } else if (data.length <= 65535) {
                frame.write(126);
                frame.write((data.length >> 8) & 0xFF);
                frame.write(data.length & 0xFF);
            } else {
                throw new IOException("Tin nhắn quá dài");
            }

            frame.write(data);
            output.write(frame.toByteArray());
            output.flush();
        }
    }
}
