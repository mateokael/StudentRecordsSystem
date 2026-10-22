package model;

import java.sql.Date;

public class Enrollment {
    private int enrollmentId, studentId, subjectId, semester;
    private String schoolYear, status;
    private Date dateEnrolled;

    public Enrollment() { }

    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    public String getSchoolYear() { return schoolYear; }
    public void setSchoolYear(String schoolYear) { this.schoolYear = schoolYear; }
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }
    public Date getDateEnrolled() { return dateEnrolled; }
    public void setDateEnrolled(Date dateEnrolled) { this.dateEnrolled = dateEnrolled; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
