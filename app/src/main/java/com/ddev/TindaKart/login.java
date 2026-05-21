package com.ddev.TindaKart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class login extends JFrame {

    /** Matches brand photo background #1a233b */
    private static final Color BRAND_NAVY = new Color(26, 35, 59);
    private static final Color NAVY = BRAND_NAVY;
    private static final Color LINK = new Color(37, 99, 235);
    private static final Color MUTED = new Color(130, 130, 130);
    private static final Color PLACEHOLDER = new Color(170, 170, 170);
    private static final Color FIELD_BORDER = new Color(210, 210, 210);

    private static final String USER_PLACEHOLDER = "Enter your username";
    private static final String PASS_PLACEHOLDER = "Enter your password";

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel eyeToggle = new JLabel(LoginIcons.eyeClosed(18));
    private boolean passwordVisible;
    private boolean passwordPlaceholder = true;

    private BufferedImage brandImage;

    public login() {
        setTitle("TindaKart - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 580));
        setSize(1100, 640);
        setLocationRelativeTo(null);
        loadBrandImage();
        initLayout();
    }

    private void loadBrandImage() {
        try {
            URL brandUrl = getClass().getResource("/images/login_brand.png");
            if (brandUrl != null) {
                brandImage = ImageIO.read(brandUrl);
            }
        } catch (Exception ex) {
            System.err.println("Could not load login brand image: " + ex.getMessage());
        }
    }

    private void initLayout() {
        JPanel root = new JPanel(new GridLayout(1, 2, 0, 0));
        root.add(buildBrandPanel());
        root.add(buildFormPanel());
        setContentPane(root);
    }

    private JPanel buildBrandPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();
                g2.setColor(BRAND_NAVY);
                g2.fillRect(0, 0, w, h);

                if (brandImage != null && w > 0 && h > 0) {
                    int iw = brandImage.getWidth();
                    int ih = brandImage.getHeight();
                    double scale = Math.min((double) w / iw, (double) h / ih);
                    int dw = (int) (iw * scale);
                    int dh = (int) (ih * scale);
                    int x = (w - dw) / 2;
                    int y = (h - dh) / 2;
                    g2.drawImage(brandImage, x, y, dw, dh, null);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
                    String text = "TINDAKART";
                    int tw = g2.getFontMetrics().stringWidth(text);
                    g2.drawString(text, (w - tw) / 2, h / 2);
                }
                g2.dispose();
            }
        };
    }

    private JPanel buildFormPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(380, 420));
        form.setMaximumSize(new Dimension(380, 420));

        JLabel welcome = new JLabel("Welcome Back!");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcome.setForeground(NAVY);
        welcome.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Please login to your account");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        setupUsernamePlaceholder();
        setupPasswordPlaceholder();

        form.add(welcome);
        form.add(Box.createRigidArea(new Dimension(0, 6)));
        form.add(subtitle);
        form.add(Box.createRigidArea(new Dimension(0, 28)));
        form.add(buildFieldBlock("Username", usernameField, LoginIcons.user(18), null));
        form.add(Box.createRigidArea(new Dimension(0, 18)));
        form.add(buildPasswordBlock());
        form.add(Box.createRigidArea(new Dimension(0, 14)));
        form.add(buildOptionsRow());
        form.add(Box.createRigidArea(new Dimension(0, 22)));
        form.add(buildLoginButton());

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(Color.WHITE);
        center.setBorder(new EmptyBorder(0, 64, 0, 64));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        center.add(form, gbc);

        JLabel footer = new JLabel("<html><center>Don't have an account? <font color='#2563EB'><b>Contact Admin</b></font></center></html>");
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setForeground(MUTED);
        footer.setBorder(new EmptyBorder(0, 64, 32, 64));
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        footer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        footer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(login.this,
                        "Please contact your system administrator to create an account.",
                        "Contact Admin",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        wrapper.add(center, BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildFieldBlock(String labelText, JComponent field, ImageIcon icon, JComponent east) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(LEFT_ALIGNMENT);
        block.setMaximumSize(new Dimension(380, 78));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(NAVY);
        label.setAlignmentX(LEFT_ALIGNMENT);

        if (field instanceof JTextField textField) {
            styleField(textField);
        } else if (field instanceof JPasswordField passField) {
            styleField(passField);
        }

        JPanel row = createRoundedInputRow();
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setBorder(new EmptyBorder(0, 14, 0, 6));
        row.add(iconLabel, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        if (east != null) {
            row.add(east, BorderLayout.EAST);
        }

        block.add(label);
        block.add(Box.createRigidArea(new Dimension(0, 8)));
        block.add(row);
        return block;
    }

    private JPanel buildPasswordBlock() {
        eyeToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eyeToggle.setBorder(new EmptyBorder(0, 6, 0, 14));
        eyeToggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!passwordPlaceholder) {
                    togglePasswordVisibility();
                }
            }
        });
        return buildFieldBlock("Password", passwordField, LoginIcons.lock(18), eyeToggle);
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(new EmptyBorder(11, 4, 11, 8));
        field.setOpaque(false);
        field.setBackground(Color.WHITE);
        field.setAlignmentX(LEFT_ALIGNMENT);
    }

    private JPanel createRoundedInputRow() {
        JPanel row = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(FIELD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(380, 44));
        row.setPreferredSize(new Dimension(380, 44));
        return row;
    }

    private void setupUsernamePlaceholder() {
        usernameField.setForeground(PLACEHOLDER);
        usernameField.setText(USER_PLACEHOLDER);
        usernameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (USER_PLACEHOLDER.equals(usernameField.getText())) {
                    usernameField.setText("");
                    usernameField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (usernameField.getText().isBlank()) {
                    usernameField.setForeground(PLACEHOLDER);
                    usernameField.setText(USER_PLACEHOLDER);
                }
            }
        });
    }

    private void setupPasswordPlaceholder() {
        showPasswordPlaceholder();
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (passwordPlaceholder) {
                    passwordPlaceholder = false;
                    passwordField.setText("");
                    passwordField.setEchoChar('\u2022');
                    passwordField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (passwordField.getPassword().length == 0 && !passwordVisible) {
                    showPasswordPlaceholder();
                }
            }
        });
        passwordField.addActionListener(e -> attemptLogin());
    }

    private void showPasswordPlaceholder() {
        passwordPlaceholder = true;
        passwordVisible = false;
        passwordField.setEchoChar((char) 0);
        passwordField.setText(PASS_PLACEHOLDER);
        passwordField.setForeground(PLACEHOLDER);
        eyeToggle.setIcon(LoginIcons.eyeClosed(18));
    }

    private JPanel buildOptionsRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(380, 26));

        JCheckBox remember = new JCheckBox("Remember me");
        remember.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        remember.setForeground(MUTED);
        remember.setOpaque(false);
        remember.setFocusPainted(false);

        JLabel forgot = new JLabel("Forgot password?");
        forgot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        forgot.setForeground(LINK);
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(login.this,
                        "Use your assigned name and password,\n"
                        + "or contact your administrator for help.",
                        "Forgot Password",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        row.add(remember, BorderLayout.WEST);
        row.add(forgot, BorderLayout.EAST);
        return row;
    }

    private JButton buildLoginButton() {
        JButton loginButton = new JButton("Login") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? NAVY.brighter() : NAVY);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        loginButton.setAlignmentX(LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(380, 46));
        loginButton.setPreferredSize(new Dimension(380, 46));
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setContentAreaFilled(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> attemptLogin());
        return loginButton;
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordField.setEchoChar(passwordVisible ? (char) 0 : '\u2022');
        eyeToggle.setIcon(passwordVisible ? LoginIcons.eyeOpen(18) : LoginIcons.eyeClosed(18));
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        if (USER_PLACEHOLDER.equals(username)) {
            username = "";
        }

        String password;
        if (passwordPlaceholder) {
            password = "";
        } else {
            password = new String(passwordField.getPassword());
        }

        if (username.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Login Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (LoginAuth.authenticate(username, password)) {
            new home().setVisible(true);
            dispose();
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Incorrect credentials. Please review your username and password,\n"
                        + "then contact your administrator if you need help.",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE);
        if (!passwordPlaceholder) {
            passwordField.setText("");
        }
    }
}
