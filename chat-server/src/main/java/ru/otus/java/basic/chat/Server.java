package ru.otus.java.basic.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class.getName());
    private final int port;
    private List<ClientHandler> clients;
    private Map<String, ClientHandler> clientsMap;
    private final AuthenticationProvider authenticationProvider;
    private final Object monitor = new Object();
    private boolean isShutdowning = false;

    public Server(int port, AuthenticationProvider authenticationProvider) {
        this.port = port;
//        clients = new ArrayList<>();
        clientsMap = new HashMap<>();
        this.authenticationProvider = authenticationProvider;
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void start() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            while (!isShutdowning) {
                logger.info("Сервер запущен. Порт " + port);
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
                logger.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    public void shutdown() {
        isShutdowning = true;
        new Thread(() -> {
            try {
                Iterator<Map.Entry<String, ClientHandler>> iterator = clientsMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String,ClientHandler> pair = iterator.next();
                    ClientHandler clientHandler = pair.getValue();
                    clientHandler.disconnect();
                    iterator.remove();
                }
            } catch (Exception e) {
                logger.error(e);
                throw new RuntimeException(e);
            }
        }).start();
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
//        clients.add(clientHandler);
        clientsMap.put(clientHandler.getUsername(), clientHandler);
        broadcastMessage("System > пользователь " + clientHandler.getUsername() + " вошёл в чат.");
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("System > пользователь " + clientHandler.getUsername() + " вышел из чата.");
//        clients.remove(clientHandler);
        clientsMap.remove(clientHandler.getUsername());
        clientHandler.disconnect();
    }

    public ClientHandler getClientByUsername(String username) {
//        for (ClientHandler client : clients) {
//            if (client.getUsername().equals(username)) {
//                return client;
//            }
//        }
        return clientsMap.get(username);
        //return null;
    }

    public synchronized List<String> getUserList() {
//        return clients.stream()
//                .map(ClientHandler::getUsername)
//                .collect(Collectors.toList());
        return new ArrayList<>(clientsMap.keySet());
    }

    public synchronized void broadcastMessage(String message) {
//        for (ClientHandler client : clients) {
//            client.sendMessage(message);
//        }
        for (var entry: clientsMap.entrySet()) {
            entry.getValue().sendMessage(message);
        }
    }

    public void privateMessage(String msg, String recipientUsername) {
//        for (ClientHandler client : clients) {
//            if (client.getUsername().equals(recipientUsername)) {
//                client.sendMessage(msg);
//            }
//        }
        clientsMap.get(recipientUsername).sendMessage(msg);
    }
}
