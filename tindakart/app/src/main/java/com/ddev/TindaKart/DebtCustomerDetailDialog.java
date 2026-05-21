package com.ddev.TindaKart;

import components.RoundedPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Customer profile dialog matching the TindaKart mockup layout.
 */
public class DebtCustomerDetailDialog extends JDialog {

    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color TEXT_DARK = new Color(17, 24, 39);
    private static final Color TEXT_MUTED = new Color(107, 114, 128);
    private static final Color BORDER = new Color(229, 231, 235);
    private static final Color SMS_BG = new Color(249, 250, 251);

    private final int debtId;
    private final String originalNickname;
    private final Date debtDate;
    private final Runnable onSaved;
    private final int initialCreditScore;

    private final JTextField fullNameField;
    private final JTextField phoneField;
    private final JTextField emailField;
    private final JTextField addressField;
    private final JTextField debtAmountField;
    private final JLabel debtDateLabel;
    private final JTextArea smsArea;
    private final CreditScorePieChart creditChart;

    private boolean dirty;

    public DebtCustomerDetailDialog(Window owner, int debtId, String nickname, double debtAmount,
            Date debtDate, Runnable onSaved) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        this.debtId = debtId;
        this.originalNickname = nickname;
        this.debtDate = debtDate;
        this.onSaved = onSaved;

        DebtCustomerProfile profile = DebtCustomerDirectory.resolve(nickname);
        this.initialCreditScore = profile.creditScore();

        fullNameField = valueField(profile.fullName(), 20, Font.BOLD);
        phoneField = valueField(profile.phone(), 14, Font.PLAIN);
        emailField = valueField(profile.email(), 14, Font.PLAIN);
        addressField = valueField(profile.address(), 14, Font.PLAIN);
        debtAmountField = valueField(formatPeso(debtAmount), 14, Font.BOLD);

        String dateText = debtDate != null
                ? new SimpleDateFormat("MMMM dd, yyyy").format(debtDate)
                : "—";
        debtDateLabel = new JLabel(dateText);
        debtDateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        debtDateLabel.setForeground(TEXT_DARK);

        creditChart = new CreditScorePieChart(profile.creditScore(), true);
        smsArea = new JTextArea(buildDefaultSmsMessage(profile.fullName(), debtAmount));

        markDirtyOnEdit();

