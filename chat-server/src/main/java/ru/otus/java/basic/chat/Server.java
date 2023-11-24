package ru.otus.java.basic.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Server {
    private final int port;
    private List<ClientHandler> clients;
    private final AuthenticationProvider authenticationProvider;
    private final Object monitor = new Object();
    private boolean isShutdowning = false;
    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port, AuthenticationProvider authenticationProvider) {
        this.port = port;
        clients = new ArrayList<>();
        this.authenticationProvider = authenticationProvider;
    }

    public void start() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            while (!isShutdowning) {
                System.out.println("System > Сервер запущен. Порт " + port); // переделать на логирование
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {

                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastMessage("System > пользователь " + clientHandler.getUsername() + " вошёл в чат.");
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("System > пользователь " + clientHandler.getUsername() + " вышел из чата.");
        clients.remove(clientHandler);
        clientHandler.disconnect();
    }

    public synchronized List<String> getUserList() {
        return clients.stream()
                .map(ClientHandler::getUsername)
                .collect(Collectors.toList());
    }

    public void privateMessage(String msg, String recipientUsername) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(recipientUsername)) {
                client.sendMessage(msg);
            }
        }
    }

    public ClientHandler getClientByUsername(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }

    public void shutdown() {
        isShutdowning = true;
        new Thread(() -> {
            try {
                synchronized (monitor) {
                    Iterator<ClientHandler> iterator = clients.iterator();
                    while (iterator.hasNext()) {
                        ClientHandler clientHandler = iterator.next();
                        clientHandler.disconnect();
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
