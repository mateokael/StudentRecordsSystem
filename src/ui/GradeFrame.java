package ui;

import dao.GradeDAO;
import model.Grade;
import util.GradeUtil;
import util.Session;
import util.UIUtil;
import util.Validator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.sql.SQLException;

/** Encode grades. Final grade = average of Prelim, Midterm and Finals; 75 and above passes. */
public class GradeFrame extends BaseFrame {

    private final GradeDAO dao = new GradeDAO();

    private final JTextField txtEnrId = UIUtil.field(6), txtStudent = UIUtil.field(22), txtSubject = UIUtil.field(18),
            txtPrelim = UIUtil.field(8), txtMidterm = UIUtil.field(8), txtFinals = UIUtil.field(8),
            txtSearch = UIUtil.field(20);
    private final JLabel lblComputed = new JLabel("Final Grade: -   Equivalent: -   Remarks: -");
    private final JTable table = UIUtil.createTable();

    public GradeFrame() {
        super("Grade Management", "Select an enrollment below, then enter Prelim, Midterm and Finals (0-100)", 1150, 700);

        txtEnrId.setEditable(false);
        txtStudent.setEditable(false);
        txtSubject.setEditable(false);
        lblComputed.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        lblComputed.setForeground(UIUtil.PRIMARY);

        JPanel form = formPanel();
        UIUtil.addField(form, 0, 0, "Enrollment ID:", txtEnrId);
        UIUtil.addField(form, 1, 0, "Student:", txtStudent);
        UIUtil.addField(form, 2, 0, "Subject:", txtSubject);
        UIUtil.addField(form, 0, 1, "Prelim:", txtPrelim);
        UIUtil.addField(form, 1, 1, "Midterm:", txtMidterm);
        UIUtil.addField(form, 2, 1, "Finals:", txtFinals);
        form.add(lblComputed, new java.awt.GridBagConstraints(0, 2, 6, 1, 0, 0, java.awt.GridBagConstraints.WEST,
                java.awt.GridBagConstraints.NONE, new java.awt.Insets(6, 8, 6, 8), 0, 0));

        JButton btnSave = UIUtil.button("SAVE GRADE", UIUtil.SUCCESS);
        JButton btnDelete = UIUtil.button("DELETE GRADE", UIUtil.DANGER);
        JButton btnClear = UIUtil.button("CLEAR", UIUtil.NEUTRAL);
        JButton btnSearch = UIUtil.button("SEARCH", UIUtil.PRIMARY);
        JButton btnRefresh = UIUtil.button("REFRESH", UIUtil.NEUTRAL);

        layoutCrud(form, buttonBar(btnSave, btnDelete, btnClear), searchBar(txtSearch, btnSearch, btnRefresh), table);

        if (!Session.canEncodeGrades()) disable(btnSave, btnDelete);

        liveCompute(txtPrelim);
        liveCompute(txtMidterm);
        liveCompute(txtFinals);

        btnSave.addActionListener(e -> save());
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

    private void liveCompute(JTextField f) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateComputed(); }
            public void removeUpdate(DocumentEvent e) { updateComputed(); }
            public void changedUpdate(DocumentEvent e) { updateComputed(); }
        });
    }

    private void updateComputed() {
        try {
            Double p = GradeUtil.parseScore(txtPrelim.getText(), "Prelim");
            Double m = GradeUtil.parseScore(txtMidterm.getText(), "Midterm");
            Double f = GradeUtil.parseScore(txtFinals.getText(), "Finals");
            Double fin = GradeUtil.computeFinal(p, m, f);
            lblComputed.setText("Final Grade: " + (fin == null ? "-" : String.format("%.2f", fin))
                    + "   Equivalent: " + GradeUtil.equivalent(fin) + "   Remarks: " + GradeUtil.remarks(fin));
        } catch (IllegalArgumentException ex) {
            lblComputed.setText("Final Grade: -   (" + ex.getMessage() + ")");
        }
    }

    private void loadTable() {
        try {
            UIUtil.fillTable(table, GradeDAO.COLUMNS, dao.search(txtSearch.getText()));
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private static String fmt(Double d) {
        return d == null ? "" : String.valueOf(d);
    }

    private void fillForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int enrId = UIUtil.selectedId(table);
        txtEnrId.setText(String.valueOf(enrId));
        txtStudent.setText(String.valueOf(table.getValueAt(row, 2)));
        txtSubject.setText(String.valueOf(table.getValueAt(row, 3)));
        try {
            Grade g = dao.getByEnrollment(enrId);
            txtPrelim.setText(g == null ? "" : fmt(g.getPrelim()));
            txtMidterm.setText(g == null ? "" : fmt(g.getMidterm()));
            txtFinals.setText(g == null ? "" : fmt(g.getFinals()));
            updateComputed();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void clearForm() {
        txtEnrId.setText("");
        txtStudent.setText("");
        txtSubject.setText("");
        txtPrelim.setText("");
        txtMidterm.setText("");
        txtFinals.setText("");
        table.clearSelection();
        updateComputed();
    }

    private void save() {
        if (Validator.isBlank(txtEnrId.getText())) {
            UIUtil.error(this, "Select an enrollment from the table first.");
            return;
        }
        Grade g = new Grade();
        try {
            g.setPrelim(GradeUtil.parseScore(txtPrelim.getText(), "Prelim"));
            g.setMidterm(GradeUtil.parseScore(txtMidterm.getText(), "Midterm"));
            g.setFinals(GradeUtil.parseScore(txtFinals.getText(), "Finals"));
        } catch (IllegalArgumentException ex) {
            UIUtil.error(this, ex.getMessage());
            return;
        }
        if (g.getPrelim() == null && g.getMidterm() == null && g.getFinals() == null) {
            UIUtil.error(this, "Enter at least one score (Prelim, Midterm or Finals).");
            return;
        }
        g.setEnrollmentId(Integer.parseInt(txtEnrId.getText()));
        try {
            dao.save(g);
            UIUtil.info(this, "Grade saved. Final grade: "
                    + (g.getFinalGrade() == null ? "incomplete (needs all three scores)" : g.getFinalGrade())
                    + "  Remarks: " + g.getRemarks());
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    private void delete() {
        if (Validator.isBlank(txtEnrId.getText())) {
            UIUtil.error(this, "Select an enrollment from the table first.");
            return;
        }
        if (!UIUtil.confirm(this, "Are you sure you want to delete this record?")) return;
        try {
            dao.delete(Integer.parseInt(txtEnrId.getText()));
            UIUtil.info(this, "Grade deleted.");
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }
}
