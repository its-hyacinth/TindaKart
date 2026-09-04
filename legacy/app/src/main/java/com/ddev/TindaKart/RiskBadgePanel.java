package com.ddev.TindaKart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Pill badge showing LOW / MEDIUM / HIGH risk from credit score. */
public class RiskBadgePanel extends JPanel {

    private static final Color LOW_BG = new Color(220, 252, 231);
    private static final Color LOW_FG = new Color(22, 101, 52);
    private static final Color MED_BG = new Color(254, 243, 199);
    private static final Color MED_FG = new Color(146, 64, 14);
    private static final Color HIGH_BG = new Color(254, 226, 226);
    private static final Color HIGH_FG = new Color(185, 28, 28);

    private final int score;

    public RiskBadgePanel(int score) {
        this.score = score;
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(new Font("Segoe UI", Font.BOLD, 10));
        String text = labelFor(score);
        return new Dimension(fm.stringWidth(text) + 36, 22);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String text = labelFor(score);
        Color bg = bgFor(score);
        Color fg = fgFor(score);

        Font font = new Font("Segoe UI", Font.BOLD, 10);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);
        int w = textW + 28;
        int h = 20;
        int x = 0;
        int y = (getHeight() - h) / 2;

        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, h, h);

        g2.setColor(fg);
        g2.fillOval(x + 6, y + 5, 10, 10);
        g2.setColor(bg);
        g2.fillOval(x + 8, y + 7, 6, 6);

        g2.setColor(fg);
        g2.drawString(text, x + 20, y + 14);

        g2.dispose();
    }

    private static String labelFor(int value) {
        if (value >= 70) {
            return "LOW RISK";
        }
        if (value >= 50) {
            return "MEDIUM RISK";
        }
        return "HIGH RISK";
    }

    private static Color bgFor(int value) {
        if (value >= 70) {
            return LOW_BG;
        }
        if (value >= 50) {
            return MED_BG;
        }
        return HIGH_BG;
    }

    private static Color fgFor(int value) {
        if (value >= 70) {
            return LOW_FG;
        }
        if (value >= 50) {
            return MED_FG;
        }
        return HIGH_FG;
    }
}