        initUi();
        setSize(640, 680);
        setLocationRelativeTo(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveIfDirty(false);
            }
        });

        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        am.put("close", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveIfDirty(false);
                dispose();
            }
        });
    }

    private void initUi() {
        JPanel shell = new RoundedPanel(14, Color.WHITE);
        shell.setLayout(new BorderLayout());
        shell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(0, 0, 0, 0)));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        root.add(buildTitleBar(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(buildProfileHeader());
        body.add(Box.createVerticalStrut(20));
        body.add(buildDetailsSection());
        body.add(Box.createVerticalStrut(18));
        body.add(buildSmsSection());
        body.add(Box.createVerticalStrut(20));
        body.add(buildFooter());

        root.add(body, BorderLayout.CENTER);
        shell.add(root, BorderLayout.CENTER);
        setContentPane(shell);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JLabel title = new JLabel("Customer Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_DARK);

        JButton closeX = new JButton("✕");
        closeX.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        closeX.setForeground(TEXT_MUTED);
        closeX.setBorderPainted(false);
        closeX.setContentAreaFilled(false);
        closeX.setFocusPainted(false);
        closeX.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeX.addActionListener(e -> {
            saveIfDirty(false);
            dispose();
        });

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(0, 0, 14, 0));
        inner.add(title, BorderLayout.WEST);
        inner.add(closeX, BorderLayout.EAST);
        bar.add(inner, BorderLayout.CENTER);
        return bar;
    }

    private JPanel buildProfileHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel left = new JPanel(new BorderLayout(14, 0));
        left.setOpaque(false);
        left.add(new AnonymousAvatarPanel(), BorderLayout.WEST);

        JPanel nameCol = new JPanel();
        nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
        nameCol.setOpaque(false);
        fullNameField.setMaximumSize(new Dimension(400, 36));
        nameCol.add(fullNameField);
        nameCol.add(Box.createVerticalStrut(10));
        nameCol.add(buildCreditScoreCard());
        left.add(nameCol, BorderLayout.CENTER);

        header.add(left, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildCreditScoreCard() {
        RoundedPanel card = new RoundedPanel(10, Color.WHITE);
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(4, 8, 4, 12)));
        card.setMaximumSize(new Dimension(340, 88));

        card.add(creditChart);

        JPanel meta = new JPanel();
        meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
        meta.setOpaque(false);
        meta.add(new RiskBadgePanel(creditChart.getScore()));
        meta.add(Box.createVerticalStrut(4));
        JLabel hint = new JLabel("Higher score = lower risk");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(TEXT_MUTED);
        meta.add(hint);

        card.add(meta);
        return card;
    }

    private JPanel buildDetailsSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        section.add(sectionHeader(ProfileIcons.section(22, ProfileIcons.Section.USER), "Customer Details"));
        section.add(Box.createVerticalStrut(10));
        section.add(buildDetailsCard());
        return section;
    }

    private JPanel buildDetailsCard() {
        RoundedPanel card = new RoundedPanel(12, Color.WHITE);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(16, 16, 16, 16)));
        card.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(0, 8, 14, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        card.add(detailCell(ProfileIcons.field(34, ProfileIcons.Field.PHONE), "Phone", phoneField), gbc);

        gbc.gridx = 1;
        card.add(detailCell(ProfileIcons.field(34, ProfileIcons.Field.ADDRESS), "Address", addressField), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        card.add(detailCell(ProfileIcons.field(34, ProfileIcons.Field.EMAIL), "Email", emailField), gbc);

        gbc.gridx = 1;
        card.add(detailCell(ProfileIcons.field(34, ProfileIcons.Field.WALLET), "Outstanding Debt", debtAmountField), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(4, 8, 12, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel divider = new JPanel();
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(10, 1));
        divider.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        card.add(divider, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 8, 0, 8);
        card.add(detailCell(ProfileIcons.field(34, ProfileIcons.Field.CALENDAR), "Debt Date", debtDateLabel), gbc);

        return card;
    }

    private JPanel buildSmsSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        section.add(sectionHeader(ProfileIcons.section(22, ProfileIcons.Section.MESSAGE), "Payment Reminder SMS"));
        section.add(Box.createVerticalStrut(10));

        RoundedPanel box = new RoundedPanel(10, SMS_BG);
        box.setLayout(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        box.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        smsArea.setLineWrap(true);
        smsArea.setWrapStyleWord(true);
        smsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        smsArea.setForeground(TEXT_DARK);
        smsArea.setBackground(SMS_BG);
        smsArea.setBorder(BorderFactory.createEmptyBorder());
        box.add(smsArea, BorderLayout.CENTER);

        section.add(box);
        return section;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        footer.setOpaque(false);
        footer.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JButton cancel = new JButton("Cancel");
        styleSecondaryButton(cancel);
        cancel.addActionListener(e -> {
            saveIfDirty(false);
            dispose();
        });

        JButton sendSms = new JButton("  Send SMS Reminder");
        sendSms.setIcon(ProfileIcons.section(18, ProfileIcons.Section.SEND));
        stylePrimaryButton(sendSms);
        sendSms.addActionListener(e -> sendSmsReminder());

        footer.add(cancel);
        footer.add(sendSms);
        return footer;
    }

    private JPanel sectionHeader(ImageIcon icon, String title) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        JLabel iconLabel = new JLabel(icon);
        JLabel text = new JLabel(title);
        text.setFont(new Font("Segoe UI", Font.BOLD, 15));
        text.setForeground(TEXT_DARK);
        row.add(iconLabel);
        row.add(text);
        return row;
    }

    private JPanel detailCell(ImageIcon icon, String label, JComponent value) {
        JPanel cell = new JPanel(new BorderLayout(10, 0));
        cell.setOpaque(false);
        cell.add(new JLabel(icon), BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel key = new JLabel(label);
        key.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        key.setForeground(TEXT_MUTED);
        text.add(key);
        text.add(Box.createVerticalStrut(2));
        if (value instanceof JTextField tf) {
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        }
        text.add(value);
        cell.add(text, BorderLayout.CENTER);
        return cell;
    }

    private JTextField valueField(String text, int size, int style) {
        JTextField field = new JTextField(text);
        field.setFont(new Font("Segoe UI", style, size));
        field.setForeground(TEXT_DARK);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 0, 0, 0)));
        field.setCaretColor(PRIMARY);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, PRIMARY));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 0, 0, 0)));
                saveIfDirty(false);
            }
        });
        return field;
    }

    private void stylePrimaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(200, 40));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    private void styleSecondaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setForeground(TEXT_DARK);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 20, 8, 20)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 40));
    }

    private void markDirtyOnEdit() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                dirty = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                dirty = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                dirty = true;
            }
        };
        for (JTextField field : new JTextField[] {
                fullNameField, phoneField, emailField, addressField, debtAmountField }) {
            field.getDocument().addDocumentListener(listener);
        }
        smsArea.getDocument().addDocumentListener(listener);
    }

    private void saveIfDirty(boolean showSuccess) {
        if (!dirty) {
            return;
        }
        String fullName = fullNameField.getText().trim();
        if (fullName.isEmpty()) {
            return;
        }
        String email = emailField.getText().trim();
        if (!CebuLocalities.isGmailAddress(email)) {
            if (showSuccess) {
                JOptionPane.showMessageDialog(this,
                        "Email must be a Gmail address ending with @gmail.com",
                        "Invalid email",
                        JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        double amount = parseAmount(debtAmountField.getText());
        if (amount < 0) {
            return;
        }

        DebtCustomerProfile profile = new DebtCustomerProfile(
                fullName,
                phoneField.getText().trim(),
                email,
                initialCreditScore,
                addressField.getText().trim(),
                "");

        try {
            DebtCustomerRepository.renameAndSave(originalNickname, fullName, profile);
            DebtCustomerRepository.updateDebtRecord(debtId, fullName, amount);
            refreshSmsMessage(fullName, amount);
            if (onSaved != null) {
                onSaved.run();
            }
            dirty = false;
            if (showSuccess) {
                JOptionPane.showMessageDialog(this,
                        "Customer information saved.",
                        "Saved",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static double parseAmount(String raw) {
        String cleaned = raw.replace("₱", "").replace("P", "").replace(",", "").trim();
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String formatPeso(double amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return "₱" + df.format(amount);
    }

    private void refreshSmsMessage(String name, double amount) {
        String dateText = debtDate != null
                ? new SimpleDateFormat("MMMM dd, yyyy").format(debtDate)
                : "recently";
        smsArea.setText("Hi " + name + ", this is TindaKart. Friendly reminder: you have an outstanding "
                + "balance of " + formatPeso(amount) + " from " + dateText
                + ". Please settle when convenient. Thank you!");
    }

    private String buildDefaultSmsMessage(String name, double amount) {
        String dateText = debtDate != null
                ? new SimpleDateFormat("MMMM dd, yyyy").format(debtDate)
                : "recently";
        return "Hi " + name + ", this is TindaKart. Friendly reminder: you have an outstanding balance of "
                + formatPeso(amount) + " from " + dateText
                + ". Please settle when convenient. Thank you!";
    }

    private void sendSmsReminder() {
        saveIfDirty(false);
        String phone = phoneField.getText().replaceAll("[^0-9+]", "");
        String body = smsArea.getText().trim();
        if (body.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a message to send.", "SMS",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add a phone number before sending SMS.", "SMS",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                String encoded = URLEncoder.encode(body, StandardCharsets.UTF_8);
                URI uri = URI.create("sms:" + phone + "?body=" + encoded);
                Desktop.getDesktop().browse(uri);
            } else {
                JOptionPane.showMessageDialog(this,
                        "SMS app could not be opened.\n\nTo: " + phone + "\n\n" + body,
                        "SMS Reminder",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not open SMS app.\n\nTo: " + phone + "\n\n" + body,
                    "SMS Reminder",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
