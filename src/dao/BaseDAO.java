package dao;

import database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Shared JDBC helper methods. All SQL uses PreparedStatement. */
public abstract class BaseDAO {

    protected void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                ps.setNull(i + 1, ps.getParameterMetaData().getParameterType(i + 1));
            } else {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    protected List<Object[]> queryRows(String sql, Object... params) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                int n = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int i = 0; i < n; i++) row[i] = rs.getObject(i + 1);
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    protected int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        }
    }

    protected int scalarInt(String sql, Object... params) throws SQLException {
        List<Object[]> rows = queryRows(sql, params);
        if (rows.isEmpty() || rows.get(0)[0] == null) return 0;
        return ((Number) rows.get(0)[0]).intValue();
    }

    protected boolean exists(String sql, Object... params) throws SQLException {
        return !queryRows(sql, params).isEmpty();
    }

    /** Builds a case-insensitive LIKE pattern from a search keyword. */
    protected String like(String keyword) {
        return "%" + (keyword == null ? "" : keyword.trim().toUpperCase()) + "%";
    }
}
