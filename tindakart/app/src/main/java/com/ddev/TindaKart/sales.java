/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.ddev.TindaKart;

/**
 *
 * @author W10
 */
import components.ImageScaler;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

class CartItem {

    String productName;
    int quantity;
    double totalPrice;

    public CartItem(String productName, int quantity, double totalPrice) {
        this.productName = productName;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    // Getters and setters for CartItem properties
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}

public class sales extends javax.swing.JPanel {

    /**
     * Creates new form sales
     */
    private List<CartItem> cartItems = new ArrayList<>();
    ImageScaler scaler = new ImageScaler();

    public sales() {
        initComponents();
        populateProductComboBox();
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{}, // Initial empty rows
                new String[]{"Product Name", "Quantity", "Total Price"} // Column names
        );
        URL imageUrl1 = getClass().getResource("/icons/receiptd.png");
        scaler.scaleImage(jLabel8, imageUrl1);

        jTable1.setModel(model);
//        populateSalesTable();
    }

    private void addToCart(java.awt.event.ActionEvent evt) {
        String selectedProduct = (String) product_name.getSelectedItem();
        String quantityStr = quantityField.getText();

        // Validate input
        if (selectedProduct == null || selectedProduct.isEmpty() || quantityStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Product and quantity are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than 0!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double price = getProductPrice(selectedProduct);
            if (price <= 0) {
                JOptionPane.showMessageDialog(this, "Invalid product price!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Add item to the cart
            double totalPrice = price * quantity;
            cartItems.add(new CartItem(selectedProduct, quantity, totalPrice));

            // Update the table with the cart items
            updateCartTable();

            // Reset input fields
            product_name.setSelectedIndex(0); // Reset combo box
            quantityField.setText(""); // Reset quantity field

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid quantity!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processPayment(java.awt.event.ActionEvent evt) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            // Validate inventory quantities before processing
            for (CartItem item : cartItems) {
                String checkStockSQL = "SELECT quantity FROM inventory WHERE name = ?";
                PreparedStatement checkStockStmt = conn.prepareStatement(checkStockSQL);
                checkStockStmt.setString(1, item.getProductName());
                ResultSet rs = checkStockStmt.executeQuery();

                if (rs.next()) {
                    int availableStock = rs.getInt("quantity");
                    if (item.getQuantity() > availableStock) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Insufficient stock for product: " + item.getProductName(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        conn.rollback(); // Roll back the transaction
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "Product not found in inventory: " + item.getProductName(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    conn.rollback(); // Roll back the transaction
                    return;
                }
            }

            // Process payment and update inventory
            for (CartItem item : cartItems) {
                // Deduct stock
                String updateStockSQL = "UPDATE inventory SET quantity = quantity - ? WHERE name = ?";
                PreparedStatement updateStockStmt = conn.prepareStatement(updateStockSQL);
                updateStockStmt.setInt(1, item.getQuantity());
                updateStockStmt.setString(2, item.getProductName());
                updateStockStmt.executeUpdate();

                // Save sale record
                String insertSaleSQL = "INSERT INTO sales (inventory_product_id, quantity, total_price, sale_date) "
                        + "VALUES ((SELECT id FROM inventory WHERE name = ?), ?, ?, ?)";
                PreparedStatement insertSaleStmt = conn.prepareStatement(insertSaleSQL);
                insertSaleStmt.setString(1, item.getProductName());
                insertSaleStmt.setInt(2, item.getQuantity());
                insertSaleStmt.setDouble(3, item.getTotalPrice());
                insertSaleStmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                insertSaleStmt.executeUpdate();

                // Check the remaining stock
                String checkStockSQL = "SELECT quantity FROM inventory WHERE name = ?";
                try (PreparedStatement checkStockStmt = conn.prepareStatement(checkStockSQL)) {
                    checkStockStmt.setString(1, item.getProductName());
                    try (ResultSet rs = checkStockStmt.executeQuery()) {
                        if (rs.next()) {
                            int remainingStock = rs.getInt("quantity");
                            if (remainingStock <= 0) {
                                // Update status to "Out of Stock"
                                String updateStatusSQL = "UPDATE inventory SET status = 'Out of Stock' WHERE name = ?";
                                try (PreparedStatement updateStatusStmt = conn.prepareStatement(updateStatusSQL)) {
                                    updateStatusStmt.setString(1, item.getProductName());
                                    updateStatusStmt.executeUpdate();
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }

            conn.commit(); // Commit transaction
            cartItems.clear(); // Clear cart
            updateCartTable(); // Refresh table

            JOptionPane.showMessageDialog(this, "Payment processed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error processing payment: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        roundedPanelBorderless3 = new components.RoundedPanelBorderless();
        product_name = new javax.swing.JComboBox<>();
        jPanel6 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        roundedPanelBorderless2 = new components.RoundedPanelBorderless();
        quantityField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        myButtonborderless1 = new components.MyButtonborderless();
        myButtonborderless2 = new components.MyButtonborderless();
        myButtonborderless3 = new components.MyButtonborderless();
        myButtonborderless4 = new components.MyButtonborderless();
        jPanel8 = new javax.swing.JPanel();
        totalPriceLabel = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new java.awt.BorderLayout());

        jPanel2.setPreferredSize(new java.awt.Dimension(300, 126));
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Sales Transactions");

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setLayout(new java.awt.GridLayout(1, 0, 10, 0));

        jPanel9.setBackground(new java.awt.Color(255, 255, 255));
        jPanel9.setOpaque(false);

        jLabel3.setText("Product Name");

        roundedPanelBorderless3.setBackground(new java.awt.Color(255, 255, 255));

        product_name.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        product_name.setBorder(null);

        javax.swing.GroupLayout roundedPanelBorderless3Layout = new javax.swing.GroupLayout(roundedPanelBorderless3);
        roundedPanelBorderless3.setLayout(roundedPanelBorderless3Layout);
        roundedPanelBorderless3Layout.setHorizontalGroup(
            roundedPanelBorderless3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanelBorderless3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(product_name, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        roundedPanelBorderless3Layout.setVerticalGroup(
            roundedPanelBorderless3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanelBorderless3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(product_name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(8, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(0, 206, Short.MAX_VALUE))
                    .addComponent(roundedPanelBorderless3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addGap(0, 0, 0)
                .addComponent(roundedPanelBorderless3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel4.add(jPanel9);

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setOpaque(false);

        jLabel2.setText("Quantity");

        roundedPanelBorderless2.setBackground(new java.awt.Color(255, 255, 255));

        quantityField.setToolTipText("Quantity");
        quantityField.setBorder(null);

        javax.swing.GroupLayout roundedPanelBorderless2Layout = new javax.swing.GroupLayout(roundedPanelBorderless2);
        roundedPanelBorderless2.setLayout(roundedPanelBorderless2Layout);
        roundedPanelBorderless2Layout.setHorizontalGroup(
            roundedPanelBorderless2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanelBorderless2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(quantityField)
                .addContainerGap())
        );
        roundedPanelBorderless2Layout.setVerticalGroup(
            roundedPanelBorderless2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roundedPanelBorderless2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(quantityField, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(0, 237, Short.MAX_VALUE))
                    .addComponent(roundedPanelBorderless2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addGap(0, 0, 0)
                .addComponent(roundedPanelBorderless2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel4.add(jPanel6);

        jLabel8.setPreferredSize(new java.awt.Dimension(25, 27));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 601, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel1)))
                .addContainerGap(46, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel1, java.awt.BorderLayout.CENTER);

        add(jPanel2, java.awt.BorderLayout.PAGE_START);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setLayout(new java.awt.BorderLayout());

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Product", "Quantity", "Total Price"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jPanel3.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setPreferredSize(new java.awt.Dimension(653, 40));
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        myButtonborderless1.setBackground(new java.awt.Color(51, 54, 82));
        myButtonborderless1.setForeground(new java.awt.Color(255, 255, 255));
        myButtonborderless1.setText("Add to cart");
        myButtonborderless1.setPreferredSize(new java.awt.Dimension(150, 30));
        myButtonborderless1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myButtonborderless1ActionPerformed(evt);
            }
        });
        jPanel5.add(myButtonborderless1);

        myButtonborderless2.setBackground(new java.awt.Color(51, 54, 82));
        myButtonborderless2.setForeground(new java.awt.Color(255, 255, 255));
        myButtonborderless2.setText("Process payment");
        myButtonborderless2.setPreferredSize(new java.awt.Dimension(150, 30));
        myButtonborderless2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myButtonborderless2ActionPerformed(evt);
            }
        });
        jPanel5.add(myButtonborderless2);

        myButtonborderless3.setBackground(new java.awt.Color(51, 54, 82));
        myButtonborderless3.setForeground(new java.awt.Color(255, 255, 255));
        myButtonborderless3.setText("Refresh");
        myButtonborderless3.setPreferredSize(new java.awt.Dimension(150, 30));
        myButtonborderless3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myButtonborderless3ActionPerformed(evt);
            }
        });
        jPanel5.add(myButtonborderless3);

        myButtonborderless4.setBackground(new java.awt.Color(255, 51, 51));
        myButtonborderless4.setForeground(new java.awt.Color(255, 255, 255));
        myButtonborderless4.setText("Delete");
        myButtonborderless4.setDestructive(true);
        myButtonborderless4.setPreferredSize(new java.awt.Dimension(72, 30));
        myButtonborderless4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myButtonborderless4ActionPerformed(evt);
            }
        });
        jPanel5.add(myButtonborderless4);

        jPanel3.add(jPanel5, java.awt.BorderLayout.PAGE_START);

        add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanel8.setBackground(new java.awt.Color(255, 255, 255));
        jPanel8.setPreferredSize(new java.awt.Dimension(653, 50));

        totalPriceLabel.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        totalPriceLabel.setText("Total Price: P0.00");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addContainerGap(423, Short.MAX_VALUE)
                .addComponent(totalPriceLabel)
                .addGap(30, 30, 30))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(totalPriceLabel)
                .addContainerGap(12, Short.MAX_VALUE))
        );

