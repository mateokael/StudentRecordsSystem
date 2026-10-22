package dao;

import database.DatabaseConnection;
import model.User;
import util.PasswordUtil;

import java.sql.*;
import java.util.List;

public class UserDAO extends BaseDAO {

    public static final String[] COLUMNS = {"ID", "Username", "Full Name", "Role", "Status", "Student No"};

    private static final String SELECT =
            "SELECT user_id, username, password, full_name, role, status, student_id FROM USERS ";

    private User map(ResultSet rs) throws SQLException {
        User u = new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("password"),
                rs.getString("full_name"), rs.getString("role"), rs.getString("status"));
        Object sid = rs.getObject("student_id");
        u.setStudentId(sid == null ? null : Integer.valueOf(((Number) sid).intValue()));
        return u;
    }

    /** Returns the user when username/password match, otherwise null. */
    public User authenticate(String username, String password) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE username = ? AND password = ?")) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void insert(User u, String plainPassword) throws SQLException {
        executeUpdate("INSERT INTO USERS (username, password, full_name, role, status, student_id) VALUES (?, ?, ?, ?, ?, ?)",
                u.getUsername(), PasswordUtil.hash(plainPassword), u.getFullName(), u.getRole(), u.getStatus(),
                u.getStudentId());
    }

    /** If newPassword is blank the old password is kept. */
    public void update(User u, String newPassword) throws SQLException {
        if (newPassword == null || newPassword.isEmpty()) {
            executeUpdate("UPDATE USERS SET username = ?, full_name = ?, role = ?, status = ?, student_id = ? WHERE user_id = ?",
                    u.getUsername(), u.getFullName(), u.getRole(), u.getStatus(), u.getStudentId(), u.getUserId());
        } else {
            executeUpdate("UPDATE USERS SET username = ?, password = ?, full_name = ?, role = ?, status = ?, student_id = ? "
                    + "WHERE user_id = ?",
                    u.getUsername(), PasswordUtil.hash(newPassword), u.getFullName(), u.getRole(), u.getStatus(),
                    u.getStudentId(), u.getUserId());
        }
    }

    public void delete(int userId) throws SQLException {
        executeUpdate("DELETE FROM USERS WHERE user_id = ?", userId);
    }

    public User getById(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE user_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** LEFT JOIN with STUDENTS so STUDENT accounts show their student number. */
    public List<Object[]> search(String keyword) throws SQLException {
        String k = like(keyword);
        return queryRows("SELECT u.user_id, u.username, u.full_name, u.role, u.status, s.student_no "
                + "FROM USERS u LEFT JOIN STUDENTS s ON u.student_id = s.student_id "
                + "WHERE UPPER(u.username) LIKE ? OR UPPER(u.full_name) LIKE ? OR UPPER(u.role) LIKE ? "
                + "OR UPPER(s.student_no) LIKE ? ORDER BY u.user_id", k, k, k, k);
    }
}
