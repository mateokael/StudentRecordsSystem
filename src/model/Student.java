package model;

import java.sql.Date;

public class Student {
    private int studentId, courseId, yearLevel;
    private String studentNo, firstName, lastName, gender, contactNo, email, address, status;
    private Date birthDate;

    public Student() { }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Date getBirthDate() { return birthDate; }
    public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }
    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public int getYearLevel() { return yearLevel; }
    public void setYearLevel(int yearLevel) { this.yearLevel = yearLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFullName() { return lastName + ", " + firstName; }

    @Override
    public String toString() { return studentNo + " - " + getFullName(); }
}
