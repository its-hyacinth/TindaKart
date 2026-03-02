package components;

import java.awt.Dimension;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.Image;
import java.net.URL;

public class ImageScaler {

    public void scaleImage(JLabel jlabel, URL imageUrl) {

        if (jlabel == null || imageUrl == null) {
            // handle the case where jlabel or imageUrl is null
            return;
        }

        // Get the preferred size of the JLabel
        Dimension size = jlabel.getPreferredSize();

        // Create an ImageIcon from the URL
        ImageIcon icon = new ImageIcon(imageUrl);
        Image img = icon.getImage().getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
        
        // Set the scaled image as the JLabel's icon
        ImageIcon scaledIcon = new ImageIcon(img);
        jlabel.setIcon(scaledIcon);
    }
}
