package ru.otus.java.basic.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class.getName());
    public static void main(String[] args) {
        int port = 8089;
//        Server server = new Server(port, new InMemoryAuthenticationProvider());
        Server server = new Server(port, new DatabaseAuthenticationProvider());
        server.start();
    }
}