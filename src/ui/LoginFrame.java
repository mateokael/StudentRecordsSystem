package ui;

import dao.UserDAO;
import database.DatabaseConnection;
import model.User;
import util.Session;
import util.UIUtil;
import util.Validator;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class LoginFrame extends JFrame {

    private final JTextField txtUser = UIUtil.field(18);
    private final JPasswordField txtPass = new JPasswordField(18);
    private final UserDAO userDao = new UserDAO();

    public LoginFrame() {
        super("Student Records System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setLayout(new BorderLayout());

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(UIUtil.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel t = new JLabel("SYSTEM LOGIN", SwingConstants.CENTER);
        t.setFont(UIUtil.TITLE);
        t.setForeground(Color.WHITE);
        JLabel s = new JLabel("Student Enrollment, Grading and Record Management System", SwingConstants.CENTER);
        s.setFont(UIUtil.FONT);
        s.setForeground(new Color(200, 218, 240));
        header.add(t);
        header.add(s);

        txtPass.setFont(UIUtil.FONT);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIUtil.BG);
        form.setBorder(BorderFactory.createEmptyBorder(25, 30, 10, 30));
        UIUtil.addField(form, 0, 0, "Username:", txtUser);
        UIUtil.addField(form, 0, 1, "Password:", txtPass);

        JButton btnLogin = UIUtil.button("LOGIN", UIUtil.PRIMARY);
        JButton btnClear = UIUtil.button("CLEAR", UIUtil.NEUTRAL);
        JButton btnExit = UIUtil.button("EXIT", UIUtil.DANGER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttons.setBackground(UIUtil.BG);
        buttons.add(btnLogin);
        buttons.add(btnClear);
        buttons.add(btnExit);

        JLabel hint = new JLabel("Database: " + DatabaseConnection.getMode(), SwingConstants.CENTER);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(UIUtil.NEUTRAL);
        hint.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UIUtil.BG);
        center.add(form, BorderLayout.NORTH);
        center.add(buttons, BorderLayout.CENTER);
        center.add(hint, BorderLayout.SOUTH);
        center.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);

        btnLogin.addActionListener(e -> doLogin());
        btnClear.addActionListener(e -> {
            txtUser.setText("");
            txtPass.setText("");
            txtUser.requestFocus();
        });
        btnExit.addActionListener(e -> {
            DatabaseConnection.shutdown();
            System.exit(0);
        });
        getRootPane().setDefaultButton(btnLogin);

        // Size the window to fit its contents (so nothing is cut off), with a sensible minimum.
        pack();
        Dimension d = getSize();
        setSize(Math.max(d.width, 480), Math.max(d.height, 340));
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());

        if (Validator.isBlank(user) || pass.isEmpty()) {
            UIUtil.error(this, "Please enter both username and password.");
            return;
        }
        try {
            User u = userDao.authenticate(user, pass);
            if (u == null) {
                UIUtil.error(this, "Invalid username or password.");
                txtPass.setText("");
                return;
            }
            if (!"ACTIVE".equals(u.getStatus())) {
                UIUtil.error(this, "This account is inactive. Please contact the administrator.");
                return;
            }
            if ("STUDENT".equals(u.getRole()) && u.getStudentId() == null) {
                UIUtil.error(this, "This student account is not linked to a student record. Please contact the registrar.");
                return;
            }
            Session.login(u);
            if (Session.isStudent()) {
                new StudentPortalFrame().setVisible(true);
            } else {
                new DashboardFrame().setVisible(true);
            }
            dispose();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }
}