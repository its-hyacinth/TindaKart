package com.ddev.TindaKart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/** Blue circular icons used in the customer profile dialog. */
final class ProfileIcons {

    static final Color BLUE = new Color(37, 99, 235);
    static final Color WHITE = Color.WHITE;

    private ProfileIcons() {
    }

    static ImageIcon section(int size, Section type) {
        return circleIcon(size, switch (type) {
            case USER -> drawUser;
            case MESSAGE -> drawMessage;
            case SEND -> drawSend;
            default -> drawUser;
        });
    }

    static ImageIcon field(int size, Field type) {
        return circleIcon(size, switch (type) {
            case PHONE -> drawPhone;
            case EMAIL -> drawEmail;
            case ADDRESS -> drawAddress;
            case WALLET -> drawWallet;
            case CALENDAR -> drawCalendar;
        });
    }

    enum Section { USER, MESSAGE, SEND }

    enum Field { PHONE, EMAIL, ADDRESS, WALLET, CALENDAR }

    @FunctionalInterface
    private interface GlyphDrawer {
        void draw(Graphics2D g, int size);
    }

    private static ImageIcon circleIcon(int size, GlyphDrawer drawer) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BLUE);
        g.fillOval(0, 0, size, size);
        g.setColor(WHITE);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawer.draw(g, size);
        g.dispose();
        return new ImageIcon(image);
    }

    private static final GlyphDrawer drawUser = (g, s) -> {
        g.drawOval(s / 2 - 4, s / 4, 8, 8);
        g.drawArc(s / 4, s / 2, s / 2, s / 3, 0, -180);
    };

    private static final GlyphDrawer drawMessage = (g, s) -> {
        g.drawRoundRect(s / 5, s / 4, s * 3 / 5, s / 3, 6, 6);
        g.drawLine(s / 3, s * 2 / 3, s / 2, s * 3 / 4);
        g.drawLine(s / 2, s * 3 / 4, s * 2 / 3, s * 2 / 3);
    };

    private static final GlyphDrawer drawSend = (g, s) -> {
        g.drawLine(s / 5, s / 2, s * 4 / 5, s / 4);
        g.drawLine(s / 5, s / 2, s * 4 / 5, s * 3 / 4);
        g.drawLine(s / 5, s / 2, s * 3 / 5, s / 2);
    };

    private static final GlyphDrawer drawPhone = (g, s) -> {
        g.drawRoundRect(s / 3, s / 5, s / 3, s * 3 / 5, 4, 4);
        g.drawLine(s / 2, s / 6, s / 2, s / 5);
    };

    private static final GlyphDrawer drawEmail = (g, s) -> {
        g.drawRoundRect(s / 5, s / 3, s * 3 / 5, s / 3, 4, 4);
        g.drawLine(s / 5, s / 3, s / 2, s / 2);
        g.drawLine(s * 4 / 5, s / 3, s / 2, s / 2);
    };

    private static final GlyphDrawer drawAddress = (g, s) -> {
        g.drawOval(s / 3, s / 6, s / 3, s / 3);
        g.drawLine(s / 2, s / 2, s / 2, s * 4 / 5);
        g.drawArc(s / 4, s / 2, s / 2, s / 3, 180, 180);
    };

    private static final GlyphDrawer drawWallet = (g, s) -> {
        g.drawRoundRect(s / 5, s / 3, s * 3 / 5, s / 3, 4, 4);
        g.drawLine(s / 3, s / 2, s * 2 / 3, s / 2);
    };

    private static final GlyphDrawer drawCalendar = (g, s) -> {
        g.drawRoundRect(s / 5, s / 4, s * 3 / 5, s / 2, 4, 4);
        g.drawLine(s / 5, s / 2, s * 4 / 5, s / 2);
        g.drawLine(s / 3, s / 6, s / 3, s / 4);
        g.drawLine(s * 2 / 3, s / 6, s * 2 / 3, s / 4);
    };
}