        add(jPanel8, java.awt.BorderLayout.PAGE_END);
    }// </editor-fold>//GEN-END:initComponents

    private void myButtonborderless1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_myButtonborderless1ActionPerformed
        // TODO add your handling code here:
        addToCart(evt);
    }//GEN-LAST:event_myButtonborderless1ActionPerformed

    private void myButtonborderless2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_myButtonborderless2ActionPerformed
        // TODO add your handling code here:
        processPayment(evt);
    }//GEN-LAST:event_myButtonborderless2ActionPerformed

    private void myButtonborderless3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_myButtonborderless3ActionPerformed
        // TODO add your handling code here:
        populateProductComboBox();
    }//GEN-LAST:event_myButtonborderless3ActionPerformed

    private void myButtonborderless4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_myButtonborderless4ActionPerformed
        // TODO add your handling code here:
        deleteFromCart();
    }//GEN-LAST:event_myButtonborderless4ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private components.MyButtonborderless myButtonborderless1;
    private components.MyButtonborderless myButtonborderless2;
    private components.MyButtonborderless myButtonborderless3;
    private components.MyButtonborderless myButtonborderless4;
    private javax.swing.JComboBox<String> product_name;
    private javax.swing.JTextField quantityField;
    private components.RoundedPanelBorderless roundedPanelBorderless2;
    private components.RoundedPanelBorderless roundedPanelBorderless3;
    private javax.swing.JLabel totalPriceLabel;
    // End of variables declaration//GEN-END:variables
