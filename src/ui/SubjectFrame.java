package ui;

import dao.CourseDAO;
import dao.SubjectDAO;
import model.Course;
import model.Subject;
import util.Session;
import util.UIUtil;
import util.Validator;

import javax.swing.*;
import java.sql.SQLException;

public class SubjectFrame extends BaseFrame {

    private final SubjectDAO dao = new SubjectDAO();
    private final CourseDAO courseDao = new CourseDAO();

    private final JTextField txtId = UIUtil.field(6), txtCode = UIUtil.field(14), txtName = UIUtil.field(28),
            txtUnits = UIUtil.field(6), txtSearch = UIUtil.field(20);
    private final JComboBox<Course> cbCourse = new JComboBox<>();
    private final JComboBox<Integer> cbYear = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
    private final JComboBox<Integer> cbSemester = new JComboBox<>(new Integer[]{1, 2, 3});
    private final JTable table = UIUtil.createTable();

    public SubjectFrame() {
        super("Subject Management", "Manage subjects offered per course, year level and semester (3 = Summer)", 1050, 650);

        txtId.setEditable(false);
        JPanel form = formPanel();
        UIUtil.addField(form, 0, 0, "Subject ID:", txtId);
        UIUtil.addField(form, 1, 0, "Subject Code:", txtCode);
        UIUtil.addField(form, 2, 0, "Units:", txtUnits);
        UIUtil.addField(form, 0, 1, "Subject Name:", txtName);
        UIUtil.addField(form, 1, 1, "Course:", cbCourse);
        UIUtil.addField(form, 0, 2, "Year Level:", cbYear);
        UIUtil.addField(form, 1, 2, "Semester:", cbSemester);

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

        try {
            for (Course c : courseDao.getAll()) cbCourse.addItem(c);
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
        loadTable();
    }

    private void loadTable() {
        try {
            UIUtil.fillTable(table, SubjectDAO.COLUMNS, dao.search(txtSearch.getText()));
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void fillForm() {
        int id = UIUtil.selectedId(table);
        if (id < 0) return;
        try {
            Subject s = dao.getById(id);
            if (s == null) return;
            txtId.setText(String.valueOf(s.getSubjectId()));
            txtCode.setText(s.getSubjectCode());
            txtName.setText(s.getSubjectName());
            txtUnits.setText(String.valueOf(s.getUnits()));
            for (int i = 0; i < cbCourse.getItemCount(); i++) {
                if (cbCourse.getItemAt(i).getCourseId() == s.getCourseId()) {
                    cbCourse.setSelectedIndex(i);
                    break;
                }
            }
            cbYear.setSelectedItem(s.getYearLevel());
            cbSemester.setSelectedItem(s.getSemester());
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void clearForm() {
        txtId.setText("");
        txtCode.setText("");
        txtName.setText("");
        txtUnits.setText("");
        if (cbCourse.getItemCount() > 0) cbCourse.setSelectedIndex(0);
        cbYear.setSelectedIndex(0);
        cbSemester.setSelectedIndex(0);
        table.clearSelection();
        txtCode.requestFocus();
    }

    private Subject readForm() {
        if (Validator.isBlank(txtCode.getText()) || Validator.isBlank(txtName.getText())
                || Validator.isBlank(txtUnits.getText())) {
            UIUtil.error(this, "Subject Code, Subject Name and Units are required.");
            return null;
        }
        if (!Validator.isInteger(txtUnits.getText())) {
            UIUtil.error(this, "Units must be a whole number.");
            return null;
        }
        int units = Integer.parseInt(txtUnits.getText().trim());
        if (units < 1 || units > 12) {
            UIUtil.error(this, "Units must be between 1 and 12.");
            return null;
        }
        if (cbCourse.getSelectedItem() == null) {
            UIUtil.error(this, "Please add a course first, then select it here.");
            return null;
        }
        Subject s = new Subject();
        s.setSubjectCode(txtCode.getText().trim().toUpperCase());
        s.setSubjectName(txtName.getText().trim());
        s.setUnits(units);
        s.setCourseId(((Course) cbCourse.getSelectedItem()).getCourseId());
        s.setYearLevel((Integer) cbYear.getSelectedItem());
        s.setSemester((Integer) cbSemester.getSelectedItem());
        return s;
    }

    private void save() {
        Subject s = readForm();
        if (s == null) return;
        try {
            dao.insert(s);
            UIUtil.info(this, "Subject saved successfully.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void update() {
        if (Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "Select a subject from the table first.");
            return;
        }
        Subject s = readForm();
        if (s == null) return;
        s.setSubjectId(Integer.parseInt(txtId.getText()));
        try {
            dao.update(s);
            UIUtil.info(this, "Subject updated successfully.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void delete() {
        if (Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "Select a subject from the table first.");
            return;
        }
        if (!UIUtil.confirm(this, "Are you sure you want to delete this record?")) return;
        try {
            dao.delete(Integer.parseInt(txtId.getText()));
            UIUtil.info(this, "Subject deleted.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }
}
