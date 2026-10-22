package ui;

import util.UIUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/** Common frame: blue title bar + padded content area. */
public abstract class BaseFrame extends JFrame {

    protected final JPanel content = new JPanel(new BorderLayout(10, 10));

    protected BaseFrame(String title, String subtitle, int width, int height) {
        super(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(width, height);
        setMinimumSize(new Dimension(Math.min(width, 900), Math.min(height, 560)));
        setLocationRelativeTo(null);

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(UIUtil.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel t = new JLabel(title);
        t.setFont(UIUtil.TITLE);
        t.setForeground(Color.WHITE);
        JLabel s = new JLabel(subtitle);
        s.setFont(UIUtil.FONT);
        s.setForeground(new Color(200, 218, 240));
        header.add(t);
        header.add(s);

        content.setBackground(UIUtil.BG);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(content, BorderLayout.CENTER);
    }

    protected JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        return p;
    }

    protected JPanel buttonBar(JButton... buttons) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBackground(Color.WHITE);
        for (JButton b : buttons) p.add(b);
        return p;
    }

    protected JPanel searchBar(JTextField txt, JButton... buttons) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        p.add(UIUtil.label("Search:"));
        p.add(txt);
        for (JButton b : buttons) p.add(b);
        return p;
    }

    protected JPanel titled(String title, JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(190, 200, 215)), title);
        tb.setTitleFont(UIUtil.BOLD);
        tb.setTitleColor(UIUtil.PRIMARY);
        p.setBorder(tb);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    /**
     * Standard management layout: form + buttons on top, search bar and table below.
     */
    protected void layoutCrud(JPanel form, JPanel buttons, JPanel search, JTable table) {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(search, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        content.add(titled("Record Details", top), BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
    }

    protected void disable(JButton... buttons) {
        for (JButton b : buttons) b.setEnabled(false);
    }
}
