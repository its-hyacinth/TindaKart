package com.ddev.TindaKart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Light-blue circular avatar with person silhouette (mockup style). */
public class AnonymousAvatarPanel extends JPanel {

    private static final Color BG = new Color(232, 240, 254);
    private static final Color ICON = new Color(37, 99, 235);

    public AnonymousAvatarPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(88, 88));
        setMinimumSize(new Dimension(88, 88));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight()) - 4;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        int cx = getWidth() / 2;

        g2.setColor(BG);
        g2.fillOval(x, y, size, size);

        g2.setColor(ICON);
        g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int head = size / 5;
        g2.drawOval(cx - head / 2, y + size / 4, head, head);
        g2.drawArc(cx - size / 4, y + size / 2, size / 2, size / 3, 0, -180);

        g2.dispose();
    }
}
