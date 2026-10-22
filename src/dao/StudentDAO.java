package dao;

import model.Student;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO extends BaseDAO {

    public static final String[] COLUMNS = {"ID", "Student No", "Last Name", "First Name", "Gender", "Birth Date",
            "Contact No", "Email", "Address", "Course", "Year", "Status"};

    private static final String BASE =
            "SELECT student_id, student_no, first_name, last_name, gender, birth_date, contact_no, email, address, "
          + "course_id, year_level, status FROM STUDENTS ";

    private Student map(Object[] r) {
        Student s = new Student();
        s.setStudentId(((Number) r[0]).intValue());
        s.setStudentNo((String) r[1]);
        s.setFirstName((String) r[2]);
        s.setLastName((String) r[3]);
        s.setGender((String) r[4]);
        s.setBirthDate((Date) r[5]);
        s.setContactNo((String) r[6]);
        s.setEmail((String) r[7]);
        s.setAddress((String) r[8]);
        s.setCourseId(((Number) r[9]).intValue());
        s.setYearLevel(((Number) r[10]).intValue());
        s.setStatus((String) r[11]);
        return s;
    }

    /** Builds the next student number for the current year, e.g. 2026-0006. */
    public String generateStudentNo() throws SQLException {
        String prefix = java.time.Year.now().getValue() + "-";
        List<Object[]> rows = queryRows(
                "SELECT MAX(student_no) FROM STUDENTS WHERE student_no LIKE ?", prefix + "%");
        int next = 1;
        if (!rows.isEmpty() && rows.get(0)[0] != null) {
            String last = (String) rows.get(0)[0];                 // e.g. 2026-0005
            try {
                next = Integer.parseInt(last.substring(prefix.length())) + 1;
            } catch (NumberFormatException ex) {
                next = 1;
            }
        }
        return String.format("%s%04d", prefix, next);
    }

    public void insert(Student s) throws SQLException {
        executeUpdate("INSERT INTO STUDENTS (student_no, first_name, last_name, gender, birth_date, contact_no, email, "
                + "address, course_id, year_level, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                s.getStudentNo(), s.getFirstName(), s.getLastName(), s.getGender(), s.getBirthDate(),
                s.getContactNo(), s.getEmail(), s.getAddress(), s.getCourseId(), s.getYearLevel(), s.getStatus());
    }

    public void update(Student s) throws SQLException {
        executeUpdate("UPDATE STUDENTS SET student_no = ?, first_name = ?, last_name = ?, gender = ?, birth_date = ?, "
                + "contact_no = ?, email = ?, address = ?, course_id = ?, year_level = ?, status = ? WHERE student_id = ?",
                s.getStudentNo(), s.getFirstName(), s.getLastName(), s.getGender(), s.getBirthDate(),
                s.getContactNo(), s.getEmail(), s.getAddress(), s.getCourseId(), s.getYearLevel(), s.getStatus(),
                s.getStudentId());
    }

    public void delete(int id) throws SQLException {
        executeUpdate("DELETE FROM STUDENTS WHERE student_id = ?", id);
    }

    public Student getById(int id) throws SQLException {
        List<Object[]> rows = queryRows(BASE + "WHERE student_id = ?", id);
        return rows.isEmpty() ? null : map(rows.get(0));
    }

    public List<Student> getAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        for (Object[] r : queryRows(BASE + "ORDER BY last_name, first_name")) list.add(map(r));
        return list;
    }

    /** Search uses an INNER JOIN with COURSES so the course code is shown instead of the ID. */
    public List<Object[]> search(String keyword) throws SQLException {
        String k = like(keyword);
        return queryRows("SELECT s.student_id, s.student_no, s.last_name, s.first_name, s.gender, s.birth_date, "
                + "s.contact_no, s.email, s.address, c.course_code, s.year_level, s.status "
                + "FROM STUDENTS s INNER JOIN COURSES c ON s.course_id = c.course_id "
                + "WHERE UPPER(s.student_no) LIKE ? OR UPPER(s.first_name) LIKE ? OR UPPER(s.last_name) LIKE ? "
                + "OR UPPER(c.course_code) LIKE ? OR UPPER(s.status) LIKE ? "
                + "ORDER BY s.last_name, s.first_name", k, k, k, k, k);
    }
}