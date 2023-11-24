package ru.otus.java.basic.chat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseAuthenticationProvider implements AuthenticationProvider {
    private static final String dbName = "jdbc:postgresql://localhost:5432/postgres";
    private static final String dbUser = "postgres";
    private static final String dbPassword = "MJxaN9bz";
    private Connection connection;

    public DatabaseAuthenticationProvider() {
        try {
            Connection connection = DriverManager.getConnection(dbName, dbUser, dbPassword);
            this.connection = connection;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        String username = null;
        try (PreparedStatement preparedStatement = this.connection.prepareStatement("SELECT username FROM public.users WHERE login = ? AND password = ?")) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                username = resultSet.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return username;
    }

    @Override
    public boolean register(String login, String password, String username, UserRole userRole) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("" +
                    "INSERT " +
                    "INTO public.users (login, username, password, role) " +
                    "VALUES (?,?,?,?);")) {
                preparedStatement.setString(1, login);
                preparedStatement.setString(2, username);
                preparedStatement.setString(3, password);
                preparedStatement.setString(4, userRole.toString());
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Не удалось выполнить запрос добавления пользователя");
            }
            connection.commit();
        } catch (SQLException e) {
            System.out.println("Не удалось соедениться с БД");
        }
        return true;
    }

    @Override
    public UserRole getUserRole(String username) {
        String role = null;
        try (PreparedStatement preparedStatement = this.connection.prepareStatement("SELECT role FROM public.users WHERE username = ?")) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                role = resultSet.getString("role");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return UserRole.valueOf(role);
    }

    @Override
    public boolean updateUserRole(String username, UserRole userRole) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("" +
                "UPDATE public.users " +
                "SET role = ? " +
                "WHERE username = ?;"
        )) {
            preparedStatement.setString(1, userRole.toString());
            preparedStatement.setString(2, username);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public List<String> getUserList() {
        List<String> users = new ArrayList<>();
        try (PreparedStatement preparedStatement = this.connection.prepareStatement("SELECT username FROM public.users")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                users.add(resultSet.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    @Override
    public boolean updateUsername(String username, String newUsername) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("" +
                "UPDATE public.users " +
                "SET username = ? " +
                "WHERE username = ?;"
        )) {
            preparedStatement.setString(1, newUsername);
            preparedStatement.setString(2, username);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
