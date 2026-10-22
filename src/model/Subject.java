package model;

public class Subject {
    private int subjectId, units, courseId, yearLevel, semester;
    private String subjectCode, subjectName;

    public Subject() { }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public int getYearLevel() { return yearLevel; }
    public void setYearLevel(int yearLevel) { this.yearLevel = yearLevel; }
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    @Override
    public String toString() { return subjectCode + " - " + subjectName; }
}
