package ui;

import dao.EnrollmentDAO;
import dao.StudentDAO;
import dao.SubjectDAO;
import model.Enrollment;
import model.Student;
import model.Subject;
import util.Session;
import util.UIUtil;
import util.Validator;

import javax.swing.*;
import java.sql.SQLException;

/** Enroll students in subjects per school year and semester. */
public class EnrollmentFrame extends BaseFrame {

    private final EnrollmentDAO dao = new EnrollmentDAO();
    private final StudentDAO studentDao = new StudentDAO();
    private final SubjectDAO subjectDao = new SubjectDAO();

    private final JTextField txtId = UIUtil.field(6), txtSchoolYear = UIUtil.field(10), txtSearch = UIUtil.field(20);
    private final JComboBox<Student> cbStudent = new JComboBox<>();
    private final JComboBox<Subject> cbSubject = new JComboBox<>();
    private final JComboBox<Integer> cbSemester = new JComboBox<>(new Integer[]{1, 2, 3});
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"ENROLLED", "DROPPED", "COMPLETED"});
    private final JTable table = UIUtil.createTable();
    private boolean adjusting = false;

    public EnrollmentFrame() {
        super("Enrollment", "Enroll students in subjects for a school year and semester", 1200, 720);

        txtId.setEditable(false);
        txtSchoolYear.setText(Validator.currentSchoolYear());
        txtSchoolYear.setToolTipText("Format: 2026-2027");

        JPanel form = formPanel();
        UIUtil.addField(form, 0, 0, "Enrollment ID:", txtId);
        UIUtil.addField(form, 1, 0, "Student:", cbStudent);
        UIUtil.addField(form, 2, 0, "Subject:", cbSubject);
        UIUtil.addField(form, 0, 1, "School Year:", txtSchoolYear);
        UIUtil.addField(form, 1, 1, "Semester:", cbSemester);
        UIUtil.addField(form, 2, 1, "Status:", cbStatus);

        JButton btnEnroll = UIUtil.button("ENROLL", UIUtil.SUCCESS);
        JButton btnTerm = UIUtil.button("ENROLL WHOLE TERM", UIUtil.WARNING);
        JButton btnUpdate = UIUtil.button("UPDATE STATUS", UIUtil.PRIMARY);
        JButton btnDelete = UIUtil.button("DELETE", UIUtil.DANGER);
        JButton btnClear = UIUtil.button("CLEAR", UIUtil.NEUTRAL);
        JButton btnSearch = UIUtil.button("SEARCH", UIUtil.PRIMARY);
        JButton btnRefresh = UIUtil.button("REFRESH", UIUtil.NEUTRAL);

        layoutCrud(form, buttonBar(btnEnroll, btnTerm, btnUpdate, btnDelete, btnClear),
                searchBar(txtSearch, btnSearch, btnRefresh), table);

        if (!Session.canManageRecords()) disable(btnEnroll, btnTerm, btnUpdate, btnDelete);

        try {
            for (Student s : studentDao.getAll()) cbStudent.addItem(s);
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
        loadSubjectsForStudent();

        cbStudent.addActionListener(e -> {
            if (!adjusting) loadSubjectsForStudent();
        });
        btnEnroll.addActionListener(e -> enroll());
        btnTerm.addActionListener(e -> enrollTerm());
        btnUpdate.addActionListener(e -> updateStatus());
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> loadTable());
        txtSearch.addActionListener(e -> loadTable());
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            loadTable();
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillForm();
        });
        loadTable();
    }

    /** Only subjects of the selected student's course are offered. */
    private void loadSubjectsForStudent() {
        cbSubject.removeAllItems();
        Student s = (Student) cbStudent.getSelectedItem();
        if (s == null) return;
        try {
            for (Subject sb : subjectDao.getByCourse(s.getCourseId())) cbSubject.addItem(sb);
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void loadTable() {
        try {
            UIUtil.fillTable(table, EnrollmentDAO.COLUMNS, dao.search(txtSearch.getText()));
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void fillForm() {
        int id = UIUtil.selectedId(table);
        if (id < 0) return;
        try {
            Enrollment en = dao.getById(id);
            if (en == null) return;
            adjusting = true;
            txtId.setText(String.valueOf(en.getEnrollmentId()));
            for (int i = 0; i < cbStudent.getItemCount(); i++) {
                if (cbStudent.getItemAt(i).getStudentId() == en.getStudentId()) {
                    cbStudent.setSelectedIndex(i);
                    break;
                }
            }
            loadSubjectsForStudent();
            for (int i = 0; i < cbSubject.getItemCount(); i++) {
                if (cbSubject.getItemAt(i).getSubjectId() == en.getSubjectId()) {
                    cbSubject.setSelectedIndex(i);
                    break;
                }
            }
            txtSchoolYear.setText(en.getSchoolYear());
            cbSemester.setSelectedItem(en.getSemester());
            cbStatus.setSelectedItem(en.getStatus());
            adjusting = false;
        } catch (SQLException ex) {
            adjusting = false;
            UIUtil.showSqlError(this, ex);
        }
    }

    private void clearForm() {
        txtId.setText("");
        txtSchoolYear.setText(Validator.currentSchoolYear());
        cbSemester.setSelectedIndex(0);
        cbStatus.setSelectedIndex(0);
        if (cbStudent.getItemCount() > 0) cbStudent.setSelectedIndex(0);
        table.clearSelection();
    }

    /** Checks student, school year and active status. Returns the student or null. */
    private Student validateCommon() {
        Student s = (Student) cbStudent.getSelectedItem();
        if (s == null) {
            UIUtil.error(this, "Please select a student.");
            return null;
        }
        if (!Validator.isValidSchoolYear(txtSchoolYear.getText())) {
            UIUtil.error(this, "School Year must look like 2026-2027 (second year = first year + 1).");
            return null;
        }
        if (!"ACTIVE".equals(s.getStatus())) {
            UIUtil.error(this, "Only ACTIVE students can be enrolled.");
            return null;
        }
        return s;
    }

    private void enroll() {
        Student s = validateCommon();
        if (s == null) return;
        Subject sb = (Subject) cbSubject.getSelectedItem();
        if (sb == null) {
            UIUtil.error(this, "Please select a subject.");
            return;
        }
        Enrollment en = new Enrollment();
        en.setStudentId(s.getStudentId());
        en.setSubjectId(sb.getSubjectId());
        en.setSchoolYear(txtSchoolYear.getText().trim());
        en.setSemester((Integer) cbSemester.getSelectedItem());
        en.setStatus("ENROLLED");
        try {
            dao.enroll(en);
            UIUtil.info(this, "Student enrolled successfully.");
            loadTable();
        } catch (SQLException ex) {
            if ("23505".equals(ex.getSQLState())) {
                UIUtil.error(this, "This student is already enrolled in that subject for the same school year and semester.");
            } else {
                UIUtil.showSqlError(this, ex);
            }
        }
    }

    private void enrollTerm() {
        Student s = validateCommon();
        if (s == null) return;
        int sem = (Integer) cbSemester.getSelectedItem();
        try {
            int n = dao.enrollTerm(s.getStudentId(), s.getCourseId(), s.getYearLevel(), sem, txtSchoolYear.getText().trim());
            if (n == 0) {
                UIUtil.error(this, "No new subjects were enrolled.\nEither no subjects exist for Year " + s.getYearLevel()
                        + ", Semester " + sem + " of this course, or the student is already enrolled in all of them.");
            } else {
                UIUtil.info(this, n + " subject(s) enrolled for Year " + s.getYearLevel() + ", Semester " + sem + ".");
            }
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void updateStatus() {
        if (Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "Select an enrollment from the table first.");
            return;
        }
        try {
            dao.updateStatus(Integer.parseInt(txtId.getText()), (String) cbStatus.getSelectedItem());
            UIUtil.info(this, "Enrollment status updated.");
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void delete() {
        if (Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "Select an enrollment from the table first.");
            return;
        }
        if (!UIUtil.confirm(this, "Are you sure you want to delete this record?\n"
                + "Its grade and attendance records will also be deleted.")) return;
        try {
            dao.delete(Integer.parseInt(txtId.getText()));
            UIUtil.info(this, "Enrollment deleted.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }
}