private void populateProductComboBox() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Clear the combo box
            product_name.removeAllItems();

            // Modify the SQL query to filter only items with 'In Stock' status
            String sql = "SELECT name FROM inventory WHERE status = 'In Stock'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            // Add a placeholder for selection (optional)
            product_name.addItem("-- Select Product --");

            while (rs.next()) {
                product_name.addItem(rs.getString("name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading products: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateSalesTable() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);  // Clear existing rows

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT s.id, i.name, s.quantity, s.total_price, s.sale_date "
                    + "FROM sales s JOIN inventory i ON s.inventory_product_id = i.id";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            // Populate the table with the fetched data
            while (rs.next()) {
                String productName = rs.getString("name");
                int quantity = rs.getInt("quantity");
                double totalPrice = rs.getDouble("total_price");
                Timestamp saleDate = rs.getTimestamp("sale_date");
                String formattedSaleDate = (saleDate != null) ? dateFormat.format(new Date(saleDate.getTime())) : "";

                // Add a row to the table
                model.addRow(new Object[]{productName, quantity, "P" + totalPrice, formattedSaleDate});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading sales data!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteFromCart() {
        // Get the selected row index in the cart table
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a cart item to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get the product name from the selected row (assuming the product name is in the first column)
        String selectedProduct = (String) jTable1.getValueAt(selectedRow, 0); // Adjust index if needed
        if (selectedProduct == null || selectedProduct.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invalid cart item selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Find and remove the corresponding item from the cart list
        CartItem itemToRemove = null;
        for (CartItem item : cartItems) {
            if (item.getProductName().equals(selectedProduct)) {
                itemToRemove = item;
                break;
            }
        }

        // If the item was found, remove it
        if (itemToRemove != null) {
            cartItems.remove(itemToRemove);

            // Update the total price label
            updateTotalPriceLabel();

            // Refresh the cart display (this would likely involve refreshing the JTable or list view)
            updateCartTable(); // Implement this to refresh the cart table

            JOptionPane.showMessageDialog(this, "Item removed from cart.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Item not found in cart.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCartTable() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0); // Clear existing rows
        updateTotalPriceLabel();
        for (CartItem item : cartItems) {
            model.addRow(new Object[]{item.productName, item.quantity, item.totalPrice, ""});
        }
    }

    private double getProductPrice(String productName) {
        double price = 0;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT price FROM inventory WHERE name = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, productName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                price = rs.getDouble("price");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return price;
    }

    private void saveSaleToDatabase(CartItem item) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            // Update stock in inventory
            String updateStockSQL = "UPDATE inventory SET quantity = quantity - ? WHERE name = ?";
            PreparedStatement updateStockStmt = conn.prepareStatement(updateStockSQL);
            updateStockStmt.setInt(1, item.getQuantity());
            updateStockStmt.setString(2, item.getProductName());
            int rowsAffected = updateStockStmt.executeUpdate();

            if (rowsAffected > 0) {
                // Check the remaining stock
                String checkStockSQL = "SELECT quantity FROM inventory WHERE name = ?";
                PreparedStatement checkStockStmt = conn.prepareStatement(checkStockSQL);
                checkStockStmt.setString(1, item.getProductName());
                ResultSet rs = checkStockStmt.executeQuery();

                if (rs.next()) {
                    int remainingStock = rs.getInt("quantity");
                    if (remainingStock <= 0) {
                        // Update status to "Out of Stock"
                        String updateStatusSQL = "UPDATE inventory SET status = 'Out of Stock' WHERE name = ?";
                        PreparedStatement updateStatusStmt = conn.prepareStatement(updateStatusSQL);
                        updateStatusStmt.setString(1, item.getProductName());
                        updateStatusStmt.executeUpdate();
                    }
                }
            }
            // Insert sale into sales table
            String insertSaleSQL = "INSERT INTO sales (inventory_product_id, quantity, total_price) VALUES ((SELECT id FROM inventory WHERE name = ?), ?, ?)";
            PreparedStatement insertSaleStmt = conn.prepareStatement(insertSaleSQL);
            insertSaleStmt.setString(1, item.getProductName());
            insertSaleStmt.setInt(2, item.getQuantity());
            insertSaleStmt.setDouble(3, item.getTotalPrice());
            insertSaleStmt.executeUpdate();

            conn.commit(); // Commit transaction
            JOptionPane.showMessageDialog(this, "Sale saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private int getProductStock(String productName) {
        int stock = 0;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT quantity FROM inventory WHERE name = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, productName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                stock = rs.getInt("quantity");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return stock;
    }

    private void updateTotalPriceLabel() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        totalPriceLabel.setText("Total Price: P" + String.format("%.2f", total));
    }

}
