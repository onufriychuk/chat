package ru.otus.java.basic.chat;

import java.time.chrono.ChronoLocalDateTime;
import java.util.List;

public interface AuthenticationProvider {
    String getUsernameByLoginAndPassword(String login, String password);
    boolean register(String login, String password, String username, UserRole userRole);
    UserRole getUserRole(String username);
    boolean updateUserRole(String username, UserRole userRole);
    public List<String> getUserList();

    boolean updateUsername(String username, String newUsername);
    public boolean updateBannedTime(String username, long bannedTo);
    public boolean isUsernameExist(String username);
    public boolean isUsernameBanned(String username);
}
