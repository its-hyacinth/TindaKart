package com.ddev.TindaKart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

final class LoginIcons {

    private static final Color ICON = new Color(158, 158, 158);

    private LoginIcons() {
    }

    static ImageIcon user(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ICON);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(size / 2 - 4, 2, 8, 8);
        g.drawArc(2, 9, size - 4, size - 6, 0, -180);
        g.dispose();
        return new ImageIcon(image);
    }

    static ImageIcon lock(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ICON);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(4, 1, 10, 10, 0, 180);
        g.drawRoundRect(3, 8, 12, 9, 2, 2);
        g.dispose();
        return new ImageIcon(image);
    }

    static ImageIcon eyeOpen(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ICON);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(2, 5, size - 4, 8);
        g.fillOval(size / 2 - 2, 8, 4, 4);
        g.dispose();
        return new ImageIcon(image);
    }

    static ImageIcon eyeClosed(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ICON);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(2, 8, size - 4, 6, 0, 180);
        g.drawLine(3, 10, size - 3, 6);
        g.dispose();
        return new ImageIcon(image);
    }

    static ImageIcon storefrontWhite(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int awningY = size / 5;
        g.fillRoundRect(size / 6, awningY, size * 2 / 3, 14, 6, 6);
        g.drawLine(size / 6, awningY + 8, size * 5 / 6, awningY + 8);

        int shopX = size / 5;
        int shopY = awningY + 16;
        int shopW = size * 3 / 5;
        int shopH = size / 2;
        g.drawRoundRect(shopX, shopY, shopW, shopH, 8, 8);
        g.drawRoundRect(shopX + shopW / 3, shopY + shopH / 4, shopW / 3, shopH / 3, 4, 4);

        g.dispose();
        return new ImageIcon(image);
    }
}
