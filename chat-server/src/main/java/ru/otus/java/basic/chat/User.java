package ru.otus.java.basic.chat;

public class User {
    private final String login;
    private final String password;
    private final String username;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public User(String login, String password, String username) {
        this.login = login;
        this.password = password;
        this.username = username;
    }
}
