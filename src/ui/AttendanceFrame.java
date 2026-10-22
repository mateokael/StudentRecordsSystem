package ui;

import dao.AttendanceDAO;
import dao.EnrollmentDAO;
import model.Attendance;
import util.Session;
import util.UIUtil;
import util.Validator;

import javax.swing.*;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;

/** Daily attendance per enrollment (student + subject). */
public class AttendanceFrame extends BaseFrame {

    private final AttendanceDAO dao = new AttendanceDAO();
    private final EnrollmentDAO enrollmentDao = new EnrollmentDAO();

    private final JTable tblEnr = UIUtil.createTable();
    private final JTable tblAtt = UIUtil.createTable();
    private final JTextField txtSearch = UIUtil.field(20), txtAttId = UIUtil.field(6),
            txtDate = UIUtil.field(12), txtRemarks = UIUtil.field(26);
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"PRESENT", "ABSENT", "LATE", "EXCUSED"});
    private final JLabel lblSelected = new JLabel("No enrollment selected");
    private final JLabel lblSummary = new JLabel(" ");
    private final JButton btnAdd = UIUtil.button("ADD", UIUtil.SUCCESS);
    private final JButton btnUpdate = UIUtil.button("UPDATE", UIUtil.PRIMARY);
    private final JButton btnDelete = UIUtil.button("DELETE", UIUtil.DANGER);
    private final JButton btnClear = UIUtil.button("CLEAR", UIUtil.NEUTRAL);

    public AttendanceFrame() {
        super("Attendance", "Select a student's enrollment, then record attendance for a date", 1200, 780);

        txtAttId.setEditable(false);
        txtDate.setText(LocalDate.now().toString());
        txtDate.setToolTipText("Format: yyyy-MM-dd");
        lblSelected.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSelected.setForeground(UIUtil.PRIMARY);
        lblSummary.setFont(UIUtil.BOLD);

        // top: enrollments
        JButton btnSearch = UIUtil.button("SEARCH", UIUtil.PRIMARY);
        JButton btnRefresh = UIUtil.button("REFRESH", UIUtil.NEUTRAL);
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(searchBar(txtSearch, btnSearch, btnRefresh), BorderLayout.NORTH);
        top.add(new JScrollPane(tblEnr), BorderLayout.CENTER);

        // bottom: attendance form + records
        JPanel form = formPanel();
        UIUtil.addField(form, 0, 0, "Record ID:", txtAttId);
        UIUtil.addField(form, 1, 0, "Date:", txtDate);
        UIUtil.addField(form, 2, 0, "Status:", cbStatus);
        UIUtil.addField(form, 0, 1, "Remarks:", txtRemarks);

        JPanel formBox = new JPanel(new BorderLayout());
        formBox.setBackground(Color.WHITE);
        JPanel labels = new JPanel(new GridLayout(2, 1));
        labels.setBackground(Color.WHITE);
        labels.setBorder(BorderFactory.createEmptyBorder(4, 10, 0, 10));
        labels.add(lblSelected);
        labels.add(lblSummary);
        formBox.add(labels, BorderLayout.NORTH);
        formBox.add(form, BorderLayout.CENTER);
        formBox.add(buttonBar(btnAdd, btnUpdate, btnDelete, btnClear), BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(titled("Attendance Record", formBox), BorderLayout.NORTH);
        bottom.add(new JScrollPane(tblAtt), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.42);
        split.setOpaque(false);
        split.setBorder(null);
        content.add(split, BorderLayout.CENTER);

        if (!Session.canEncodeGrades()) disable(btnAdd, btnUpdate, btnDelete);

        btnSearch.addActionListener(e -> loadEnrollments());
        txtSearch.addActionListener(e -> loadEnrollments());
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            loadEnrollments();
        });
        btnAdd.addActionListener(e -> add());
        btnUpdate.addActionListener(e -> update());
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clearForm());
        tblEnr.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) enrollmentSelected();
        });
        tblAtt.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) attendanceSelected();
        });

        loadEnrollments();
    }

    private void loadEnrollments() {
        try {
            UIUtil.fillTable(tblEnr, EnrollmentDAO.COLUMNS, enrollmentDao.search(txtSearch.getText()));
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private int enrollmentId() {
        return UIUtil.selectedId(tblEnr);
    }

    private void enrollmentSelected() {
        int row = tblEnr.getSelectedRow();
        if (row < 0) return;
        lblSelected.setText("Selected: " + tblEnr.getValueAt(row, 2) + "  |  " + tblEnr.getValueAt(row, 3)
                + " - " + tblEnr.getValueAt(row, 4) + "  |  SY " + tblEnr.getValueAt(row, 6) + ", Sem "
                + tblEnr.getValueAt(row, 7));
        clearForm();
        loadAttendance();
    }

    private void loadAttendance() {
        int enrId = enrollmentId();
        if (enrId < 0) return;
        try {
            UIUtil.fillTable(tblAtt, AttendanceDAO.COLUMNS, dao.listByEnrollment(enrId));
            int[] s = dao.summary(enrId);
            lblSummary.setText("Present: " + s[0] + "   Absent: " + s[1] + "   Late: " + s[2] + "   Excused: " + s[3]);
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void attendanceSelected() {
        int id = UIUtil.selectedId(tblAtt);
        if (id < 0) return;
        try {
            Attendance a = dao.getById(id);
            if (a == null) return;
            txtAttId.setText(String.valueOf(a.getAttendanceId()));
            txtDate.setText(a.getAttendanceDate().toString());
            cbStatus.setSelectedItem(a.getStatus());
            txtRemarks.setText(a.getRemarks() == null ? "" : a.getRemarks());
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void clearForm() {
        txtAttId.setText("");
        txtDate.setText(LocalDate.now().toString());
        cbStatus.setSelectedIndex(0);
        txtRemarks.setText("");
        tblAtt.clearSelection();
    }

    private Attendance readForm() {
        if (enrollmentId() < 0) {
            UIUtil.error(this, "Select an enrollment from the top table first.");
            return null;
        }
        Date d = Validator.parseDate(txtDate.getText());
        if (d == null) {
            UIUtil.error(this, "Date must be a valid date in the format yyyy-MM-dd.");
            return null;
        }
        if (d.toLocalDate().isAfter(LocalDate.now())) {
            UIUtil.error(this, "Attendance cannot be recorded for a future date.");
            return null;
        }
        Attendance a = new Attendance();
        a.setEnrollmentId(enrollmentId());
        a.setAttendanceDate(d);
        a.setStatus((String) cbStatus.getSelectedItem());
        a.setRemarks(txtRemarks.getText().trim());
        return a;
    }

    private void handleSql(SQLException ex) {
        if ("23505".equals(ex.getSQLState())) {
            UIUtil.error(this, "Attendance for that date is already recorded. Select it in the table and use UPDATE.");
        } else {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void add() {
        Attendance a = readForm();
        if (a == null) return;
        try {
            dao.insert(a);
            UIUtil.info(this, "Attendance recorded.");
            clearForm();
            loadAttendance();
        } catch (SQLException ex) {
            handleSql(ex);
        }
    }

    private void update() {
        if (Validator.isBlank(txtAttId.getText())) {
            UIUtil.error(this, "Select an attendance record from the bottom table first.");
            return;
        }
        Attendance a = readForm();
        if (a == null) return;
        a.setAttendanceId(Integer.parseInt(txtAttId.getText()));
        try {
            dao.update(a);
            UIUtil.info(this, "Attendance updated.");
            clearForm();
            loadAttendance();
        } catch (SQLException ex) {
            handleSql(ex);
        }
    }

    private void delete() {
        if (Validator.isBlank(txtAttId.getText())) {
            UIUtil.error(this, "Select an attendance record from the bottom table first.");
            return;
        }
        if (!UIUtil.confirm(this, "Are you sure you want to delete this record?")) return;
        try {
            dao.delete(Integer.parseInt(txtAttId.getText()));
            UIUtil.info(this, "Attendance record deleted.");
            clearForm();
            loadAttendance();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }
}
