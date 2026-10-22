package ui;

import dao.EnrollmentDAO;
import dao.ReportDAO;
import dao.StudentDAO;
import dao.SubjectDAO;
import model.Student;
import model.Subject;
import util.GradeUtil;
import util.UIUtil;
import util.Validator;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;

/** Reports shown in a JTable: lists, report card, transcript and summaries. */
public class ReportFrame extends BaseFrame {

    private static final String[] REPORTS = {
            "Student Master List",
            "Subject Offerings",
            "Enrollment List",
            "Class List (by Subject)",
            "Report Card (by Student)",
            "Transcript of Records (by Student)",
            "Grade Summary per Subject",
            "Attendance Summary",
            "Students per Course",
            "Daily Enrollment Summary"
    };

    private final ReportDAO reportDao = new ReportDAO();
    private final StudentDAO studentDao = new StudentDAO();
    private final SubjectDAO subjectDao = new SubjectDAO();
    private final EnrollmentDAO enrollmentDao = new EnrollmentDAO();

    private final JComboBox<String> cbReport = new JComboBox<>(REPORTS);
    private final JComboBox<Student> cbStudent = new JComboBox<>();
    private final JComboBox<Subject> cbSubject = new JComboBox<>();
    private final JTextField txtSchoolYear = UIUtil.field(10);
    private final JComboBox<Integer> cbSemester = new JComboBox<>(new Integer[]{1, 2, 3});
    private final JLabel lblSummary = new JLabel("Choose a report and click GENERATE.");
    private final JTable table = UIUtil.createTable();
    private String currentTitle = "Report";

