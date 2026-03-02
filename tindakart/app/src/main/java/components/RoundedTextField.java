package components;


import javax.swing.*;
import java.awt.*;
import javax.swing.border.AbstractBorder;

public class RoundedTextField extends JTextField {
    private int cornerRadius = 20;

    // Default constructor for NetBeans GUI Builder
    public RoundedTextField() {
        this(10); // Default columns
    }

    public RoundedTextField(int columns) {
        super(columns);
        setBorder(new RoundedBorder(cornerRadius)); // Default rounded border
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        setBorder(new RoundedBorder(cornerRadius));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
    }
}

class RoundedBorder extends AbstractBorder {
    private int radius;

    public RoundedBorder(int radius) {
        this.radius = radius;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(radius, radius, radius, radius);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.GRAY); // Border color
        g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
    }
}
