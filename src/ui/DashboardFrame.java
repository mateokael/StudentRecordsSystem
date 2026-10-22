package ui;

import dao.CourseDAO;
import dao.ReportDAO;
import dao.StudentDAO;
import database.DatabaseConnection;
import model.Course;
import model.Student;
import util.Session;
import util.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFrame extends JFrame {

    // ---------- light palette ----------
    private static final Color BG = new Color(240, 244, 250);
    private static final Color HEADER_BG = UIUtil.PRIMARY;
    private static final Color SIDEBAR_BG = Color.WHITE;
    private static final Color CARD_BG = Color.WHITE;
    private static final Color LINE = new Color(214, 222, 234);
    private static final Color BTN_LINE = new Color(200, 205, 212);
    private static final Color TEXT = new Color(33, 37, 41);
    private static final Color MUTED = new Color(108, 117, 125);
    private static final Color ACCENT = UIUtil.PRIMARY;
    private static final Color ACCENT_TEXT = UIUtil.PRIMARY;
    private static final Color ACTIVE_BG = UIUtil.PRIMARY;
    private static final Color HOVER_BG = new Color(226, 234, 245);
    private static final Color GREEN = new Color(30, 140, 60);
    private static final Color TRACK = new Color(226, 232, 242);
    private static final Color HEADER_MUTED = new Color(200, 218, 240);
    private static final Color BADGE = new Color(214, 48, 49);

    // ---------- fonts (change sizes here to scale the whole screen) ----------
    private static final Font F_HEADER_TITLE = new Font("SansSerif", Font.BOLD, 24);
    private static final Font F_WELCOME = new Font("SansSerif", Font.PLAIN, 28);
    private static final Font F_SUB = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font F_SECTION_LABEL = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font F_NAV = new Font("SansSerif", Font.PLAIN, 17);
    private static final Font F_CARD_VALUE = new Font("SansSerif", Font.BOLD, 38);
    private static final Font F_CARD_TITLE = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font F_SECTION = new Font("SansSerif", Font.BOLD, 20);
    private static final Font F_BUTTON = new Font("SansSerif", Font.PLAIN, 18);
    private static final Font F_ROW = new Font("SansSerif", Font.PLAIN, 17);
    private static final Font F_SMALL = new Font("SansSerif", Font.PLAIN, 13);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final ReportDAO reportDao = new ReportDAO();
    private final StudentDAO studentDao = new StudentDAO();
    private final CourseDAO courseDao = new CourseDAO();

    private final JLabel lblStudents = value(), lblCourses = value(),
            lblSubjects = value(), lblEnrollments = value();
    private final JLabel subStudents = sub(), subCourses = sub(),
            subSubjects = sub(), subEnrollments = sub();
    private final JLabel lblDate = new JLabel(), lblTime = new JLabel();
    private final BarChart chart = new BarChart();
    private final javax.swing.Timer clock = new javax.swing.Timer(1000, e -> tick());

    public DashboardFrame() {
        super("Student Records System - Dashboard");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1200, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(buildHeader(), BorderLayout.NORTH);
        getContentPane().add(buildSidebar(), BorderLayout.WEST);
        getContentPane().add(buildMain(), BorderLayout.CENTER);

        tick();
        clock.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                refreshStats();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                exitApp();
            }
        });
    }

    @Override
    public void dispose() {
        clock.stop();
        super.dispose();
    }

    // =====================================================================
    //  HEADER
    // =====================================================================
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(HEADER_BG);
        h.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, HEADER_BG),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)));

        // left: logo + app name
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(new JLabel(new NavIcon("Subjects", Color.WHITE, 26)));
        JLabel title = new JLabel("Student Records");
        title.setFont(F_HEADER_TITLE);
        title.setForeground(Color.WHITE);
        left.add(title);

        // right: user menu
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        right.setOpaque(false);
        right.add(buildUserMenu());

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildUserMenu() {
        final JPopupMenu menu = new JPopupMenu();
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Student Enrollment, Grading and Record Management System\n"
              + "Java Swing / AWT + JDBC + Apache Derby\n\n"
              + "Roles:\nADMIN - full access\nREGISTRAR - students, courses, subjects, enrollment\n"
              + "TEACHER - grades and attendance", "About", JOptionPane.INFORMATION_MESSAGE));
        JMenuItem logout = new JMenuItem("Log out");
        logout.addActionListener(e -> logout());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> exitApp());
        menu.add(about);
        menu.addSeparator();
        menu.add(logout);
        menu.add(exit);

        final JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.setToolTipText("Role: " + Session.getUser().getRole());
        JLabel name = new JLabel(Session.getUser().getFullName());
        name.setFont(new Font("SansSerif", Font.PLAIN, 20));
        name.setForeground(Color.WHITE);
        JLabel chevron = new JLabel("\u25BE");
        chevron.setFont(new Font("SansSerif", Font.PLAIN, 18));
        chevron.setForeground(HEADER_MUTED);
        p.add(new JLabel(new NavIcon("Students", Color.WHITE, 22)));
        p.add(name);
        p.add(chevron);
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                menu.show(p, 0, p.getHeight());
            }
        });
        return p;
    }

    // =====================================================================
    //  SIDEBAR
    // =====================================================================
    private JPanel buildSidebar() {
        JPanel s = new JPanel();
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBackground(SIDEBAR_BG);
        s.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, LINE),
                BorderFactory.createEmptyBorder(8, 0, 12, 0)));
        s.setPreferredSize(new Dimension(220, 100));

        s.add(sectionLabel("MAIN"));
        s.add(navItem("Dashboard", null, true, true));
        s.add(navItem("Students", () -> new StudentFrame().setVisible(true), true, false));
        s.add(navItem("Courses", () -> new CourseFrame().setVisible(true), true, false));
        s.add(navItem("Subjects", () -> new SubjectFrame().setVisible(true), true, false));
        s.add(navItem("Enrollment", () -> new EnrollmentFrame().setVisible(true), true, false));

        s.add(sectionLabel("ACADEMICS"));
        s.add(navItem("Grades", () -> new GradeFrame().setVisible(true), true, false));
        s.add(navItem("Attendance", () -> new AttendanceFrame().setVisible(true), true, false));

        s.add(sectionLabel("ADMIN"));
        s.add(navItem("Reports", () -> new ReportFrame().setVisible(true), true, false));
        s.add(navItem("Users", () -> new UserFrame().setVisible(true), Session.isAdmin(), false));

        s.add(Box.createVerticalGlue());
        JLabel db = new JLabel("Database: " + DatabaseConnection.getMode());
        db.setFont(F_SMALL);
        db.setForeground(MUTED);
        db.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        db.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.add(db);
        return s;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_SECTION_LABEL);
        l.setForeground(MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(20, 20, 8, 16));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height));
        return l;
    }

    private JButton navItem(String text, Runnable action, boolean enabled, boolean active) {
        final JButton b = new JButton(text);
        b.setFont(F_NAV);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        if (active) {
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(130, 178, 240)),
                    BorderFactory.createEmptyBorder(11, 16, 11, 16)));
            b.setBackground(ACTIVE_BG);
            b.setForeground(Color.WHITE);
            b.setFont(F_NAV.deriveFont(Font.BOLD));
        } else {
            b.setBorder(BorderFactory.createEmptyBorder(11, 20, 11, 16));
            b.setBackground(SIDEBAR_BG);
            b.setForeground(enabled ? TEXT : new Color(175, 180, 186));
        }
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        b.setEnabled(enabled);
        if (action != null) b.addActionListener(e -> action.run());
        if (enabled && !active) {
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    b.setBackground(HOVER_BG);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    b.setBackground(SIDEBAR_BG);
                }
            });
        }
        return b;
    }

    // =====================================================================
    //  MAIN CONTENT
    // =====================================================================
    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setBackground(BG);
        main.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        // --- welcome row ---
        JLabel welcome = new JLabel("Welcome back, " + Session.getUser().getFullName());
        welcome.setFont(F_WELCOME);
        welcome.setForeground(TEXT);
        JLabel subtitle = new JLabel("Here's what's happening today");
        subtitle.setFont(F_SUB);
        subtitle.setForeground(MUTED);
        JPanel welcomeText = new JPanel(new GridLayout(2, 1));
        welcomeText.setOpaque(false);
        welcomeText.add(welcome);
        welcomeText.add(subtitle);

        for (JLabel l : new JLabel[]{lblDate, lblTime}) {
            l.setFont(F_SUB);
            l.setForeground(MUTED);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        JPanel clockPanel = new JPanel(new GridLayout(2, 1));
        clockPanel.setOpaque(false);
        clockPanel.add(lblDate);
        clockPanel.add(lblTime);

        JPanel welcomeRow = new JPanel(new BorderLayout());
        welcomeRow.setOpaque(false);
        welcomeRow.add(welcomeText, BorderLayout.WEST);
        welcomeRow.add(clockPanel, BorderLayout.EAST);

        // --- stat cards ---
        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(100, 180));
        stats.add(statCard("Students", lblStudents, "Students", subStudents));
        stats.add(statCard("Courses", lblCourses, "Courses", subCourses));
        stats.add(statCard("Subjects", lblSubjects, "Subjects", subSubjects));
        stats.add(statCard("Enrollment", lblEnrollments, "Enrollments", subEnrollments));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(welcomeRow, BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);

        // --- quick actions ---
        ActionButton btnRegister = new ActionButton("Register student", "Students", UIUtil.SUCCESS);
        ActionButton btnEnroll = new ActionButton("Enroll student", "Enrollment", UIUtil.PRIMARY);
        ActionButton btnReport = new ActionButton("Generate report", "Reports", UIUtil.NEUTRAL);
        btnRegister.addActionListener(e -> new StudentFrame().setVisible(true));
        btnEnroll.addActionListener(e -> new EnrollmentFrame().setVisible(true));
        btnReport.addActionListener(e -> new ReportFrame().setVisible(true));

        JPanel buttons = new JPanel(new GridLayout(3, 1, 0, 14));
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(100, 3 * 58 + 2 * 14));
        buttons.add(btnRegister);
        buttons.add(btnEnroll);
        buttons.add(btnReport);
        JPanel buttonsWrap = new JPanel(new BorderLayout());
        buttonsWrap.setOpaque(false);
        buttonsWrap.add(buttons, BorderLayout.NORTH);

        JPanel quick = section("Bolt", "Quick actions", buttonsWrap);
        JPanel perCourse = section("Chart", "Students per course", chart);

        JPanel lower = new JPanel(new GridBagLayout());
        lower.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.44;
        c.insets = new Insets(0, 0, 0, 8);
        lower.add(quick, c);
        c.gridx = 1;
        c.weightx = 0.56;
        c.insets = new Insets(0, 8, 0, 0);
        lower.add(perCourse, c);

        main.add(top, BorderLayout.NORTH);
        main.add(lower, BorderLayout.CENTER);
        return main;
    }

    private static JLabel value() {
        JLabel l = new JLabel("0");
        l.setFont(F_CARD_VALUE);
        l.setForeground(UIUtil.PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel sub() {
        JLabel l = new JLabel(" ");
        l.setFont(F_CARD_TITLE);
        l.setForeground(GREEN);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel statCard(String iconKind, JLabel val, String title, JLabel subLabel) {
        RoundPanel p = new RoundPanel(new BorderLayout(), CARD_BG, LINE, 18);
        p.setTopBar(UIUtil.PRIMARY);
        p.setBorder(BorderFactory.createEmptyBorder(22, 20, 16, 20));

        JPanel iconWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        iconWrap.setOpaque(false);
        iconWrap.add(new JLabel(new NavIcon(iconKind, ACCENT_TEXT, 24)));

        JLabel t = new JLabel(title);
        t.setFont(F_CARD_TITLE);
        t.setForeground(MUTED);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel txt = new JPanel();
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.setOpaque(false);
        txt.add(val);
        txt.add(t);
        txt.add(Box.createVerticalStrut(4));
        txt.add(subLabel);

        p.add(iconWrap, BorderLayout.NORTH);
        p.add(txt, BorderLayout.SOUTH);
        return p;
    }

    /** Rounded dark panel with an icon + title row, used for the two lower sections. */
    private JPanel section(String iconKind, String title, JComponent body) {
        RoundPanel p = new RoundPanel(new BorderLayout(0, 16), CARD_BG, LINE, 18);
        p.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        head.setOpaque(false);
        head.add(new JLabel(new NavIcon(iconKind, ACCENT_TEXT, 22)));
        JLabel l = new JLabel(title);
        l.setFont(F_SECTION);
        l.setForeground(UIUtil.PRIMARY);
        head.add(l);

        p.add(head, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    // =====================================================================
    //  DATA
    // =====================================================================
    private void tick() {
        LocalDateTime now = LocalDateTime.now();
        lblDate.setText(now.format(DATE_FMT));
        lblTime.setText(now.format(TIME_FMT));
    }

    private void refreshStats() {
        try {
            lblStudents.setText(String.valueOf(reportDao.count("STUDENTS")));
            lblCourses.setText(String.valueOf(reportDao.count("COURSES")));
            lblSubjects.setText(String.valueOf(reportDao.count("SUBJECTS")));
            lblEnrollments.setText(String.valueOf(reportDao.count("ENROLLMENTS")));
            int today = reportDao.countEnrollmentsToday();
            subEnrollments.setText(today > 0 ? "+" + today + " today" : " ");
            refreshChart();
        } catch (SQLException ex) {
            UIUtil.showSqlError(this, ex);
        }
    }

    /** Rebuilds the students-per-course chart (top 6 courses). */
    private void refreshChart() throws SQLException {
        List<Student> all = studentDao.getAll();

        Map<Integer, String> codes = new LinkedHashMap<>();
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (Course c : courseDao.getAll()) {
            String label = c.toString();
            int idx = label.indexOf(" - ");
            if (idx > 0) label = label.substring(0, idx);
            codes.put(c.getCourseId(), label);
            counts.put(c.getCourseId(), 0);
        }
        for (Student s : all) {
            counts.merge(s.getCourseId(), 1, Integer::sum);
        }

        List<Map.Entry<Integer, Integer>> rows = new ArrayList<>(counts.entrySet());
        rows.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        Map<String, Integer> data = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(6, rows.size()); i++) {
            Map.Entry<Integer, Integer> e = rows.get(i);
            data.put(codes.getOrDefault(e.getKey(), "?"), e.getValue());
        }
        chart.setData(data);
    }

    // =====================================================================
    //  CUSTOM COMPONENTS
    // =====================================================================

    /** Panel with rounded corners, a fill colour and an optional outline. */
    private static class RoundPanel extends JPanel {
        private final Color fill;
        private final Color outline;
        private final int arc;
        private Color topBar;

        void setTopBar(Color c) {
            this.topBar = c;
        }

        RoundPanel(LayoutManager lm, Color fill, Color outline, int arc) {
            super(lm);
            this.fill = fill;
            this.outline = outline;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (topBar != null) {
                g2.clip(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc));
                g2.setColor(topBar);
                g2.fillRect(0, 0, getWidth(), 5);
                g2.setClip(null);
            }
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Solid, rounded quick-action button with an icon. */
    private static class ActionButton extends JButton {
        private final Color base;
        private boolean hover;

        ActionButton(String text, String iconKind, Color base) {
            super(text);
            this.base = base;
            setIcon(new NavIcon(iconKind, Color.WHITE, 20));
            setIconTextGap(12);
            setFont(new Font("SansSerif", Font.BOLD, 17));
            setForeground(Color.WHITE);
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hover ? base.darker() : base);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Horizontal bar chart drawn by hand (no library needed). */
    private static class BarChart extends JPanel {
        private Map<String, Integer> data = new LinkedHashMap<>();

        BarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(300, 220));
        }

        void setData(Map<String, Integer> d) {
            this.data = d;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(F_ROW);
            FontMetrics fm = g2.getFontMetrics();
            if (data.isEmpty()) {
                g2.setColor(MUTED);
                g2.drawString("No data yet", 2, 24);
                g2.dispose();
                return;
            }
            int max = 1;
            int labelW = 50;
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                max = Math.max(max, e.getValue());
                labelW = Math.max(labelW, fm.stringWidth(e.getKey()));
            }
            labelW += 22;
            int valueW = 44, rowH = 42, barH = 12;
            int trackW = Math.max(40, getWidth() - labelW - valueW);

            int y = 4;
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                int cy = y + rowH / 2;
                int textY = cy - (fm.getAscent() + fm.getDescent()) / 2 + fm.getAscent();

                g2.setColor(TEXT);
                g2.drawString(e.getKey(), 0, textY);

                g2.setColor(TRACK);
                g2.fillRoundRect(labelW, cy - barH / 2, trackW, barH, barH, barH);
                if (e.getValue() > 0) {
                    int w = (int) Math.round((double) trackW * e.getValue() / max);
                    g2.setColor(ACCENT);
                    g2.fillRoundRect(labelW, cy - barH / 2, Math.max(w, barH), barH, barH, barH);
                }

                g2.setColor(TEXT);
                String v = String.valueOf(e.getValue());
                g2.drawString(v, getWidth() - fm.stringWidth(v), textY);
                y += rowH;
            }
            g2.dispose();
        }
    }

    /** Small line icons drawn in code, so there are no image files to manage. */
    private static class NavIcon implements Icon {
        private final String kind;
        private final Color color;
        private final int size;

        NavIcon(String kind, Color color, int size) {
            this.kind = kind;
            this.color = color;
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.scale(size / 20.0, size / 20.0); // icons are designed on a 20x20 grid
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(color);
            switch (kind) {
                case "Dashboard":
                    g2.drawRoundRect(2, 2, 6, 6, 2, 2);
                    g2.drawRoundRect(12, 2, 6, 6, 2, 2);
                    g2.drawRoundRect(2, 12, 6, 6, 2, 2);
                    g2.drawRoundRect(12, 12, 6, 6, 2, 2);
                    break;
                case "Students":
                    g2.drawOval(6, 2, 8, 8);
                    g2.drawArc(2, 12, 16, 14, 0, 180);
                    break;
                case "Courses":
                    g2.drawPolygon(new int[]{10, 19, 10, 1}, new int[]{3, 8, 13, 8}, 4);
                    g2.drawLine(5, 11, 5, 15);
                    g2.drawLine(15, 11, 15, 15);
                    g2.drawLine(5, 15, 15, 15);
                    break;
                case "Subjects":
                    g2.drawRoundRect(3, 2, 14, 16, 3, 3);
                    g2.drawLine(7, 2, 7, 18);
                    g2.drawLine(10, 7, 14, 7);
                    break;
                case "Enrollment":
                    g2.drawRoundRect(3, 3, 14, 15, 3, 3);
                    g2.drawLine(6, 11, 9, 14);
                    g2.drawLine(9, 14, 14, 8);
                    break;
                case "Reports":
                    g2.drawRoundRect(4, 2, 12, 16, 2, 2);
                    g2.drawLine(7, 7, 13, 7);
                    g2.drawLine(7, 10, 13, 10);
                    g2.drawLine(7, 13, 11, 13);
                    break;
                case "Search":
                    g2.drawOval(3, 3, 10, 10);
                    g2.drawLine(11, 11, 17, 17);
                    break;
                case "Bell":
                    g2.drawArc(4, 2, 12, 12, 0, 180);
                    g2.drawLine(4, 8, 4, 14);
                    g2.drawLine(16, 8, 16, 14);
                    g2.drawLine(2, 14, 18, 14);
                    g2.drawArc(8, 14, 4, 4, 180, 180);
                    break;
                case "Bolt":
                    g2.drawPolygon(new int[]{11, 4, 9, 8, 16, 11},
                            new int[]{2, 11, 11, 18, 8, 8}, 6);
                    break;
                case "Chart":
                    g2.drawLine(4, 17, 4, 10);
                    g2.drawLine(10, 17, 10, 3);
                    g2.drawLine(16, 17, 16, 7);
                    break;
                default:
                    g2.drawOval(4, 4, 12, 12);
            }
            g2.dispose();
        }
    }

    // =====================================================================
    //  ACTIONS
    // =====================================================================
    private void logout() {
        if (UIUtil.confirm(this, "Are you sure you want to log out?")) {
            Session.logout();
            for (Frame f : Frame.getFrames()) {
                if (f != this && f.isDisplayable()) f.dispose();
            }
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void exitApp() {
        if (UIUtil.confirm(this, "Exit the application?")) {
            DatabaseConnection.shutdown();
            System.exit(0);
        }
    }
}