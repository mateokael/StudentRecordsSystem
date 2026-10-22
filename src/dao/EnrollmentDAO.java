package dao;

import database.DatabaseConnection;
import model.Enrollment;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDAO extends BaseDAO {

    public static final String[] STUDENT_COLUMNS = {"Code", "Subject", "Units", "School Year", "Sem", "Date Enrolled", "Status"};

    public static final String[] COLUMNS = {"ID", "Student No", "Student Name", "Subject Code", "Subject Name",
            "Units", "School Year", "Sem", "Date Enrolled", "Status"};

    public void enroll(Enrollment e) throws SQLException {
        executeUpdate("INSERT INTO ENROLLMENTS (student_id, subject_id, school_year, semester, date_enrolled, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                e.getStudentId(), e.getSubjectId(), e.getSchoolYear(), e.getSemester(),
                Date.valueOf(LocalDate.now()), e.getStatus());
    }

    /**
     * Enrolls a student in every subject of his/her course, year level and the chosen semester
     * (skipping subjects already enrolled). Returns how many new enrollments were created.
     */
    public int enrollTerm(int studentId, int courseId, int yearLevel, int semester, String schoolYear) throws SQLException {
        String subs = "SELECT subject_id FROM SUBJECTS WHERE course_id = ? AND year_level = ? AND semester = ?";
        String check = "SELECT COUNT(*) FROM ENROLLMENTS WHERE student_id = ? AND subject_id = ? AND school_year = ? AND semester = ?";
        String ins = "INSERT INTO ENROLLMENTS (student_id, subject_id, school_year, semester, date_enrolled, status) "
                   + "VALUES (?, ?, ?, ?, ?, 'ENROLLED')";
        int added = 0;
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Integer> ids = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(subs)) {
                    ps.setInt(1, courseId);
                    ps.setInt(2, yearLevel);
                    ps.setInt(3, semester);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) ids.add(rs.getInt(1));
                    }
                }
                for (int subjectId : ids) {
                    boolean already;
                    try (PreparedStatement ps = c.prepareStatement(check)) {
                        ps.setInt(1, studentId);
                        ps.setInt(2, subjectId);
                        ps.setString(3, schoolYear);
                        ps.setInt(4, semester);
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            already = rs.getInt(1) > 0;
                        }
                    }
                    if (already) continue;
                    try (PreparedStatement ps = c.prepareStatement(ins)) {
                        ps.setInt(1, studentId);
                        ps.setInt(2, subjectId);
                        ps.setString(3, schoolYear);
                        ps.setInt(4, semester);
                        ps.setDate(5, Date.valueOf(LocalDate.now()));
                        ps.executeUpdate();
                        added++;
                    }
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
        return added;
    }

    public void updateStatus(int enrollmentId, String status) throws SQLException {
        executeUpdate("UPDATE ENROLLMENTS SET status = ? WHERE enrollment_id = ?", status, enrollmentId);
    }

    /** Grades and attendance of this enrollment are removed automatically (ON DELETE CASCADE). */
    public void delete(int enrollmentId) throws SQLException {
        executeUpdate("DELETE FROM ENROLLMENTS WHERE enrollment_id = ?", enrollmentId);
    }

    public Enrollment getById(int id) throws SQLException {
        List<Object[]> rows = queryRows("SELECT enrollment_id, student_id, subject_id, school_year, semester, "
                + "date_enrolled, status FROM ENROLLMENTS WHERE enrollment_id = ?", id);
        if (rows.isEmpty()) return null;
        Object[] r = rows.get(0);
        Enrollment e = new Enrollment();
        e.setEnrollmentId(((Number) r[0]).intValue());
        e.setStudentId(((Number) r[1]).intValue());
        e.setSubjectId(((Number) r[2]).intValue());
        e.setSchoolYear((String) r[3]);
        e.setSemester(((Number) r[4]).intValue());
        e.setDateEnrolled((Date) r[5]);
        e.setStatus((String) r[6]);
        return e;
    }

    public List<Object[]> search(String keyword) throws SQLException {
        String k = like(keyword);
        return queryRows("SELECT e.enrollment_id, st.student_no, st.last_name || ', ' || st.first_name AS student_name, "
                + "sb.subject_code, sb.subject_name, sb.units, e.school_year, e.semester, e.date_enrolled, e.status "
                + "FROM ENROLLMENTS e "
                + "INNER JOIN STUDENTS st ON e.student_id = st.student_id "
                + "INNER JOIN SUBJECTS sb ON e.subject_id = sb.subject_id "
                + "WHERE UPPER(st.student_no) LIKE ? OR UPPER(st.last_name) LIKE ? OR UPPER(st.first_name) LIKE ? "
                + "OR UPPER(sb.subject_code) LIKE ? OR UPPER(sb.subject_name) LIKE ? OR UPPER(e.school_year) LIKE ? "
                + "ORDER BY e.enrollment_id DESC", k, k, k, k, k, k);
    }

    /** Subjects of one student (used by the Student Portal). */
    public List<Object[]> listByStudent(int studentId) throws SQLException {
        return queryRows("SELECT sb.subject_code, sb.subject_name, sb.units, e.school_year, e.semester, "
                + "e.date_enrolled, e.status FROM ENROLLMENTS e INNER JOIN SUBJECTS sb ON e.subject_id = sb.subject_id "
                + "WHERE e.student_id = ? ORDER BY e.school_year DESC, e.semester DESC, sb.subject_code", studentId);
    }
}