    public ReportFrame() {
        super("Reports", "Generate lists, report cards, transcripts and summaries", 1200, 720);

        txtSchoolYear.setText(Validator.currentSchoolYear());
        lblSummary.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSummary.setForeground(UIUtil.PRIMARY);

        JPanel filters = formPanel();
        UIUtil.addField(filters, 0, 0, "Report:", cbReport);
        UIUtil.addField(filters, 1, 0, "Student:", cbStudent);
        UIUtil.addField(filters, 2, 0, "Subject:", cbSubject);
        UIUtil.addField(filters, 0, 1, "School Year:", txtSchoolYear);
        UIUtil.addField(filters, 1, 1, "Semester:", cbSemester);

        JButton btnGenerate = UIUtil.button("GENERATE", UIUtil.SUCCESS);
        JButton btnPrint = UIUtil.button("PRINT", UIUtil.PRIMARY);
        JButton btnExport = UIUtil.button("EXPORT CSV", UIUtil.WARNING);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(filters, BorderLayout.CENTER);
        top.add(buttonBar(btnGenerate, btnPrint, btnExport), BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(lblSummary, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        content.add(titled("Report Options (filters apply only to the reports that need them)", top), BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);

        try {
            for (Student s : studentDao.getAll()) cbStudent.addItem(s);
            for (Subject s : subjectDao.getAll()) cbSubject.addItem(s);
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }

        cbReport.addActionListener(e -> updateFilters());
        btnGenerate.addActionListener(e -> generate());
        btnPrint.addActionListener(e -> print());
        btnExport.addActionListener(e -> exportCsv());
        updateFilters();
    }

    private void updateFilters() {
        int r = cbReport.getSelectedIndex();
        boolean student = (r == 4 || r == 5);
        boolean subject = (r == 3);
        boolean term = (r == 3 || r == 4);
        cbStudent.setEnabled(student);
        cbSubject.setEnabled(subject);
        txtSchoolYear.setEnabled(term);
        cbSemester.setEnabled(term);
    }

    private void generate() {
        int r = cbReport.getSelectedIndex();
        currentTitle = REPORTS[r];
        try {
            switch (r) {
                case 0:
                    UIUtil.fillTable(table, StudentDAO.COLUMNS, studentDao.search(""));
                    lblSummary.setText("Total students: " + table.getRowCount());
                    break;
                case 1:
                    UIUtil.fillTable(table, SubjectDAO.COLUMNS, subjectDao.search(""));
                    lblSummary.setText("Total subjects: " + table.getRowCount());
                    break;
                case 2:
                    UIUtil.fillTable(table, EnrollmentDAO.COLUMNS, enrollmentDao.search(""));
                    lblSummary.setText("Total enrollment records: " + table.getRowCount());
                    break;
                case 3:
                    classList();
                    break;
                case 4:
                    reportCard();
                    break;
                case 5:
                    transcript();
                    break;
                case 6:
                    UIUtil.fillTable(table, ReportDAO.GRADE_SUMMARY_COLUMNS, reportDao.gradeSummary());
                    lblSummary.setText("Pass/fail counts and average final grade for every subject.");
                    break;
                case 7:
                    UIUtil.fillTable(table, ReportDAO.ATTENDANCE_SUMMARY_COLUMNS, reportDao.attendanceSummary());
                    lblSummary.setText("Attendance totals per student and subject.");
                    break;
                case 8:
                    UIUtil.fillTable(table, ReportDAO.COURSE_SUMMARY_COLUMNS, reportDao.studentsPerCourse());
                    lblSummary.setText("Number of students in each course.");
                    break;
                default:
                    UIUtil.fillTable(table, ReportDAO.DAILY_COLUMNS, reportDao.dailyEnrollments());
                    lblSummary.setText("Number of enrollments recorded per day.");
                    break;
            }
            if (table.getRowCount() == 0) {
                lblSummary.setText(lblSummary.getText() + "  (No matching records found.)");
            }
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private boolean validTerm() {
        if (!Validator.isValidSchoolYear(txtSchoolYear.getText())) {
            UIUtil.error(this, "School Year must look like 2026-2027 (second year = first year + 1).");
            return false;
        }
        return true;
    }

    private void classList() throws SQLException {
        Subject sb = (Subject) cbSubject.getSelectedItem();
        if (sb == null || !validTerm()) {
            if (sb == null) UIUtil.error(this, "Please select a subject.");
            return;
        }
        int sem = (Integer) cbSemester.getSelectedItem();
        String sy = txtSchoolYear.getText().trim();
        UIUtil.fillTable(table, ReportDAO.CLASS_LIST_COLUMNS, reportDao.classList(sb.getSubjectId(), sy, sem));
        currentTitle = "Class List - " + sb.getSubjectCode() + " SY " + sy + " Sem " + sem;
        lblSummary.setText(sb + "  |  SY " + sy + ", Semester " + sem + "  |  Students: " + table.getRowCount());
    }

    private void reportCard() throws SQLException {
        Student st = (Student) cbStudent.getSelectedItem();
        if (st == null || !validTerm()) {
            if (st == null) UIUtil.error(this, "Please select a student.");
            return;
        }
        int sem = (Integer) cbSemester.getSelectedItem();
        String sy = txtSchoolYear.getText().trim();
        List<Object[]> rows = reportDao.reportCard(st.getStudentId(), sy, sem);
        UIUtil.fillTable(table, ReportDAO.REPORT_CARD_COLUMNS, rows);
        currentTitle = "Report Card - " + st.getFullName() + " SY " + sy + " Sem " + sem;
        lblSummary.setText(summaryText(st, rows, 2, 6, "SY " + sy + ", Sem " + sem));
    }

    private void transcript() throws SQLException {
        Student st = (Student) cbStudent.getSelectedItem();
        if (st == null) {
            UIUtil.error(this, "Please select a student.");
            return;
        }
        List<Object[]> rows = reportDao.transcript(st.getStudentId());
        UIUtil.fillTable(table, ReportDAO.TRANSCRIPT_COLUMNS, rows);
        currentTitle = "Transcript of Records - " + st.getFullName();
        lblSummary.setText(summaryText(st, rows, 4, 5, "All terms"));
    }

    private String summaryText(Student st, List<Object[]> rows, int unitsIdx, int gradeIdx, String term) {
        int units = 0;
        for (Object[] r : rows) units += ((Number) r[unitsIdx]).intValue();
        Double gwa = GradeUtil.weightedAverage(rows, unitsIdx, gradeIdx, false);
        Double gpa = GradeUtil.weightedAverage(rows, unitsIdx, gradeIdx, true);
        return st + "  |  " + term + "  |  Units: " + units
                + "  |  GWA: " + (gwa == null ? "-" : String.format("%.2f", gwa))
                + "  |  GPA (equivalent): " + (gpa == null ? "-" : String.format("%.2f", gpa));
    }

    private void print() {
        if (table.getRowCount() == 0) {
            UIUtil.error(this, "Generate a report first.");
            return;
        }
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat(currentTitle), new MessageFormat("Page {0}"));
        } catch (java.awt.print.PrinterException ex) {
            UIUtil.error(this, "Printing failed: " + ex.getMessage());
        }
    }

    private void exportCsv() {
        if (table.getRowCount() == 0) {
            UIUtil.error(this, "Generate a report first.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(currentTitle.replaceAll("[^A-Za-z0-9._-]+", "_") + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new java.io.FileOutputStream(f), StandardCharsets.UTF_8))) {
            int cols = table.getColumnCount();
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < cols; c++) sb.append(c > 0 ? "," : "").append(csv(table.getColumnName(c)));
            out.println(sb);
            for (int r = 0; r < table.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < cols; c++) {
                    Object v = table.getValueAt(r, c);
                    sb.append(c > 0 ? "," : "").append(csv(v == null ? "" : v.toString()));
                }
                out.println(sb);
            }
            UIUtil.info(this, "Report exported to:\n" + f.getAbsolutePath());
        } catch (IOException ex) {
            UIUtil.error(this, "Could not save the file: " + ex.getMessage());
        }
    }

    private static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
