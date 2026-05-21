package com.ddev.TindaKart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Donut chart for credit score (0–100), styled like the profile mockup. */
public class CreditScorePieChart extends JPanel {

    private static final Color SCORE_GOOD = new Color(34, 197, 94);
    private static final Color SCORE_MID = new Color(245, 158, 11);
    private static final Color SCORE_LOW = new Color(239, 68, 68);
    private static final Color TRACK = new Color(229, 231, 235);
    private static final Color TEXT = new Color(17, 24, 39);

    private int score;
    private final boolean compact;

    public CreditScorePieChart(int score) {
        this(score, true);
    }

    public CreditScorePieChart(int score, boolean compact) {
        this.compact = compact;
        setScore(score);
        setOpaque(false);
        setPreferredSize(new Dimension(compact ? 72 : 120, compact ? 72 : 120));
    }

    public void setScore(int score) {
        this.score = Math.max(0, Math.min(100, score));
        repaint();
    }

    public int getScore() {
        return score;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight()) - 4;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        g2.setColor(TRACK);
        g2.fillOval(x, y, size, size);

        int arcAngle = (int) (360.0 * score / 100.0);
        g2.setColor(colorForScore(score));
        g2.fillArc(x, y, size, size, 90, -arcAngle);

        int inner = (int) (size * 0.64);
        int innerX = x + (size - inner) / 2;
        int innerY = y + (size - inner) / 2;
        g2.setColor(Color.WHITE);
        g2.fillOval(innerX, innerY, inner, inner);

        int scoreSize = compact ? 16 : 18;
        g2.setColor(TEXT);
        g2.setFont(new Font("Segoe UI", Font.BOLD, scoreSize));
        String text = score + "";
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, getWidth() / 2 - tw / 2, getHeight() / 2 + (compact ? 2 : 6));

        g2.setFont(new Font("Segoe UI", Font.PLAIN, compact ? 7 : 9));
        g2.setColor(new Color(107, 114, 128));
        String label = "Credit Score";
        int lw = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, getWidth() / 2 - lw / 2, getHeight() / 2 + (compact ? 14 : 22));

        g2.dispose();
    }

    private Color colorForScore(int value) {
        if (value >= 80) {
            return SCORE_GOOD;
        }
        if (value >= 65) {
            return SCORE_MID;
        }
        return SCORE_LOW;
    }
}
