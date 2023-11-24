package ru.otus.java.basic.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class ClientHandler {
    private static final Logger logger = LogManager.getLogger(ClientHandler.class.getName());
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                authenticateUser(server);
                communicateWithServer(server);
            } catch (IOException e) {
                logger.error(e);
                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }).start();
    }

    private void authenticateUser(Server server) throws IOException {
        boolean isAuthenticate = false;
        while (!isAuthenticate) {
            sendMessage("Залогиниться - /auth login password \nЗарегистрироваться - /register login nick password");
            String message = in.readUTF();
            String[] args = message.split(" ");
            String command = args[0];
            switch (command) {
                case "/auth": {
                    if (args.length != 3) {
                        sendMessage("System > Неверный формат. Должно быть: \"/auth login password\"");
                        continue;
                    }
                    String login = args[1];
                    String password = args[2];
                    String username = server.getAuthenticationProvider().getUsernameByLoginAndPassword(login, password);
                    if (server.getAuthenticationProvider().isUsernameBanned(username)) {
                        sendMessage("Вы забанены");
                        continue;
                    }
                    if (username == null || username.isBlank()) {
                        sendMessage("System > Указан неверный логин/пароль.");
                        continue;
                    }
                    this.username = username;
                    sendMessage("System > " + username + " , добро пожаловать!");
                    server.subscribe(this);
                    isAuthenticate = true;

                    break;
                }
                case "/register": {
                    if (args.length != 4) {
                        sendMessage("System > Неверный формат. Должно быть: \"/register login nickname password\"");
                        continue;
                    }
                    String login = args[1];
                    String nickname = args[2];
                    String password = args[3];
                    boolean isRegistered = server.getAuthenticationProvider().register(login, password, nickname, UserRole.USER);
                    if (!isRegistered) {
                        sendMessage("System > Указанный логин/ник уже заняты.");
                    } else {
                        this.username = nickname;
                        sendMessage("System > " + nickname + " , добро пожаловать!");
                        server.subscribe(this);
                        isAuthenticate = true;
                    }
                    break;
                }
                default: {
                    sendMessage("System > Нужно авторизоваться.");
                }
            }
        }
    }

    private void communicateWithServer(Server server) throws IOException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith("/")) {
                String[] args = message.split(" ");
                String command = args[0];
                switch (command) {
                    case "/ban": {
                        if (server.getAuthenticationProvider().getUserRole(username) == UserRole.ADMIN) {
                            if (args.length < 3) {
                                sendMessage("System > Неверный формат. Должно быть: \"/ban User1 HOURS\"");
                                continue;
                            }
                            String userTo = args[1];
                            long banHrs = Long.parseLong(args[2]);
                            boolean isBanChanged = server.getAuthenticationProvider().updateBannedTime(userTo, System.currentTimeMillis() + banHrs * 3600000);
                            if (!isBanChanged) {
                                sendMessage("System > Не удалось забанить пользователя.");
                            } else {
                                sendMessage("System > Админ " + userTo + " забанил участника " + userTo);
                                server.privateMessage("System > Администратор " + username + " забанил вас на " + banHrs + " часов.", userTo);
                            }
                        }
                        continue;
                    }
                    case "/list": {
                        if (server.getAuthenticationProvider().getUserRole(username) == UserRole.ADMIN) {
                            String allUsers = String.join(", ", server.getAuthenticationProvider().getUserList());
                            sendMessage("System > Зарегистированные пользователи: " + allUsers);
                        }
                        continue;
                    }
                    case "/setrole": {
                        if (server.getAuthenticationProvider().getUserRole(username) == UserRole.ADMIN) {
                            // /setrole User1 ADMIN
                            if (args.length < 3) {
                                sendMessage("System > Неверный формат. Должно быть: \"/setrole User1 ADMIN\"");
                                continue;
                            }
                            String userTo = args[1];
                            String newRole = args[2];
                            boolean isRoleChanged = server.getAuthenticationProvider().updateUserRole(userTo, UserRole.valueOf(newRole));
                            if (!isRoleChanged) {
                                sendMessage("System > Не удалось изменить роль.");
                            } else {
                                sendMessage("System > Роль пользователя " + userTo + " успешно изменена на " + newRole);
                                server.privateMessage("System > Администратор " + username + " изменил ваши права на " + newRole, userTo);
                            }
                        }
                        continue;
                    }
                    case "/kick": {
                        if (server.getAuthenticationProvider().getUserRole(username) == UserRole.ADMIN) {
                            if (args.length < 2) {
                                sendMessage("System > Неверный формат. Должно быть: \"/kick user\"");
                                continue;
                            }
                            String userTo = args[1];
                            ClientHandler userHandler = server.getClientByUsername(userTo);
                            if (userHandler != null) {
                                System.out.println(userHandler.username);
                                server.unsubscribe(userHandler);
                            } else {
                                sendMessage("System > Не найден пользователь " + userTo);
                            }
                        }
                        continue;
                    }
                    case "/activelist": {
                        List<String> userList = server.getUserList();
                        String joinedUsers = String.join(", ", userList);
                        sendMessage("System > Пользователи онлайн: " + joinedUsers);
                        continue;
                    }
                    case "/w": {
                        // /w nickname My example message
                        if (args.length < 2) {
                            sendMessage("System > Неверный формат. Должно быть: \"/w nickname My example message\"");
                            continue;
                        }
                        String userTo = args[1];
                        String msg = message.substring(message.indexOf(userTo) + userTo.length() + 1);
                        server.privateMessage("Личное сообщение от " + username + ": " + msg, userTo);
                        continue;
                    }
                    case "/exit": {
                        server.unsubscribe(this);
                        disconnect();
                        break;
                    }
                    case "/shutdown": {
                        if (server.getAuthenticationProvider().getUserRole(username) == UserRole.ADMIN) {
                            server.broadcastMessage("Сервер отключается.");
                            server.shutdown();
                            continue;
                        }
                    }
                    case "/changenick": {
                        if (args.length < 2) {
                            sendMessage("System > Неверный формат. Должно быть: \"/changenick newNickName\"");
                            continue;
                        }
                        String newUsername = args[1];
                        if (server.getAuthenticationProvider().isUsernameExist(newUsername)) {
                            sendMessage("Это имя занято.");
                            continue;
                        }
                        boolean isUsernameChanged = server.getAuthenticationProvider().updateUsername(username, newUsername);
                        if (!isUsernameChanged) {
                            sendMessage("System > Не удалось изменить ник.");
                        } else {
                            server.broadcastMessage("System > Пользователь " + username + " изменил свой ник на " + newUsername);
                            this.username = newUsername;
                            server.unsubscribe(this);
                            server.subscribe(this);
                        }
                        continue;
                    }
                    default: {
                        server.broadcastMessage(username + ": " + message);
                    }
                }
            } else {
                server.broadcastMessage(username + ": " + message);
            }
        }
    }

    public void disconnect() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                logger.error(e);
                throw new RuntimeException(e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                logger.error(e);
                throw new RuntimeException(e);
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessage(String message) {
        try {
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            out.writeUTF("[" + date + "] " + message);
        } catch (IOException e) {
            logger.error(e);
            disconnect();
        }
    }
}
