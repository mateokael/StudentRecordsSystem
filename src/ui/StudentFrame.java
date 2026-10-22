package ui;

import dao.CourseDAO;
import dao.StudentDAO;
import model.Course;
import model.Student;
import util.Session;
import util.UIUtil;
import util.Validator;

import javax.swing.*;
import java.sql.Date;
import java.sql.SQLException;

/** Student registration and records. */
public class StudentFrame extends BaseFrame {

    private final StudentDAO dao = new StudentDAO();
    private final CourseDAO courseDao = new CourseDAO();

    private final JTextField txtId = UIUtil.field(6), txtNo = UIUtil.field(14), txtFirst = UIUtil.field(18),
            txtLast = UIUtil.field(18), txtBirth = UIUtil.field(12), txtContact = UIUtil.field(14),
            txtEmail = UIUtil.field(22), txtAddress = UIUtil.field(28), txtSearch = UIUtil.field(20);
    private final JComboBox<String> cbGender = new JComboBox<>(new String[]{"Male", "Female", "Other"});
    private final JComboBox<Course> cbCourse = new JComboBox<>();
    private final JComboBox<Integer> cbYear = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE", "GRADUATED"});
    private final JTable table = UIUtil.createTable();

    /** Opens the screen with the given student already selected (used by the dashboard). */
    public StudentFrame(int studentId) {
        this();
        selectStudent(studentId);
    }

    public StudentFrame() {
        super("Student Registration", "Register students and manage their personal and academic information", 1150, 720);

        txtId.setEditable(false);
        txtNo.setEditable(false);   // Student No. is generated automatically
        txtBirth.setToolTipText("Format: yyyy-MM-dd (example 2007-03-14)");

        JPanel form = formPanel();
        UIUtil.addField(form, 0, 0, "Student ID:", txtId);
        UIUtil.addField(form, 1, 0, "Student No.:", txtNo);
        UIUtil.addField(form, 2, 0, "Status:", cbStatus);
        UIUtil.addField(form, 0, 1, "First Name:", txtFirst);
        UIUtil.addField(form, 1, 1, "Last Name:", txtLast);
        UIUtil.addField(form, 2, 1, "Gender:", cbGender);
        UIUtil.addField(form, 0, 2, "Birth Date:", txtBirth);
        UIUtil.addField(form, 1, 2, "Contact No.:", txtContact);
        UIUtil.addField(form, 2, 2, "Email:", txtEmail);
        UIUtil.addField(form, 0, 3, "Course:", cbCourse);
        UIUtil.addField(form, 1, 3, "Year Level:", cbYear);
        UIUtil.addField(form, 2, 3, "Address:", txtAddress);

        JButton btnSave = UIUtil.button("SAVE", UIUtil.SUCCESS);
        JButton btnUpdate = UIUtil.button("UPDATE", UIUtil.PRIMARY);
        JButton btnDelete = UIUtil.button("DELETE", UIUtil.DANGER);
        JButton btnClear = UIUtil.button("CLEAR", UIUtil.NEUTRAL);
        JButton btnSearch = UIUtil.button("SEARCH", UIUtil.PRIMARY);
        JButton btnRefresh = UIUtil.button("REFRESH", UIUtil.NEUTRAL);

        layoutCrud(form, buttonBar(btnSave, btnUpdate, btnDelete, btnClear),
                searchBar(txtSearch, btnSearch, btnRefresh), table);

        if (!Session.canManageRecords()) disable(btnSave, btnUpdate, btnDelete);

        btnSave.addActionListener(e -> save());
        btnUpdate.addActionListener(e -> update());
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

        loadCourses();
        loadTable();
        showNextStudentNo();
    }

