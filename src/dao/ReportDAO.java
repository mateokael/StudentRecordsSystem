package dao;

import util.GradeUtil;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Read-only queries used by the dashboard and the Reports screen. */
public class ReportDAO extends BaseDAO {

    public static final String[] REPORT_CARD_COLUMNS = {"Code", "Subject", "Units", "Prelim", "Midterm", "Finals",
            "Final Grade", "Equivalent", "Remarks"};
    public static final String[] TRANSCRIPT_COLUMNS = {"School Year", "Sem", "Code", "Subject", "Units",
            "Final Grade", "Equivalent", "Remarks"};
    public static final String[] CLASS_LIST_COLUMNS = {"Student No", "Student Name", "Course", "Year", "Status",
            "Final Grade", "Remarks"};
    public static final String[] GRADE_SUMMARY_COLUMNS = {"Code", "Subject", "Enrolled", "Graded", "Passed", "Failed", "Average"};
    public static final String[] ATTENDANCE_SUMMARY_COLUMNS = {"Student No", "Last Name", "First Name", "Subject",
            "School Year", "Sem", "Present", "Absent", "Late", "Excused"};
    public static final String[] STUDENT_ATTENDANCE_COLUMNS = {"Code", "Subject", "School Year", "Sem", "Present", "Absent", "Late", "Excused"};
    public static final String[] COURSE_SUMMARY_COLUMNS = {"Course Code", "Course Name", "Total Students"};
    public static final String[] DAILY_COLUMNS = {"Date", "Total Enrollments"};

    // ---------------------------------------------------------------- dashboard counts

    public int count(String table) throws SQLException {
        String t = table.toUpperCase();
        if (!t.equals("STUDENTS") && !t.equals("COURSES") && !t.equals("SUBJECTS")
                && !t.equals("ENROLLMENTS") && !t.equals("USERS")) {
            throw new IllegalArgumentException("Unknown table: " + table);
        }
        return scalarInt("SELECT COUNT(*) FROM " + t);
    }

    public int countEnrollmentsToday() throws SQLException {
        return scalarInt("SELECT COUNT(*) FROM ENROLLMENTS WHERE date_enrolled = ?", Date.valueOf(LocalDate.now()));
    }

    // ---------------------------------------------------------------- student reports

