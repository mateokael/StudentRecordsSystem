package model;

public class Grade {
    private int gradeId, enrollmentId;
    private Double prelim, midterm, finals, finalGrade;
    private String remarks;

    public Grade() { }

    public int getGradeId() { return gradeId; }
    public void setGradeId(int gradeId) { this.gradeId = gradeId; }
    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }
    public Double getPrelim() { return prelim; }
    public void setPrelim(Double prelim) { this.prelim = prelim; }
    public Double getMidterm() { return midterm; }
    public void setMidterm(Double midterm) { this.midterm = midterm; }
    public Double getFinals() { return finals; }
    public void setFinals(Double finals) { this.finals = finals; }
    public Double getFinalGrade() { return finalGrade; }
    public void setFinalGrade(Double finalGrade) { this.finalGrade = finalGrade; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
