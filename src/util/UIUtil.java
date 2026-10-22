package util;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/** Shared look-and-feel helpers (AWT colors, fonts, layouts) and message dialogs. */
public class UIUtil {

    public static final Color PRIMARY = new Color(21, 67, 125);
    public static final Color BG = new Color(241, 245, 250);
    public static final Color SUCCESS = new Color(30, 132, 73);
    public static final Color WARNING = new Color(214, 137, 16);
    public static final Color DANGER = new Color(176, 58, 46);
    public static final Color NEUTRAL = new Color(93, 109, 126);

    public static final Font FONT = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 22);

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(BOLD);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(Math.max(95, b.getPreferredSize().width + 16), 32));
        return b;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BOLD);
        return l;
    }

    public static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(FONT);
        return f;
    }

    public static JTable createTable() {
        JTable t = new JTable();
        t.setFont(FONT);
        t.setRowHeight(25);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setSelectionBackground(new Color(174, 206, 240));
        t.setSelectionForeground(Color.BLACK);
        t.setGridColor(new Color(220, 226, 234));
        t.getTableHeader().setFont(BOLD);
        t.getTableHeader().setBackground(PRIMARY);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    public static void fillTable(JTable table, String[] columns, List<Object[]> rows) {
        DefaultTableModel m = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (Object[] r : rows) m.addRow(r);
        table.setModel(m);
    }

    /** Returns the integer in column 0 of the selected row, or -1 if nothing is selected. */
    public static int selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) return -1;
        Object v = table.getValueAt(row, 0);
        return v == null ? -1 : ((Number) v).intValue();
    }

    /** Adds "label : component" at the given grid column/row (two grid cells per column). */
    public static void addField(JPanel p, int col, int row, String text, JComponent comp) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.anchor = GridBagConstraints.WEST;
        g.gridy = row;
        g.gridx = col * 2;
        g.weightx = 0;
        p.add(label(text), g);
        comp.setFont(FONT);
        g.gridx = col * 2 + 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(comp, g);
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Please Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Converts Derby SQL errors into messages a normal user can understand. */
    public static void showSqlError(Component parent, SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String msg;
        if ("23505".equals(state)) {
            msg = "Duplicate record: a record with the same unique value already exists.";
        } else if ("23503".equals(state)) {
            msg = "This action is blocked by related records (foreign key).\n"
                + "Delete or change the related records first, or choose a valid reference.";
        } else if ("22001".equals(state)) {
            msg = "One of the values is too long for its field.";
        } else if ("23513".equals(state)) {
            msg = "A value breaks a database rule (for example, grades must be between 0 and 100).";
        } else if (state.startsWith("08") || "XJ040".equals(state) || "XJ004".equals(state)) {
            msg = "Cannot connect to the Apache Derby database.\n" + e.getMessage();
        } else {
            msg = "Database error: " + e.getMessage();
        }
        error(parent, msg);
    }
}