    public List<Object[]> reportCard(int studentId, String schoolYear, int semester) throws SQLException {
        List<Object[]> raw = queryRows("SELECT sb.subject_code, sb.subject_name, sb.units, g.prelim, g.midterm, g.finals, "
                + "g.final_grade, g.remarks "
                + "FROM ENROLLMENTS e INNER JOIN SUBJECTS sb ON e.subject_id = sb.subject_id "
                + "LEFT JOIN GRADES g ON g.enrollment_id = e.enrollment_id "
                + "WHERE e.student_id = ? AND e.school_year = ? AND e.semester = ? ORDER BY sb.subject_code",
                studentId, schoolYear, semester);
        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : raw) {
            Double fin = GradeUtil.toDouble(r[6]);
            rows.add(new Object[]{r[0], r[1], r[2], r[3], r[4], r[5], r[6], GradeUtil.equivalent(fin),
                    r[7] == null ? "NOT GRADED" : r[7]});
        }
        return rows;
    }

    public List<Object[]> transcript(int studentId) throws SQLException {
        List<Object[]> raw = queryRows("SELECT e.school_year, e.semester, sb.subject_code, sb.subject_name, sb.units, "
                + "g.final_grade, g.remarks "
                + "FROM ENROLLMENTS e INNER JOIN SUBJECTS sb ON e.subject_id = sb.subject_id "
                + "LEFT JOIN GRADES g ON g.enrollment_id = e.enrollment_id "
                + "WHERE e.student_id = ? ORDER BY e.school_year, e.semester, sb.subject_code", studentId);
        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : raw) {
            Double fin = GradeUtil.toDouble(r[5]);
            rows.add(new Object[]{r[0], r[1], r[2], r[3], r[4], r[5], GradeUtil.equivalent(fin),
                    r[6] == null ? "NOT GRADED" : r[6]});
        }
        return rows;
    }

    public List<Object[]> classList(int subjectId, String schoolYear, int semester) throws SQLException {
        List<Object[]> rows = queryRows("SELECT st.student_no, st.last_name || ', ' || st.first_name, c.course_code, "
                + "st.year_level, e.status, g.final_grade, g.remarks "
                + "FROM ENROLLMENTS e INNER JOIN STUDENTS st ON e.student_id = st.student_id "
                + "INNER JOIN COURSES c ON st.course_id = c.course_id "
                + "LEFT JOIN GRADES g ON g.enrollment_id = e.enrollment_id "
                + "WHERE e.subject_id = ? AND e.school_year = ? AND e.semester = ? "
                + "ORDER BY st.last_name, st.first_name", subjectId, schoolYear, semester);
        for (Object[] r : rows) {
            if (r[6] == null) r[6] = "NOT GRADED";
        }
        return rows;
    }

    // ---------------------------------------------------------------- summary reports

    public List<Object[]> gradeSummary() throws SQLException {
        return queryRows("SELECT sb.subject_code, sb.subject_name, COUNT(e.enrollment_id) AS enrolled, "
                + "COUNT(g.grade_id) AS graded, "
                + "SUM(CASE WHEN g.remarks = 'PASSED' THEN 1 ELSE 0 END) AS passed, "
                + "SUM(CASE WHEN g.remarks = 'FAILED' THEN 1 ELSE 0 END) AS failed, "
                + "AVG(g.final_grade) AS average_grade "
                + "FROM SUBJECTS sb LEFT JOIN ENROLLMENTS e ON e.subject_id = sb.subject_id "
                + "LEFT JOIN GRADES g ON g.enrollment_id = e.enrollment_id "
                + "GROUP BY sb.subject_code, sb.subject_name ORDER BY sb.subject_code");
    }

    public List<Object[]> attendanceSummary() throws SQLException {
        return queryRows("SELECT st.student_no, st.last_name, st.first_name, sb.subject_code, e.school_year, e.semester, "
                + "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_days, "
                + "SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_days, "
                + "SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) AS late_days, "
                + "SUM(CASE WHEN a.status = 'EXCUSED' THEN 1 ELSE 0 END) AS excused_days "
                + "FROM ENROLLMENTS e INNER JOIN STUDENTS st ON e.student_id = st.student_id "
                + "INNER JOIN SUBJECTS sb ON e.subject_id = sb.subject_id "
                + "LEFT JOIN ATTENDANCE a ON a.enrollment_id = e.enrollment_id "
                + "GROUP BY e.enrollment_id, st.student_no, st.last_name, st.first_name, sb.subject_code, "
                + "e.school_year, e.semester ORDER BY st.last_name, st.first_name, sb.subject_code");
    }

    public List<Object[]> studentsPerCourse() throws SQLException {
        return queryRows("SELECT c.course_code, c.course_name, COUNT(s.student_id) AS total_students "
                + "FROM COURSES c LEFT JOIN STUDENTS s ON s.course_id = c.course_id "
                + "GROUP BY c.course_code, c.course_name ORDER BY c.course_code");
    }

    public List<Object[]> dailyEnrollments() throws SQLException {
        return queryRows("SELECT date_enrolled, COUNT(*) AS total_enrollments FROM ENROLLMENTS "
                + "GROUP BY date_enrolled ORDER BY date_enrolled DESC");
    }

    public List<Object[]> studentAttendance(int studentId) throws SQLException {
        return queryRows("SELECT sb.subject_code, sb.subject_name, e.school_year, e.semester, "
                + "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_days, "
                + "SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_days, "
                + "SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) AS late_days, "
                + "SUM(CASE WHEN a.status = 'EXCUSED' THEN 1 ELSE 0 END) AS excused_days "
                + "FROM ENROLLMENTS e INNER JOIN SUBJECTS sb ON e.subject_id = sb.subject_id "
                + "LEFT JOIN ATTENDANCE a ON a.enrollment_id = e.enrollment_id "
                + "WHERE e.student_id = ? "
                + "GROUP BY e.enrollment_id, sb.subject_code, sb.subject_name, e.school_year, e.semester "
                + "ORDER BY e.school_year DESC, e.semester DESC, sb.subject_code", studentId);
    }
}
