package ru.otus.java.basic.chat;

public class Main {
    public static void main(String[] args) {
        int port = 8088;
        Server server = new Server(port, new InMemoryAuthenticationProvider());
        server.start();
    }
}