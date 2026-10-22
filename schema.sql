-- Student Enrollment, Grading and Record Management System
-- Apache Derby schema (the program creates these automatically on first run;
-- this file is for your documentation / manual setup in NetBeans).

CREATE TABLE COURSES (
    course_id   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_code VARCHAR(20)  NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE STUDENTS (
    student_id  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_no  VARCHAR(20)  NOT NULL UNIQUE,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    gender      VARCHAR(10)  NOT NULL,
    birth_date  DATE,
    contact_no  VARCHAR(30),
    email       VARCHAR(100),
    address     VARCHAR(255),
    course_id   INTEGER NOT NULL,
    year_level  INTEGER NOT NULL DEFAULT 1,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_student_course FOREIGN KEY (course_id) REFERENCES COURSES(course_id)
);

CREATE TABLE USERS (
    user_id    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,              -- SHA-256 hash
    full_name  VARCHAR(100) NOT NULL,
    role       VARCHAR(30)  NOT NULL,              -- ADMIN / REGISTRAR / TEACHER / STUDENT
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    student_id INTEGER,                            -- only for STUDENT accounts
    CONSTRAINT fk_user_student FOREIGN KEY (student_id) REFERENCES STUDENTS(student_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_student UNIQUE (student_id)
);

CREATE TABLE SUBJECTS (
    subject_id   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_code VARCHAR(20)  NOT NULL UNIQUE,
    subject_name VARCHAR(100) NOT NULL,
    units        INTEGER NOT NULL DEFAULT 3,
    course_id    INTEGER NOT NULL,
    year_level   INTEGER NOT NULL DEFAULT 1,
    semester     INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_subject_course FOREIGN KEY (course_id) REFERENCES COURSES(course_id)
);

CREATE TABLE ENROLLMENTS (
    enrollment_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id    INTEGER NOT NULL,
    subject_id    INTEGER NOT NULL,
    school_year   VARCHAR(9) NOT NULL,
    semester      INTEGER NOT NULL,
    date_enrolled DATE NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    CONSTRAINT fk_enr_student FOREIGN KEY (student_id) REFERENCES STUDENTS(student_id),
    CONSTRAINT fk_enr_subject FOREIGN KEY (subject_id) REFERENCES SUBJECTS(subject_id),
    CONSTRAINT uq_enrollment UNIQUE (student_id, subject_id, school_year, semester)
);

CREATE TABLE GRADES (
    grade_id       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enrollment_id  INTEGER NOT NULL UNIQUE,
    prelim         DECIMAL(5,2) CHECK (prelim >= 0 AND prelim <= 100),
    midterm        DECIMAL(5,2) CHECK (midterm >= 0 AND midterm <= 100),
    finals         DECIMAL(5,2) CHECK (finals >= 0 AND finals <= 100),
    final_grade    DECIMAL(5,2) CHECK (final_grade >= 0 AND final_grade <= 100),
    remarks        VARCHAR(20),
    CONSTRAINT fk_grade_enr FOREIGN KEY (enrollment_id) REFERENCES ENROLLMENTS(enrollment_id) ON DELETE CASCADE
);

CREATE TABLE ATTENDANCE (
    attendance_id   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enrollment_id   INTEGER NOT NULL,
    attendance_date DATE NOT NULL,
    status          VARCHAR(20) NOT NULL,          -- PRESENT / ABSENT / LATE / EXCUSED
    remarks         VARCHAR(100),
    CONSTRAINT fk_att_enr FOREIGN KEY (enrollment_id) REFERENCES ENROLLMENTS(enrollment_id) ON DELETE CASCADE,
    CONSTRAINT uq_attendance UNIQUE (enrollment_id, attendance_date)
);
