package com.ddev.TindaKart;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URL;
import javax.swing.UIManager;

public class App {

    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        // Set the look and feel for the application
        FlatLightLaf.setup();

        try {
            // Set the look and feel to FlatLight
            UIManager.setLookAndFeel(new FlatLightLaf());

            // Load the Poppins font
            URL fontURL = App.class.getResource("/fonts/Poppins-Regular.ttf");

            try {
                if (fontURL != null) {
                    // Load the font
                    Font poppinsFont = Font.createFont(Font.TRUETYPE_FONT, fontURL.openStream()).deriveFont(12f);

                    // Register the font to the GraphicsEnvironment
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    ge.registerFont(poppinsFont);

                    // Apply the Poppins font to all UI components
                    UIManager.put("Button.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("ToggleButton.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("RadioButton.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("CheckBox.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("ColorChooser.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("ComboBox.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("Label.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("List.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("MenuBar.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("MenuItem.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("RadioButtonMenuItem.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("CheckBoxMenuItem.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("Menu.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("PopupMenu.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("OptionPane.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("Panel.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("ProgressBar.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("ScrollPane.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("Viewport.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("TabbedPane.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("Table.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("TableHeader.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("TextField.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("PasswordField.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("TextArea.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("TextPane.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("EditorPane.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("TitledBorder.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("ToolBar.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("ToolTip.font", new Font("Poppins", Font.PLAIN, 12));
                    UIManager.put("Tree.font", new Font("Poppins", Font.PLAIN, 12));
                } else {
                    System.err.println("Font file not found!");
                }
            } catch (FontFormatException | IOException e) {
                e.printStackTrace();
                System.err.println("Error loading font.");
            }

        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
            ex.printStackTrace();
        }

        // Show splash screen (if any)
        new splash().setVisible(true);
    }
}
