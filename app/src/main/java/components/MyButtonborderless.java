package components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

public class MyButtonborderless extends JButton {

    private boolean over;
    private boolean destructive; // New property to determine if the button is destructive
    private Color color;
    private Color colorOver;
    private Color colorClick;
    private Color borderColor;
    private int radius = 10;

    public boolean isOver() {
        return over;
    }

    public void setOver(boolean over) {
        this.over = over;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        setBackground(color);
    }

    public Color getColorOver() {
        return colorOver;
    }

    public void setColorOver(Color colorOver) {
        this.colorOver = colorOver;
    }

    public Color getColorClick() {
        return colorClick;
    }

    public void setColorClick(Color colorClick) {
        this.colorClick = colorClick;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public boolean isDestructive() {
        return destructive;
    }

    public void setDestructive(boolean destructive) {
        this.destructive = destructive;
        if (destructive) {
            setColor(new Color(255,51,51)); // Red for destructive
            setColorOver(new Color(200, 40, 60));
            setColorClick(new Color(180, 30, 50));
        } else {
            setColor(new Color(51, 54, 82)); // Default color
            setColorOver(new Color(70, 80, 100));
            setColorClick(new Color(245, 245, 245));
        }
    }

    public MyButtonborderless() {
        // Init Color
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setDestructive(false); // Default is not destructive
        borderColor = new Color(63, 106, 149, 50);
        setContentAreaFilled(false);

        // Add event mouse
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent me) {
                setBackground(colorOver);
                over = true;
            }

            @Override
            public void mouseExited(MouseEvent me) {
                setBackground(color);
                over = false;
            }

            @Override
            public void mousePressed(MouseEvent me) {
                setBackground(colorClick);
                setForeground(new Color(58, 58, 58));
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                setForeground(new Color(245, 245, 245));
                if (over) {
                    setBackground(colorOver);
                } else {
                    setBackground(color);
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Paint Border
        g2.setColor(borderColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.setColor(getBackground());
        // Border set 2 Pix
        g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, radius, radius);
        super.paintComponent(grphcs);
    }
}
