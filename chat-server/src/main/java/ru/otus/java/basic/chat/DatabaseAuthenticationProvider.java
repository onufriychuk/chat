package ru.otus.java.basic.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DatabaseAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LogManager.getLogger(Server.class.getName());
    private static final String dbName = "jdbc:postgresql://localhost:5432/postgres";
    private static final String dbUser = "postgres";
    private static final String dbPassword = "MJxaN9bz";
    private Connection connection;

    public DatabaseAuthenticationProvider() {
        try {
            Connection connection = DriverManager.getConnection(dbName, dbUser, dbPassword);
            this.connection = connection;
        } catch (SQLException e) {
            logger.error(e);
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
            logger.error(e);
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
            logger.error("Не удалось соедениться с БД");
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
            logger.error(e);
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
            logger.error(e);
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
            logger.error(e);
        }
        return users;
    }

    @Override
    public boolean updateUsername(String username, String newUsername) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE public.users " +
                "SET username = ? " +
                "WHERE username = ?;"
        )) {
            preparedStatement.setString(1, newUsername);
            preparedStatement.setString(2, username);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Не удалось соедениться с БД");
            return false;
        }
        return true;
    }
    public boolean updateBannedTime(String username, long bannedTo) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE public.users " +
                        "SET banned_to = ? " +
                        "WHERE username = ?;"
        )) {
            preparedStatement.setLong(1, bannedTo);
            preparedStatement.setString(2, username);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Не удалось соедениться с БД");
            return false;
        }
        return true;
    }

    public boolean isUsernameExist(String username) {
        List<String> users = new ArrayList<>();
        try (PreparedStatement preparedStatement = this.connection.prepareStatement("SELECT username FROM public.users WHERE username = ?")) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            logger.error(e);
        }
        return false;
    }

    @Override
    public boolean isUsernameBanned(String username) {
        try (PreparedStatement preparedStatement = this.connection.prepareStatement("SELECT banned_to FROM public.users WHERE username = ?")) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                if (resultSet.getLong("banned_to") < System.currentTimeMillis()) {
                    return false;
                }
            }
        } catch (SQLException e) {
            logger.error(e);
        }
        return true;
    }

}
