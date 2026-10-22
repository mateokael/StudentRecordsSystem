package model;

import java.sql.Date;

public class Attendance {
    private int attendanceId, enrollmentId;
    private Date attendanceDate;
    private String status, remarks;

    public Attendance() { }

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }
    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }
    public Date getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(Date attendanceDate) { this.attendanceDate = attendanceDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
