package ru.otus.java.basic.chat;

public interface AuthenticationProvider {
    String getUsernameByLoginAndPassword(String login, String password);
    boolean register(String login, String password, String username, UserRole userRole);
    UserRole getUserRoleByUsername(String username);
    boolean setUserRoleByUsername(String username, UserRole userRole);
}
