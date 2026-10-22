package model;

public class Course {
    private int courseId;
    private String courseCode, courseName, description;

    public Course() { }

    public Course(int courseId, String courseCode, String courseName, String description) {
        this.courseId = courseId; this.courseCode = courseCode;
        this.courseName = courseName; this.description = description;
    }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() { return courseCode + " - " + courseName; }
}
