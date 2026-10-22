package dao;

import model.Subject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SubjectDAO extends BaseDAO {

    public static final String[] COLUMNS = {"ID", "Subject Code", "Subject Name", "Units", "Course", "Year", "Semester"};

    private static final String BASE =
            "SELECT subject_id, subject_code, subject_name, units, course_id, year_level, semester FROM SUBJECTS ";

    private Subject map(Object[] r) {
        Subject s = new Subject();
        s.setSubjectId(((Number) r[0]).intValue());
        s.setSubjectCode((String) r[1]);
        s.setSubjectName((String) r[2]);
        s.setUnits(((Number) r[3]).intValue());
        s.setCourseId(((Number) r[4]).intValue());
        s.setYearLevel(((Number) r[5]).intValue());
        s.setSemester(((Number) r[6]).intValue());
        return s;
    }

    public void insert(Subject s) throws SQLException {
        executeUpdate("INSERT INTO SUBJECTS (subject_code, subject_name, units, course_id, year_level, semester) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                s.getSubjectCode(), s.getSubjectName(), s.getUnits(), s.getCourseId(), s.getYearLevel(), s.getSemester());
    }

    public void update(Subject s) throws SQLException {
        executeUpdate("UPDATE SUBJECTS SET subject_code = ?, subject_name = ?, units = ?, course_id = ?, "
                + "year_level = ?, semester = ? WHERE subject_id = ?",
                s.getSubjectCode(), s.getSubjectName(), s.getUnits(), s.getCourseId(), s.getYearLevel(),
                s.getSemester(), s.getSubjectId());
    }

    public void delete(int id) throws SQLException {
        executeUpdate("DELETE FROM SUBJECTS WHERE subject_id = ?", id);
    }

    public Subject getById(int id) throws SQLException {
        List<Object[]> rows = queryRows(BASE + "WHERE subject_id = ?", id);
        return rows.isEmpty() ? null : map(rows.get(0));
    }

    public List<Subject> getAll() throws SQLException {
        List<Subject> list = new ArrayList<>();
        for (Object[] r : queryRows(BASE + "ORDER BY subject_code")) list.add(map(r));
        return list;
    }

    public List<Subject> getByCourse(int courseId) throws SQLException {
        List<Subject> list = new ArrayList<>();
        for (Object[] r : queryRows(BASE + "WHERE course_id = ? ORDER BY year_level, semester, subject_code", courseId)) {
            list.add(map(r));
        }
        return list;
    }

    public List<Subject> getByTerm(int courseId, int yearLevel, int semester) throws SQLException {
        List<Subject> list = new ArrayList<>();
        for (Object[] r : queryRows(BASE + "WHERE course_id = ? AND year_level = ? AND semester = ? ORDER BY subject_code",
                courseId, yearLevel, semester)) {
            list.add(map(r));
        }
        return list;
    }

    public List<Object[]> search(String keyword) throws SQLException {
        String k = like(keyword);
        return queryRows("SELECT sb.subject_id, sb.subject_code, sb.subject_name, sb.units, c.course_code, "
                + "sb.year_level, sb.semester "
                + "FROM SUBJECTS sb INNER JOIN COURSES c ON sb.course_id = c.course_id "
                + "WHERE UPPER(sb.subject_code) LIKE ? OR UPPER(sb.subject_name) LIKE ? OR UPPER(c.course_code) LIKE ? "
                + "ORDER BY c.course_code, sb.year_level, sb.semester, sb.subject_code", k, k, k);
    }
}
