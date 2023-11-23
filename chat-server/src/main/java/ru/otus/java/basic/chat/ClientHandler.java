package ru.otus.java.basic.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientHandler {
    private Socket socket;

    private Server server;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }

    private static int userCount = 0;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
//        username = "user" + userCount++;
        new Thread(() -> {
            try {
                authenticateUser(server);
                communicateWithServer(server);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }).start();
    }

    private void authenticateUser(Server server) throws IOException {
        boolean isAuthenticate = false;
        while (!isAuthenticate) {
            String message = in.readUTF();
            String[] args = message.split(" ");
            String command = args[0];
            switch (command) {
                case "/auth": {
                    String login = args[1];
                    String password = args[2];
                    String username = server.getAuthenticationProvider().getUsernameByLoginAndPassword(login, password);
                    if (username == null || username.isBlank()) {
                        sendMessage("Системное сообщение: Указан неверный логин/пароль.");
                    } else {
                        this.username = username;
                        sendMessage("Системное сообщение: " + username + " , добро пожаловать!");
                        server.subscribe(this);
                        isAuthenticate = true;
                    }
                    break;
                }
                case "/register": {
                    String login = args[1];
                    String nickname = args[2];
                    String password = args[3];
                    Boolean isRegistered = server.getAuthenticationProvider().register(login, password, nickname, UserRole.USER);
                    if (!isRegistered) {
                        sendMessage("Системное сообщение: Указанный логин/ник уже заняты.");
                    } else {
                        this.username = nickname;
                        sendMessage("Системное сообщение: " + nickname + " , добро пожаловать!");
                        server.subscribe(this);
                        isAuthenticate = true;
                    }
                    break;
                }
                default: {
                    sendMessage("Системное сообщение: Нужно авторизоваться.");
                }
            }
        }
    }

    private void communicateWithServer(Server server) throws IOException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith("/")) {
                if (message.equals("/exit")) {
                    break;
                } else if (message.equals("/list")) {
                    List<String> userList = server.getUserList();
                    String joinedUsers = String.join(", ", userList);
                    sendMessage("Системное сообщение! Пользователи в чате: " + joinedUsers);
                } else if (message.startsWith("/w ")) {
                    // /w nickname My example message
                    String[] args = message.split(" ");
                    String userTo = args[1];
                    String msg = message.substring(message.indexOf(userTo) + userTo.length() + 1);
                    server.privateMessage("Личное сообщение от " + username + ": " + msg, userTo);
                } else if (message.startsWith("/setrole") && server.getAuthenticationProvider().getUserRoleByUsername(username) == UserRole.ADMIN) {
                    // /setrole User1 ADMIN
                    String[] args = message.split(" ");
                    String userTo = args[1];
                    String newRole = args[2];
                    boolean isRoleChanged = server.getAuthenticationProvider().setUserRoleByUsername(userTo, UserRole.valueOf(newRole));
                    if (!isRoleChanged) {
                        sendMessage("Системное сообщение! Не удалось изменить роль.");
                    } else {
                        sendMessage("Системное сообщение! Роль пользователя " + userTo + "успешно изменена на " + newRole);
                    }
                } else if (message.startsWith("/kick") && server.getAuthenticationProvider().getUserRoleByUsername(username) == UserRole.ADMIN) {
                    String[] args = message.split(" ");
                    String userTo = args[1];
                    ClientHandler userHandler = server.getClientByUsername(userTo);
                    if (userHandler != null) {
                        server.unsubscribe(userHandler);
                    } else {
                        sendMessage("Системное сообщение! Не найден пользователь " + userTo);
                    }
                }
            } else {
                server.broadcastMessage(username + ": " + message);
            }
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }
}
