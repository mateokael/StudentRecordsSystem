package ui;

import dao.CourseDAO;
import model.Course;
import util.Session;
import util.UIUtil;
import util.Validator;

import javax.swing.*;
import java.sql.SQLException;

public class CourseFrame extends BaseFrame {

    private final CourseDAO dao = new CourseDAO();
    private final JTextField txtId = UIUtil.field(6), txtCode = UIUtil.field(14), txtName = UIUtil.field(30),
            txtDesc = UIUtil.field(30), txtSearch = UIUtil.field(20);
    private final JTable table = UIUtil.createTable();

    public CourseFrame() {
        super("Course Management", "Manage degree programs / courses", 950, 600);

        txtId.setEditable(false);
        JPanel form = formPanel();
        UIUtil.addField(form, 0, 0, "Course ID:", txtId);
        UIUtil.addField(form, 1, 0, "Course Code:", txtCode);
        UIUtil.addField(form, 0, 1, "Course Name:", txtName);
        UIUtil.addField(form, 1, 1, "Description:", txtDesc);

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
        loadTable();
    }

    private void loadTable() {
        try {
            UIUtil.fillTable(table, CourseDAO.COLUMNS, dao.search(txtSearch.getText()));
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void fillForm() {
        int id = UIUtil.selectedId(table);
        if (id < 0) return;
        try {
            Course c = dao.getById(id);
            if (c == null) return;
            txtId.setText(String.valueOf(c.getCourseId()));
            txtCode.setText(c.getCourseCode());
            txtName.setText(c.getCourseName());
            txtDesc.setText(c.getDescription() == null ? "" : c.getDescription());
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void clearForm() {
        txtId.setText("");
        txtCode.setText("");
        txtName.setText("");
        txtDesc.setText("");
        table.clearSelection();
        txtCode.requestFocus();
    }

    private Course readForm() {
        if (Validator.isBlank(txtCode.getText()) || Validator.isBlank(txtName.getText())) {
            UIUtil.error(this, "Course Code and Course Name are required.");
            return null;
        }
        Course c = new Course();
        c.setCourseCode(txtCode.getText().trim().toUpperCase());
        c.setCourseName(txtName.getText().trim());
        c.setDescription(txtDesc.getText().trim());
        return c;
    }

    private void save() {
        Course c = readForm();
        if (c == null) return;
        try {
            dao.insert(c);
            UIUtil.info(this, "Course saved successfully.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void update() {
        if (Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "Select a course from the table first.");
            return;
        }
        Course c = readForm();
        if (c == null) return;
        c.setCourseId(Integer.parseInt(txtId.getText()));
        try {
            dao.update(c);
            UIUtil.info(this, "Course updated successfully.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void delete() {
        if (Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "Select a course from the table first.");
            return;
        }
        if (!UIUtil.confirm(this, "Are you sure you want to delete this record?")) return;
        try {
            dao.delete(Integer.parseInt(txtId.getText()));
            UIUtil.info(this, "Course deleted.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }
}
