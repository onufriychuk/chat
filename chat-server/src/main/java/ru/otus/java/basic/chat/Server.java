package ru.otus.java.basic.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private final AuthenticationProvider authenticationProvider;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port, AuthenticationProvider authenticationProvider) {
        this.port = port;
        clients = new ArrayList<>();
        this.authenticationProvider = authenticationProvider;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("Системное сообщение: сервер запущен на порту " + port); // переделать на логирование
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastMessage("Системное сообщение: пользователь " + clientHandler.getUsername() + " вошёл в чат.");
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Системное сообщение: пользователь " + clientHandler.getUsername() + " вышел из чата.");
    }

    public synchronized List<String> getUserList() {
        return clients.stream()
                .map(ClientHandler::getUsername)
                .collect(Collectors.toList());
    }


}
