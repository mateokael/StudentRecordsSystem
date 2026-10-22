package ui;

import dao.StudentDAO;
import dao.UserDAO;
import model.Student;
import model.User;
import util.Session;
import util.UIUtil;
import util.Validator;

import javax.swing.*;
import java.sql.SQLException;

/** User management (ADMIN only). STUDENT accounts must be linked to a registered student. */
public class UserFrame extends BaseFrame {

    private final UserDAO dao = new UserDAO();
    private final StudentDAO studentDao = new StudentDAO();

    private final JTextField txtId = UIUtil.field(6), txtUsername = UIUtil.field(18), txtFullName = UIUtil.field(22),
            txtSearch = UIUtil.field(20);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JComboBox<String> cbRole = new JComboBox<>(new String[]{"ADMIN", "REGISTRAR", "TEACHER", "STUDENT"});
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});
    private final JComboBox<Student> cbStudent = new JComboBox<>();
    private final JTable table = UIUtil.createTable();

    public UserFrame() {
        super("User Management", "Create and manage system accounts (Administrator only)", 1000, 680);

        txtId.setEditable(false);
        JPanel form = formPanel();
        UIUtil.addField(form, 0, 0, "User ID:", txtId);
        UIUtil.addField(form, 1, 0, "Full Name:", txtFullName);
        UIUtil.addField(form, 0, 1, "Username:", txtUsername);
        UIUtil.addField(form, 1, 1, "Password:", txtPassword);
        UIUtil.addField(form, 0, 2, "Role:", cbRole);
        UIUtil.addField(form, 1, 2, "Status:", cbStatus);
        UIUtil.addField(form, 0, 3, "Linked Student:", cbStudent);
        form.add(UIUtil.label("(Linked Student is only for the STUDENT role. When updating, leave Password blank to keep it.)"),
                new java.awt.GridBagConstraints(0, 4, 4, 1, 0, 0, java.awt.GridBagConstraints.WEST,
                        java.awt.GridBagConstraints.NONE, new java.awt.Insets(0, 8, 4, 8), 0, 0));

        JButton btnSave = UIUtil.button("SAVE", UIUtil.SUCCESS);
        JButton btnUpdate = UIUtil.button("UPDATE", UIUtil.PRIMARY);
        JButton btnDelete = UIUtil.button("DELETE", UIUtil.DANGER);
        JButton btnClear = UIUtil.button("CLEAR", UIUtil.NEUTRAL);
        JButton btnSearch = UIUtil.button("SEARCH", UIUtil.PRIMARY);
        JButton btnRefresh = UIUtil.button("REFRESH", UIUtil.NEUTRAL);

        layoutCrud(form, buttonBar(btnSave, btnUpdate, btnDelete, btnClear),
                searchBar(txtSearch, btnSearch, btnRefresh), table);

        try {
            cbStudent.addItem(null);   // placeholder so no student is linked by accident
            for (Student s : studentDao.getAll()) cbStudent.addItem(s);
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
        cbStudent.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                   boolean sel, boolean focus) {
                return super.getListCellRendererComponent(list,
                        value == null ? "-- select a registered student --" : value, index, sel, focus);
            }
        });
        cbRole.addActionListener(e -> cbStudent.setEnabled("STUDENT".equals(cbRole.getSelectedItem())));
        cbStudent.setEnabled(false);

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
            UIUtil.fillTable(table, UserDAO.COLUMNS, dao.search(txtSearch.getText()));
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void fillForm() {
        int id = UIUtil.selectedId(table);
        if (id < 0) return;
        try {
            User u = dao.getById(id);
            if (u == null) return;
            txtId.setText(String.valueOf(u.getUserId()));
            txtUsername.setText(u.getUsername());
            txtFullName.setText(u.getFullName());
            txtPassword.setText("");
            cbRole.setSelectedItem(u.getRole());
            cbStatus.setSelectedItem(u.getStatus());
            cbStudent.setEnabled("STUDENT".equals(u.getRole()));
            cbStudent.setSelectedIndex(0);
            if (u.getStudentId() != null) {
                for (int i = 1; i < cbStudent.getItemCount(); i++) {
                    if (cbStudent.getItemAt(i).getStudentId() == u.getStudentId()) {
                        cbStudent.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void clearForm() {
        txtId.setText("");
        txtUsername.setText("");
        txtFullName.setText("");
        txtPassword.setText("");
        cbRole.setSelectedIndex(0);
        cbStatus.setSelectedIndex(0);
        if (cbStudent.getItemCount() > 0) cbStudent.setSelectedIndex(0);
        cbStudent.setEnabled(false);
        table.clearSelection();
        txtUsername.requestFocus();
    }

    /** Validates the form; returns null (after showing a message) if something is wrong. */
    private User readForm(boolean passwordRequired) {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String role = (String) cbRole.getSelectedItem();
        Student linked = null;

        if ("STUDENT".equals(role)) {
            linked = (Student) cbStudent.getSelectedItem();
            if (linked == null) {
                UIUtil.error(this, "A STUDENT account must be linked to a registered student.\n"
                        + "Register the student first in the Students module.");
                return null;
            }
            if (Validator.isBlank(fullName)) fullName = linked.getFirstName() + " " + linked.getLastName();
            else if (!fullName.equalsIgnoreCase(linked.getFirstName() + " " + linked.getLastName())
                    && !UIUtil.confirm(this, "The Full Name \"" + fullName + "\" does not match the linked student\n"
                            + linked + ".\nThis account will show that student's records. Continue?")) {
                return null;
            }
        }
        if (Validator.isBlank(username) || Validator.isBlank(fullName)) {
            UIUtil.error(this, "Username and Full Name are required.");
            return null;
        }
        if (username.length() < 4 || username.contains(" ")) {
            UIUtil.error(this, "Username must be at least 4 characters and contain no spaces.");
            return null;
        }
        if (passwordRequired && pass.isEmpty()) {
            UIUtil.error(this, "Password is required for a new user.");
            return null;
        }
        if (!pass.isEmpty() && pass.length() < 6) {
            UIUtil.error(this, "Password must be at least 6 characters.");
            return null;
        }
        User u = new User();
        u.setUsername(username);
        u.setFullName(fullName);
        u.setRole(role);
        u.setStatus((String) cbStatus.getSelectedItem());
        u.setStudentId(linked == null ? null : Integer.valueOf(linked.getStudentId()));
        return u;
    }

    private void handleSql(SQLException ex) {
        if ("23505".equals(ex.getSQLState())) {
            UIUtil.error(this, "Duplicate: that username already exists, or that student already has an account.");
        } else {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void save() {
        User u = readForm(true);
        if (u == null) return;
        try {
            dao.insert(u, new String(txtPassword.getPassword()));
            UIUtil.info(this, "User saved successfully.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            handleSql(ex);
        }
    }

    private void update() {
        if (Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "Select a user from the table first.");
            return;
        }
        User u = readForm(false);
        if (u == null) return;
        u.setUserId(Integer.parseInt(txtId.getText()));
        if (u.getUserId() == Session.getUser().getUserId()
                && ("INACTIVE".equals(u.getStatus()) || !"ADMIN".equals(u.getRole()))) {
            UIUtil.error(this, "You cannot deactivate or change the role of your own account.");
            return;
        }
        try {
            dao.update(u, new String(txtPassword.getPassword()));
            UIUtil.info(this, "User updated successfully.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            handleSql(ex);
        }
    }

    private void delete() {
        if (Validator.isBlank(txtId.getText())) {
            UIUtil.error(this, "Select a user from the table first.");
            return;
        }
        int id = Integer.parseInt(txtId.getText());
        if (id == Session.getUser().getUserId()) {
            UIUtil.error(this, "You cannot delete the account you are logged in with.");
            return;
        }
        if (!UIUtil.confirm(this, "Are you sure you want to delete this record?")) return;
        try {
            dao.delete(id);
            UIUtil.info(this, "User deleted.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }
}