    /** Selects the table row with this student ID, which fills the form. */
    private void selectStudent(int studentId) {
        for (int i = 0; i < table.getRowCount(); i++) {
            if (String.valueOf(table.getValueAt(i, 0)).equals(String.valueOf(studentId))) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                return;
            }
        }
    }

    /** Puts the next auto-generated student number in the Student No. field. */
    private void showNextStudentNo() {
        try {
            txtNo.setText(dao.generateStudentNo());
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void loadCourses() {
        try {
            cbCourse.removeAllItems();
            for (Course c : courseDao.getAll()) cbCourse.addItem(c);
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void loadTable() {
        try {
            UIUtil.fillTable(table, StudentDAO.COLUMNS, dao.search(txtSearch.getText()));
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void fillForm() {
        int id = UIUtil.selectedId(table);
        if (id < 0) return;
        try {
            Student s = dao.getById(id);
            if (s == null) return;
            txtId.setText(String.valueOf(s.getStudentId()));
            txtNo.setText(s.getStudentNo());
            txtFirst.setText(s.getFirstName());
            txtLast.setText(s.getLastName());
            cbGender.setSelectedItem(s.getGender());
            txtBirth.setText(s.getBirthDate() == null ? "" : s.getBirthDate().toString());
            txtContact.setText(s.getContactNo() == null ? "" : s.getContactNo());
            txtEmail.setText(s.getEmail() == null ? "" : s.getEmail());
            txtAddress.setText(s.getAddress() == null ? "" : s.getAddress());
            for (int i = 0; i < cbCourse.getItemCount(); i++) {
                if (cbCourse.getItemAt(i).getCourseId() == s.getCourseId()) {
                    cbCourse.setSelectedIndex(i);
                    break;
                }
            }
            cbYear.setSelectedItem(s.getYearLevel());
            cbStatus.setSelectedItem(s.getStatus());
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void clearForm() {
        txtId.setText("");
        txtFirst.setText("");
        txtLast.setText("");
        txtBirth.setText("");
        txtContact.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        cbGender.setSelectedIndex(0);
        if (cbCourse.getItemCount() > 0) cbCourse.setSelectedIndex(0);
        cbYear.setSelectedIndex(0);
        cbStatus.setSelectedIndex(0);
        table.clearSelection();
        showNextStudentNo();
        txtFirst.requestFocus();
    }

    private Student readForm() {
        String no = txtNo.getText().trim();
        String first = txtFirst.getText().trim();
        String last = txtLast.getText().trim();

        if (Validator.isBlank(no) || Validator.isBlank(first) || Validator.isBlank(last)) {
            UIUtil.error(this, "Student No., First Name and Last Name are required.");
            return null;
        }
        if (cbCourse.getSelectedItem() == null) {
            UIUtil.error(this, "Please add a course first (Courses module), then select it here.");
            return null;
        }
        Date birth = null;
        if (!Validator.isBlank(txtBirth.getText())) {
            birth = Validator.parseDate(txtBirth.getText());
            if (birth == null) {
                UIUtil.error(this, "Birth Date must be a valid date in the format yyyy-MM-dd.");
                return null;
            }
            if (birth.after(new java.util.Date())) {
                UIUtil.error(this, "Birth Date cannot be in the future.");
                return null;
            }
        }
        if (!Validator.isBlank(txtContact.getText()) && !Validator.isValidPhone(txtContact.getText())) {
            UIUtil.error(this, "Contact No. may only contain digits, spaces, + - ( ) and be 7-20 characters long.");
            return null;
        }
        if (!Validator.isBlank(txtEmail.getText()) && !Validator.isValidEmail(txtEmail.getText())) {
            UIUtil.error(this, "Please enter a valid email address.");
            return null;
        }

        Student s = new Student();
        s.setStudentNo(no);
        s.setFirstName(first);
        s.setLastName(last);
        s.setGender((String) cbGender.getSelectedItem());
        s.setBirthDate(birth);
        s.setContactNo(txtContact.getText().trim());
        s.setEmail(txtEmail.getText().trim());
        s.setAddress(txtAddress.getText().trim());
        s.setCourseId(((Course) cbCourse.getSelectedItem()).getCourseId());
        s.setYearLevel((Integer) cbYear.getSelectedItem());
        s.setStatus((String) cbStatus.getSelectedItem());
        return s;
    }

    private void save() {
        // A row selected in the table means the form holds an existing student; don't insert a duplicate.
        if (!Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "This student already exists. Click CLEAR to register a new one, or UPDATE to save changes.");
            return;
        }
        // Regenerate right before saving so two people registering at once don't collide.
        showNextStudentNo();
        Student s = readForm();
        if (s == null) return;
        try {
            dao.insert(s);
            UIUtil.info(this, "Student registered successfully. Student No.: " + s.getStudentNo());
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void update() {
        if (Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "Select a student from the table first.");
            return;
        }
        Student s = readForm();
        if (s == null) return;
        s.setStudentId(Integer.parseInt(txtId.getText()));
        try {
            dao.update(s);
            UIUtil.info(this, "Student record updated successfully.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void delete() {
        if (Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "Select a student from the table first.");
            return;
        }
        if (!UIUtil.confirm(this, "Are you sure you want to delete this record?")) return;
        try {
            dao.delete(Integer.parseInt(txtId.getText()));
            UIUtil.info(this, "Student deleted.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }
}