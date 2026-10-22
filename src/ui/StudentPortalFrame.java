package ui;

import dao.CourseDAO;
import dao.EnrollmentDAO;
import dao.ReportDAO;
import dao.StudentDAO;
import database.DatabaseConnection;
import model.Course;
import model.Student;
import util.GradeUtil;
import util.Session;
import util.UIUtil;
import util.Validator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * Read-only portal for STUDENT accounts. Every query uses the student_id stored in the
 * logged-in account, so a student can only ever see his or her own records.
 */
public class StudentPortalFrame extends BaseFrame {

    private final int studentId;
    private final StudentDAO studentDao = new StudentDAO();
    private final CourseDAO courseDao = new CourseDAO();
    private final EnrollmentDAO enrollmentDao = new EnrollmentDAO();
    private final ReportDAO reportDao = new ReportDAO();

    private final JTable tblSubjects = UIUtil.createTable();
    private final JTable tblCard = UIUtil.createTable();
    private final JTable tblTranscript = UIUtil.createTable();
    private final JTable tblAttendance = UIUtil.createTable();
    private final JTextField txtSchoolYear = UIUtil.field(10);
    private final JComboBox<Integer> cbSemester = new JComboBox<>(new Integer[]{1, 2, 3});
    private final JLabel lblCard = new JLabel(" ");
    private final JLabel lblTranscript = new JLabel(" ");
    private final JPanel profile = formPanel();

    public StudentPortalFrame() {
        super("My Student Portal", "View your enrollment, grades and attendance (read-only)", 1100, 700);

        Integer sid = Session.getUser().getStudentId();
        studentId = sid == null ? -1 : sid;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        txtSchoolYear.setText(Validator.currentSchoolYear());
        for (JLabel l : new JLabel[]{lblCard, lblTranscript}) {
            l.setFont(new Font("SansSerif", Font.BOLD, 14));
            l.setForeground(UIUtil.PRIMARY);
        }

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIUtil.BOLD);
        tabs.addTab("My Profile", profileTab());
        tabs.addTab("My Subjects", scroll(tblSubjects));
        tabs.addTab("Report Card", cardTab());
        tabs.addTab("Transcript", transcriptTab());
        tabs.addTab("Attendance", scroll(tblAttendance));

        JButton btnRefresh = UIUtil.button("REFRESH", UIUtil.NEUTRAL);
        JButton btnLogout = UIUtil.button("LOGOUT", UIUtil.DANGER);
        JLabel who = new JLabel("Logged in as: " + Session.getUser().getFullName() + "  (STUDENT)   |   Database: "
                + DatabaseConnection.getMode());
        who.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(who, BorderLayout.WEST);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnRefresh);
        btns.add(btnLogout);
        bottom.add(btns, BorderLayout.EAST);

        content.add(tabs, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadAll());
        btnLogout.addActionListener(e -> logout());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (UIUtil.confirm(StudentPortalFrame.this, "Exit the application?")) {
                    DatabaseConnection.shutdown();
                    System.exit(0);
                }
            }
        });

        if (studentId < 0) {
            UIUtil.error(this, "This account is not linked to a student record. Please contact the registrar.");
        } else {
            loadAll();
        }
    }

    private JScrollPane scroll(JTable t) {
        return new JScrollPane(t);
    }

    private JPanel profileTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(profile, BorderLayout.NORTH);
        return p;
    }

    private JPanel cardTab() {
        JButton btnShow = UIUtil.button("SHOW", UIUtil.SUCCESS);
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filter.setOpaque(false);
        filter.add(UIUtil.label("School Year:"));
        filter.add(txtSchoolYear);
        filter.add(UIUtil.label("Semester:"));
        filter.add(cbSemester);
        filter.add(btnShow);
        btnShow.addActionListener(e -> loadCard());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(filter, BorderLayout.NORTH);
        top.add(lblCard, BorderLayout.SOUTH);

        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(top, BorderLayout.NORTH);
        p.add(scroll(tblCard), BorderLayout.CENTER);
        return p;
    }

    private JPanel transcriptTab() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(lblTranscript, BorderLayout.NORTH);
        p.add(scroll(tblTranscript), BorderLayout.CENTER);
        return p;
    }

    private void loadAll() {
        loadProfile();
        try {
            UIUtil.fillTable(tblSubjects, EnrollmentDAO.STUDENT_COLUMNS, enrollmentDao.listByStudent(studentId));
            UIUtil.fillTable(tblAttendance, ReportDAO.STUDENT_ATTENDANCE_COLUMNS, reportDao.studentAttendance(studentId));
            List<Object[]> rows = reportDao.transcript(studentId);
            UIUtil.fillTable(tblTranscript, ReportDAO.TRANSCRIPT_COLUMNS, rows);
            lblTranscript.setText(summary(rows, 4, 5, "All terms"));
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
        loadCard();
    }

    private void loadCard() {
        if (studentId < 0) return;
        if (!Validator.isValidSchoolYear(txtSchoolYear.getText())) {
            UIUtil.error(this, "School Year must look like 2026-2027 (second year = first year + 1).");
            return;
        }
        try {
            String sy = txtSchoolYear.getText().trim();
            int sem = (Integer) cbSemester.getSelectedItem();
            List<Object[]> rows = reportDao.reportCard(studentId, sy, sem);
            UIUtil.fillTable(tblCard, ReportDAO.REPORT_CARD_COLUMNS, rows);
            lblCard.setText(rows.isEmpty() ? "No subjects found for SY " + sy + ", Semester " + sem + "."
                    : summary(rows, 2, 6, "SY " + sy + ", Sem " + sem));
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private String summary(List<Object[]> rows, int unitsIdx, int gradeIdx, String term) {
        int units = 0;
        for (Object[] r : rows) units += ((Number) r[unitsIdx]).intValue();
        Double gwa = GradeUtil.weightedAverage(rows, unitsIdx, gradeIdx, false);
        Double gpa = GradeUtil.weightedAverage(rows, unitsIdx, gradeIdx, true);
        return term + "  |  Units: " + units + "  |  GWA: " + (gwa == null ? "-" : String.format("%.2f", gwa))
                + "  |  GPA (equivalent): " + (gpa == null ? "-" : String.format("%.2f", gpa));
    }

    private void loadProfile() {
        profile.removeAll();
        try {
            Student s = studentDao.getById(studentId);
            if (s == null) return;
            Course c = courseDao.getById(s.getCourseId());
            addRo(0, 0, "Student No.:", s.getStudentNo());
            addRo(1, 0, "Name:", s.getFullName());
            addRo(0, 1, "Course:", c == null ? "" : c.toString());
            addRo(1, 1, "Year Level:", String.valueOf(s.getYearLevel()));
            addRo(0, 2, "Gender:", s.getGender());
            addRo(1, 2, "Birth Date:", s.getBirthDate() == null ? "" : s.getBirthDate().toString());
            addRo(0, 3, "Contact No.:", s.getContactNo());
            addRo(1, 3, "Email:", s.getEmail());
            addRo(0, 4, "Address:", s.getAddress());
            addRo(1, 4, "Status:", s.getStatus());
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
        profile.revalidate();
        profile.repaint();
    }

    private void addRo(int col, int row, String label, String value) {
        JTextField f = UIUtil.field(24);
        f.setText(value == null ? "" : value);
        f.setEditable(false);
        UIUtil.addField(profile, col, row, label, f);
    }

    private void logout() {
        if (UIUtil.confirm(this, "Are you sure you want to log out?")) {
            Session.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
