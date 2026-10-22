package database;

import util.PasswordUtil;

import java.sql.*;
import java.time.LocalDate;

/** Creates all tables (if missing) and inserts sample data the first time the program runs. */
public class DatabaseInitializer {

    public static void initialize() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            verifySchema(c, true);    // before creating anything: reject a foreign/old database early
            createTables(c);
            verifySchema(c, false);   // after creating/upgrading: everything must match
            seedData(c);
        }
    }

    private static boolean tableExists(Connection c, String name) throws SQLException {
        try (ResultSet rs = c.getMetaData().getTables(null, null, name, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static void createIfMissing(Connection c, String table, String sql) throws SQLException {
        if (!tableExists(c, table)) {
            try (Statement st = c.createStatement()) {
                st.execute(sql);
            }
        }
    }

    private static void createTables(Connection c) throws SQLException {
        createIfMissing(c, "COURSES",
            "CREATE TABLE COURSES ("
          + " course_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
          + " course_code VARCHAR(20) NOT NULL UNIQUE,"
          + " course_name VARCHAR(100) NOT NULL,"
          + " description VARCHAR(255))");

        createIfMissing(c, "STUDENTS",
            "CREATE TABLE STUDENTS ("
          + " student_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
          + " student_no VARCHAR(20) NOT NULL UNIQUE,"
          + " first_name VARCHAR(50) NOT NULL,"
          + " last_name VARCHAR(50) NOT NULL,"
          + " gender VARCHAR(10) NOT NULL,"
          + " birth_date DATE,"
          + " contact_no VARCHAR(30),"
          + " email VARCHAR(100),"
          + " address VARCHAR(255),"
          + " course_id INTEGER NOT NULL,"
          + " year_level INTEGER NOT NULL DEFAULT 1,"
          + " status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',"
          + " CONSTRAINT fk_student_course FOREIGN KEY (course_id) REFERENCES COURSES(course_id))");

        createIfMissing(c, "USERS",
            "CREATE TABLE USERS ("
          + " user_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
          + " username VARCHAR(50) NOT NULL UNIQUE,"
          + " password VARCHAR(255) NOT NULL,"
          + " full_name VARCHAR(100) NOT NULL,"
          + " role VARCHAR(30) NOT NULL,"
          + " status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',"
          + " student_id INTEGER,"
          + " CONSTRAINT fk_user_student FOREIGN KEY (student_id) REFERENCES STUDENTS(student_id) ON DELETE CASCADE,"
          + " CONSTRAINT uq_user_student UNIQUE (student_id))");
        upgradeUsersTable(c);
        fixLegacyRoleCheck(c);

        createIfMissing(c, "SUBJECTS",
            "CREATE TABLE SUBJECTS ("
          + " subject_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
          + " subject_code VARCHAR(20) NOT NULL UNIQUE,"
          + " subject_name VARCHAR(100) NOT NULL,"
          + " units INTEGER NOT NULL DEFAULT 3,"
          + " course_id INTEGER NOT NULL,"
          + " year_level INTEGER NOT NULL DEFAULT 1,"
          + " semester INTEGER NOT NULL DEFAULT 1,"
          + " CONSTRAINT fk_subject_course FOREIGN KEY (course_id) REFERENCES COURSES(course_id))");

        createIfMissing(c, "ENROLLMENTS",
            "CREATE TABLE ENROLLMENTS ("
          + " enrollment_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
          + " student_id INTEGER NOT NULL,"
          + " subject_id INTEGER NOT NULL,"
          + " school_year VARCHAR(9) NOT NULL,"
          + " semester INTEGER NOT NULL,"
          + " date_enrolled DATE NOT NULL,"
          + " status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',"
          + " CONSTRAINT fk_enr_student FOREIGN KEY (student_id) REFERENCES STUDENTS(student_id),"
          + " CONSTRAINT fk_enr_subject FOREIGN KEY (subject_id) REFERENCES SUBJECTS(subject_id),"
          + " CONSTRAINT uq_enrollment UNIQUE (student_id, subject_id, school_year, semester))");

        createIfMissing(c, "GRADES",
            "CREATE TABLE GRADES ("
          + " grade_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
          + " enrollment_id INTEGER NOT NULL UNIQUE,"
          + " prelim DECIMAL(5,2) CHECK (prelim >= 0 AND prelim <= 100),"
          + " midterm DECIMAL(5,2) CHECK (midterm >= 0 AND midterm <= 100),"
          + " finals DECIMAL(5,2) CHECK (finals >= 0 AND finals <= 100),"
          + " final_grade DECIMAL(5,2) CHECK (final_grade >= 0 AND final_grade <= 100),"
          + " remarks VARCHAR(20),"
          + " CONSTRAINT fk_grade_enr FOREIGN KEY (enrollment_id) REFERENCES ENROLLMENTS(enrollment_id) ON DELETE CASCADE)");

        createIfMissing(c, "ATTENDANCE",
            "CREATE TABLE ATTENDANCE ("
          + " attendance_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
          + " enrollment_id INTEGER NOT NULL,"
          + " attendance_date DATE NOT NULL,"
          + " status VARCHAR(20) NOT NULL,"
          + " remarks VARCHAR(100),"
          + " CONSTRAINT fk_att_enr FOREIGN KEY (enrollment_id) REFERENCES ENROLLMENTS(enrollment_id) ON DELETE CASCADE,"
          + " CONSTRAINT uq_attendance UNIQUE (enrollment_id, attendance_date))");
    }

    /** Older databases created before STUDENT accounts existed get the new column automatically. */
    private static void upgradeUsersTable(Connection c) throws SQLException {
        try (ResultSet rs = c.getMetaData().getColumns(null, null, "USERS", "STUDENT_ID")) {
            if (rs.next()) return;
        }
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE USERS ADD COLUMN student_id INTEGER");
            st.execute("ALTER TABLE USERS ADD CONSTRAINT fk_user_student FOREIGN KEY (student_id) "
                     + "REFERENCES STUDENTS(student_id) ON DELETE CASCADE");
            st.execute("ALTER TABLE USERS ADD CONSTRAINT uq_user_student UNIQUE (student_id)");
        } catch (SQLException ignored) {
            // verifySchema() reports a clear message if the table still does not match
        }
    }

    private static final String[][] EXPECTED = {
        {"USERS", "user_id", "username", "password", "full_name", "role", "status", "student_id"},
        {"COURSES", "course_id", "course_code", "course_name", "description"},
        {"STUDENTS", "student_id", "student_no", "first_name", "last_name", "gender", "birth_date", "contact_no",
                "email", "address", "course_id", "year_level", "status"},
        {"SUBJECTS", "subject_id", "subject_code", "subject_name", "units", "course_id", "year_level", "semester"},
        {"ENROLLMENTS", "enrollment_id", "student_id", "subject_id", "school_year", "semester", "date_enrolled", "status"},
        {"GRADES", "grade_id", "enrollment_id", "prelim", "midterm", "finals", "final_grade", "remarks"},
        {"ATTENDANCE", "attendance_id", "enrollment_id", "attendance_date", "status", "remarks"}
    };

    /** Stops with a clear message if the database already contains tables made by a different program/version. */
    private static void verifySchema(Connection c, boolean beforeCreate) throws SQLException {
        for (String[] t : EXPECTED) {
            java.util.Set<String> have = new java.util.HashSet<>();
            try (ResultSet rs = c.getMetaData().getColumns(null, null, t[0], null)) {
                while (rs.next()) have.add(rs.getString("COLUMN_NAME").toUpperCase());
            }
            if (beforeCreate && have.isEmpty()) continue;   // table not created yet
            for (int i = 1; i < t.length; i++) {
                if (beforeCreate && t[0].equals("USERS") && t[i].equals("student_id")) continue;   // added by upgrade
                if (!have.contains(t[i].toUpperCase())) {
                    throw new SQLException("The database '" + DatabaseConnection.DB_NAME + "' already has a table named "
                            + t[0] + " that was created by a different program or version (missing column "
                            + t[i].toUpperCase() + ").\n\nFix: close the program, delete the folder "
                            + DatabaseConnection.DB_NAME + " (embedded mode) or the database of the same name in the "
                            + "Derby server, or change DB_NAME in DatabaseConnection.java to a new name.", "SCHEMA");
                }
            }
        }
    }

    /**
     * Some older USERS tables carry a CHECK constraint (CK_USERS_ROLE) that only allows the first three roles.
     * Replace it with one that also allows STUDENT.
     */
    private static void fixLegacyRoleCheck(Connection c) {
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE USERS DROP CONSTRAINT ck_users_role");
        } catch (SQLException notThere) {
            return;   // no legacy constraint: nothing to fix
        }
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE USERS ADD CONSTRAINT ck_users_role "
                     + "CHECK (role IN ('ADMIN', 'REGISTRAR', 'TEACHER', 'STUDENT'))");
        } catch (SQLException ignored) {
            // leave the table without the check rather than stop the program
        }
    }

    // ------------------------------------------------------------------ sample data

    private static int count(Connection c, String table) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int idOf(Connection c, String sql, String key) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static void seedData(Connection c) throws SQLException {
        if (count(c, "USERS") == 0) {
            addUser(c, "admin", "admin123", "System Administrator", "ADMIN");
            addUser(c, "registrar", "registrar123", "Maria Santos", "REGISTRAR");
            addUser(c, "teacher", "teacher123", "Juan Dela Cruz", "TEACHER");
        }

        if (count(c, "COURSES") == 0) {
            addCourse(c, "BSIT", "Bachelor of Science in Information Technology", "4-year IT program");
            addCourse(c, "BSCS", "Bachelor of Science in Computer Science", "4-year Computer Science program");
            addCourse(c, "BSBA", "Bachelor of Science in Business Administration", "4-year Business program");
        }

        if (count(c, "SUBJECTS") == 0) {
            int it = idOf(c, "SELECT course_id FROM COURSES WHERE course_code = ?", "BSIT");
            int cs = idOf(c, "SELECT course_id FROM COURSES WHERE course_code = ?", "BSCS");
            int ba = idOf(c, "SELECT course_id FROM COURSES WHERE course_code = ?", "BSBA");
            addSubject(c, "IT101", "Introduction to Computing", 3, it, 1, 1);
            addSubject(c, "IT102", "Computer Programming 1", 3, it, 1, 1);
            addSubject(c, "IT103", "Digital Literacy", 3, it, 1, 1);
            addSubject(c, "IT104", "Discrete Mathematics", 3, it, 1, 1);
            addSubject(c, "IT105", "Computer Programming 2", 3, it, 1, 2);
            addSubject(c, "IT106", "Database Management Systems", 3, it, 1, 2);
            addSubject(c, "IT107", "Networking Fundamentals", 3, it, 1, 2);
            addSubject(c, "CS101", "Introduction to Computer Science", 3, cs, 1, 1);
            addSubject(c, "CS102", "Programming Fundamentals", 3, cs, 1, 1);
            addSubject(c, "BA101", "Principles of Management", 3, ba, 1, 1);
            addSubject(c, "BA102", "Business Mathematics", 3, ba, 1, 1);
        }

        if (count(c, "STUDENTS") == 0) {
            int it = idOf(c, "SELECT course_id FROM COURSES WHERE course_code = ?", "BSIT");
            int cs = idOf(c, "SELECT course_id FROM COURSES WHERE course_code = ?", "BSCS");
            int ba = idOf(c, "SELECT course_id FROM COURSES WHERE course_code = ?", "BSBA");
            addStudent(c, "2026-0001", "Cholo", "Reyes", "Male", "2007-03-14", "09171234567", "cholo.reyes@example.com", "Quezon City", it);
            addStudent(c, "2026-0002", "Angela", "Garcia", "Female", "2007-07-02", "09181234567", "angela.garcia@example.com", "Caloocan City", it);
            addStudent(c, "2026-0003", "Mark", "Villanueva", "Male", "2006-11-21", "09191234567", "mark.v@example.com", "Marikina City", cs);
            addStudent(c, "2026-0004", "Sofia", "Mendoza", "Female", "2007-01-30", "09201234567", "sofia.m@example.com", "Pasig City", ba);
        }

        if (count(c, "ENROLLMENTS") == 0) {
            String sy = util.Validator.currentSchoolYear();
            int s1 = idOf(c, "SELECT student_id FROM STUDENTS WHERE student_no = ?", "2026-0001");
            int s2 = idOf(c, "SELECT student_id FROM STUDENTS WHERE student_no = ?", "2026-0002");
            String[] codes = {"IT101", "IT102", "IT103", "IT104"};
            double[][] g1 = {{90, 92, 94}, {85, 88, 87}, {95, 96, 97}, {72, 70, 74}};
            double[][] g2 = {{88, 85, 90}, {79, 81, 80}, {92, 90, 91}, {83, 84, 86}};
            for (int i = 0; i < codes.length; i++) {
                int sub = idOf(c, "SELECT subject_id FROM SUBJECTS WHERE subject_code = ?", codes[i]);
                int e1 = addEnrollment(c, s1, sub, sy, 1);
                addGrade(c, e1, g1[i][0], g1[i][1], g1[i][2]);
                int e2 = addEnrollment(c, s2, sub, sy, 1);
                addGrade(c, e2, g2[i][0], g2[i][1], g2[i][2]);
                if (i == 0) {
                    addAttendance(c, e1, LocalDate.now().minusDays(2), "PRESENT");
                    addAttendance(c, e1, LocalDate.now().minusDays(1), "LATE");
                    addAttendance(c, e2, LocalDate.now().minusDays(2), "PRESENT");
                    addAttendance(c, e2, LocalDate.now().minusDays(1), "ABSENT");
                }
            }
        }

        try {
            if (count(c, "USERS WHERE role = 'STUDENT'") == 0) {
                int sid = idOf(c, "SELECT student_id FROM STUDENTS WHERE student_no = ?", "2026-0001");
                if (sid > 0) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO USERS (username, password, full_name, role, status, student_id) VALUES (?, ?, ?, 'STUDENT', 'ACTIVE', ?)")) {
                        ps.setString(1, "student");
                        ps.setString(2, PasswordUtil.hash("student123"));
                        ps.setString(3, "Cholo Reyes");
                        ps.setInt(4, sid);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException ignored) {
            // the sample student account is optional; create student accounts from the Users screen instead
        }
    }

    private static void addUser(Connection c, String user, String pass, String name, String role) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO USERS (username, password, full_name, role, status) VALUES (?, ?, ?, ?, 'ACTIVE')")) {
            ps.setString(1, user);
            ps.setString(2, PasswordUtil.hash(pass));
            ps.setString(3, name);
            ps.setString(4, role);
            ps.executeUpdate();
        }
    }

    private static void addCourse(Connection c, String code, String name, String desc) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO COURSES (course_code, course_name, description) VALUES (?, ?, ?)")) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, desc);
            ps.executeUpdate();
        }
    }

    private static void addSubject(Connection c, String code, String name, int units, int course, int year, int sem) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO SUBJECTS (subject_code, subject_name, units, course_id, year_level, semester) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setInt(3, units);
            ps.setInt(4, course);
            ps.setInt(5, year);
            ps.setInt(6, sem);
            ps.executeUpdate();
        }
    }

    private static void addStudent(Connection c, String no, String first, String last, String gender,
                                   String birth, String contact, String email, String address, int course) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO STUDENTS (student_no, first_name, last_name, gender, birth_date, contact_no, email, address, course_id, year_level, status) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'ACTIVE')")) {
            ps.setString(1, no);
            ps.setString(2, first);
            ps.setString(3, last);
            ps.setString(4, gender);
            ps.setDate(5, Date.valueOf(birth));
            ps.setString(6, contact);
            ps.setString(7, email);
            ps.setString(8, address);
            ps.setInt(9, course);
            ps.executeUpdate();
        }
    }

    private static int addEnrollment(Connection c, int student, int subject, String sy, int sem) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ENROLLMENTS (student_id, subject_id, school_year, semester, date_enrolled, status) VALUES (?, ?, ?, ?, ?, 'ENROLLED')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, student);
            ps.setInt(2, subject);
            ps.setString(3, sy);
            ps.setInt(4, sem);
            ps.setDate(5, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void addGrade(Connection c, int enrollmentId, double p, double m, double f) throws SQLException {
        double fin = util.GradeUtil.round2((p + m + f) / 3.0);
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO GRADES (enrollment_id, prelim, midterm, finals, final_grade, remarks) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, enrollmentId);
            ps.setDouble(2, p);
            ps.setDouble(3, m);
            ps.setDouble(4, f);
            ps.setDouble(5, fin);
            ps.setString(6, util.GradeUtil.remarks(fin));
            ps.executeUpdate();
        }
    }

    private static void addAttendance(Connection c, int enrollmentId, LocalDate date, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO ATTENDANCE (enrollment_id, attendance_date, status, remarks) VALUES (?, ?, ?, '')")) {
            ps.setInt(1, enrollmentId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, status);
            ps.executeUpdate();
        }
    }
}
