package dao;

import model.Attendance;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class AttendanceDAO extends BaseDAO {

    public static final String[] COLUMNS = {"ID", "Date", "Status", "Remarks"};

    public void insert(Attendance a) throws SQLException {
        executeUpdate("INSERT INTO ATTENDANCE (enrollment_id, attendance_date, status, remarks) VALUES (?, ?, ?, ?)",
                a.getEnrollmentId(), a.getAttendanceDate(), a.getStatus(), a.getRemarks());
    }

    public void update(Attendance a) throws SQLException {
        executeUpdate("UPDATE ATTENDANCE SET attendance_date = ?, status = ?, remarks = ? WHERE attendance_id = ?",
                a.getAttendanceDate(), a.getStatus(), a.getRemarks(), a.getAttendanceId());
    }

    public void delete(int id) throws SQLException {
        executeUpdate("DELETE FROM ATTENDANCE WHERE attendance_id = ?", id);
    }

    public Attendance getById(int id) throws SQLException {
        List<Object[]> rows = queryRows("SELECT attendance_id, enrollment_id, attendance_date, status, remarks "
                + "FROM ATTENDANCE WHERE attendance_id = ?", id);
        if (rows.isEmpty()) return null;
        Object[] r = rows.get(0);
        Attendance a = new Attendance();
        a.setAttendanceId(((Number) r[0]).intValue());
        a.setEnrollmentId(((Number) r[1]).intValue());
        a.setAttendanceDate((Date) r[2]);
        a.setStatus((String) r[3]);
        a.setRemarks((String) r[4]);
        return a;
    }

    public List<Object[]> listByEnrollment(int enrollmentId) throws SQLException {
        return queryRows("SELECT attendance_id, attendance_date, status, remarks FROM ATTENDANCE "
                + "WHERE enrollment_id = ? ORDER BY attendance_date DESC", enrollmentId);
    }

    /** Returns {present, absent, late, excused} counts for one enrollment. */
    public int[] summary(int enrollmentId) throws SQLException {
        int[] counts = new int[4];
        for (Object[] r : queryRows("SELECT status, COUNT(*) FROM ATTENDANCE WHERE enrollment_id = ? GROUP BY status", enrollmentId)) {
            int n = ((Number) r[1]).intValue();
            String s = (String) r[0];
            if ("PRESENT".equals(s)) counts[0] = n;
            else if ("ABSENT".equals(s)) counts[1] = n;
            else if ("LATE".equals(s)) counts[2] = n;
            else if ("EXCUSED".equals(s)) counts[3] = n;
        }
        return counts;
    }
}
