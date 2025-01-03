package org.example.labwork3.database;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import lombok.extern.log4j.Log4j2;
import org.example.labwork3.models.Point;

import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Named
@Log4j2
@ApplicationScoped
public class PointService implements Repository<Point>, Serializable {

    private static final String USER = "SYSTEM";
    private static final String PASSWORD = "Oracle_123";
    private static final String DB_URL = "jdbc:oracle:thin:@//db:1521/FREE";

    static {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        createTable();
    }

    private static void createTable() {
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            String createQuery = """
                       CREATE TABLE points (
                       id NUMBER PRIMARY KEY,
                       x NUMBER NOT NULL,
                       y NUMBER NOT NULL,
                       r NUMBER NOT NULL,
                       is_hit NUMBER(1) NOT NULL CHECK(is_hit in (0, 1)),
                       time VARCHAR(255) NOT NULL,
                       execution_time NUMBER NOT NULL,
                       session_id VARCHAR(50))
                    """;
            statement.execute(createQuery);
        } catch (SQLException exception) {
            log.error(exception);
        }
    }

    @Override
    public List<Point> findBySessionId(String sessionId) {
        List<Point> points = new ArrayList<>();
        String selectByIdQuery = "SELECT * FROM points WHERE session_id = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(selectByIdQuery)) {
            statement.setString(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery(selectByIdQuery)) {
                while (resultSet.next()) {
                    Point point = new Point();
                    point.setId(resultSet.getInt("id"));
                    point.setX(resultSet.getDouble("x"));
                    point.setY(resultSet.getDouble("y"));
                    point.setR(resultSet.getDouble("r"));
                    point.setHit(resultSet.getInt("is_hit") == 1);
                    point.setTime(resultSet.getString("time"));
                    point.setExecutionTime(resultSet.getLong("execution_time"));
                    point.setSessionId(resultSet.getString("session_id"));
                    points.add(point);
                }
            }
        } catch (SQLException exception) {
            log.error(exception);
        }
        return points;
    }

    @Override
    public void insert(Point point) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            connection.setAutoCommit(false);
            String insertQuery = "INSERT INTO points (x, y, r, is_hit, time, execution_time, session_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                statement.setDouble(1, point.getX());
                statement.setDouble(2, point.getY());
                statement.setDouble(3, point.getR());
                statement.setInt(4, point.isHit() ? 1 : 0);
                statement.setString(5, point.getTime());
                statement.setLong(6, point.getExecutionTime());
                statement.setString(7, point.getSessionId());
                statement.execute(insertQuery);
            }
            connection.commit();
        } catch (SQLException exception) {
            log.error(exception);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }
}
