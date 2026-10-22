package dao;

import model.Grade;
import util.GradeUtil;

import java.sql.SQLException;
import java.util.List;

public class GradeDAO extends BaseDAO {

    public static final String[] COLUMNS = {"Enr. ID", "Student No", "Student", "Subject", "School Year", "Sem",
            "Prelim", "Midterm", "Finals", "Final Grade", "Remarks"};

    /** Lists every enrollment with its grade (LEFT JOIN so ungraded enrollments also appear). */
    public List<Object[]> search(String keyword) throws SQLException {
        String k = like(keyword);
        List<Object[]> rows = queryRows("SELECT e.enrollment_id, st.student_no, st.last_name || ', ' || st.first_name, "
                + "sb.subject_code, e.school_year, e.semester, g.prelim, g.midterm, g.finals, g.final_grade, g.remarks "
                + "FROM ENROLLMENTS e "
                + "INNER JOIN STUDENTS st ON e.student_id = st.student_id "
                + "INNER JOIN SUBJECTS sb ON e.subject_id = sb.subject_id "
                + "LEFT JOIN GRADES g ON g.enrollment_id = e.enrollment_id "
                + "WHERE UPPER(st.student_no) LIKE ? OR UPPER(st.last_name) LIKE ? OR UPPER(st.first_name) LIKE ? "
                + "OR UPPER(sb.subject_code) LIKE ? OR UPPER(e.school_year) LIKE ? "
                + "ORDER BY st.last_name, st.first_name, sb.subject_code", k, k, k, k, k);
        for (Object[] r : rows) {
            if (r[10] == null) r[10] = "NOT GRADED";
        }
        return rows;
    }

    public Grade getByEnrollment(int enrollmentId) throws SQLException {
        List<Object[]> rows = queryRows("SELECT grade_id, enrollment_id, prelim, midterm, finals, final_grade, remarks "
                + "FROM GRADES WHERE enrollment_id = ?", enrollmentId);
        if (rows.isEmpty()) return null;
        Object[] r = rows.get(0);
        Grade g = new Grade();
        g.setGradeId(((Number) r[0]).intValue());
        g.setEnrollmentId(((Number) r[1]).intValue());
        g.setPrelim(GradeUtil.toDouble(r[2]));
        g.setMidterm(GradeUtil.toDouble(r[3]));
        g.setFinals(GradeUtil.toDouble(r[4]));
        g.setFinalGrade(GradeUtil.toDouble(r[5]));
        g.setRemarks((String) r[6]);
        return g;
    }

    /** Inserts the grade or updates it if the enrollment already has one. */
    public void save(Grade g) throws SQLException {
        Double fin = GradeUtil.computeFinal(g.getPrelim(), g.getMidterm(), g.getFinals());
        g.setFinalGrade(fin);
        g.setRemarks(GradeUtil.remarks(fin));
        if (exists("SELECT 1 FROM GRADES WHERE enrollment_id = ?", g.getEnrollmentId())) {
            executeUpdate("UPDATE GRADES SET prelim = ?, midterm = ?, finals = ?, final_grade = ?, remarks = ? "
                    + "WHERE enrollment_id = ?",
                    g.getPrelim(), g.getMidterm(), g.getFinals(), g.getFinalGrade(), g.getRemarks(), g.getEnrollmentId());
        } else {
            executeUpdate("INSERT INTO GRADES (enrollment_id, prelim, midterm, finals, final_grade, remarks) "
                    + "VALUES (?, ?, ?, ?, ?, ?)",
                    g.getEnrollmentId(), g.getPrelim(), g.getMidterm(), g.getFinals(), g.getFinalGrade(), g.getRemarks());
        }
    }

    public void delete(int enrollmentId) throws SQLException {
        executeUpdate("DELETE FROM GRADES WHERE enrollment_id = ?", enrollmentId);
    }
}
