package dao;

import model.Course;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO extends BaseDAO {

    public static final String[] COLUMNS = {"ID", "Course Code", "Course Name", "Description"};

    private Course map(Object[] r) {
        return new Course(((Number) r[0]).intValue(), (String) r[1], (String) r[2], (String) r[3]);
    }

    public void insert(Course c) throws SQLException {
        executeUpdate("INSERT INTO COURSES (course_code, course_name, description) VALUES (?, ?, ?)",
                c.getCourseCode(), c.getCourseName(), c.getDescription());
    }

    public void update(Course c) throws SQLException {
        executeUpdate("UPDATE COURSES SET course_code = ?, course_name = ?, description = ? WHERE course_id = ?",
                c.getCourseCode(), c.getCourseName(), c.getDescription(), c.getCourseId());
    }

    public void delete(int id) throws SQLException {
        executeUpdate("DELETE FROM COURSES WHERE course_id = ?", id);
    }

    public Course getById(int id) throws SQLException {
        List<Object[]> rows = queryRows("SELECT course_id, course_code, course_name, description FROM COURSES WHERE course_id = ?", id);
        return rows.isEmpty() ? null : map(rows.get(0));
    }

    public List<Course> getAll() throws SQLException {
        List<Course> list = new ArrayList<>();
        for (Object[] r : queryRows("SELECT course_id, course_code, course_name, description FROM COURSES ORDER BY course_code")) {
            list.add(map(r));
        }
        return list;
    }

    public List<Object[]> search(String keyword) throws SQLException {
        String k = like(keyword);
        return queryRows("SELECT course_id, course_code, course_name, description FROM COURSES "
                + "WHERE UPPER(course_code) LIKE ? OR UPPER(course_name) LIKE ? OR UPPER(description) LIKE ? "
                + "ORDER BY course_code", k, k, k);
    }
}
