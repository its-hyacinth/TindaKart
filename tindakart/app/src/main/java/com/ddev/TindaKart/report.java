/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.ddev.TindaKart;

import components.ImageScaler;
import java.net.URL;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author W10
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.JOptionPane;
import java.util.Date;

public class report extends javax.swing.JPanel {

    /**
     * Creates new form report
     */
    Date today = new Date();
    ImageScaler scaler = new ImageScaler();

    public report() {
        initComponents();
        populateSalesSummaryReport(today);
        calculateTotalSales();
        populateMonthlySalesSummaryReport(today);
        calculateTotalMonthlySales();

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy"); // For the month-based title (e.g., January 2025)
        SimpleDateFormat dayFormat = new SimpleDateFormat("MMMM dd");
        jLabel1.setText("Daily Sales Summary - " + dayFormat.format(today));
        jLabel3.setText("Monthly Sales Summary - " + monthFormat.format(today));
        URL imageUrl1 = getClass().getResource("/icons/chart-bard.png");
        scaler.scaleImage(jLabel8, imageUrl1);
        URL imageUrl2 = getClass().getResource("/icons/chart-bard.png");
        scaler.scaleImage(jLabel9, imageUrl2);
    }

    private void populateSalesSummaryReport(Date selectedDate) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);  // Clear existing rows

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); // Use the exact format to match the selected date

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Format the selected date into the required format (yyyy-MM-dd)
            String formattedDate = dateFormat.format(selectedDate);

            // Query to get sales summary (grouped by product and filtered by sale date)
            String sql = "SELECT \n"
                    + "    i.name AS product_name,\n"
                    + "    SUM(s.quantity) AS total_quantity_sold,\n"
                    + "    SUM(s.total_price) AS total_sales\n"
                    + "FROM \n"
                    + "    public.sales s\n"
                    + "JOIN \n"
                    + "    public.inventory i ON s.inventory_product_id = i.id\n"
                    + "WHERE \n"
                    + "    s.sale_date::date = ?::date  -- Use prepared statement for date\n"
                    + "GROUP BY \n"
                    + "    i.name\n"
                    + "ORDER BY \n"
                    + "    total_sales DESC;";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, formattedDate);  // Set the formatted date parameter
            ResultSet rs = pstmt.executeQuery();

            // Populate the table with the summarized sales data
            while (rs.next()) {
                String productName = rs.getString("product_name");
                int totalQuantity = rs.getInt("total_quantity_sold");
                double totalSales = rs.getDouble("total_sales");

                // Add a row to the table
                model.addRow(new Object[]{productName, totalQuantity, "P" + totalSales});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading sales summary!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateMonthlySalesSummaryReport(Date today) {
        DefaultTableModel model = (DefaultTableModel) jTable3.getModel();
        model.setRowCount(0);  // Clear existing rows

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get the current month's first day and the last day
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(today);

            // Set the first day of the month (start date)
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date startDate = calendar.getTime();

            // Set the last day of the month (end date)
            calendar.add(Calendar.MONTH, 1);  // Move to the next month
            calendar.set(Calendar.DAY_OF_MONTH, 1);  // Set the day to the 1st of the next month
            calendar.add(Calendar.DAY_OF_MONTH, -1); // Go back to the last day of the current month
            Date endDate = calendar.getTime();

            // Format the start and end dates into the required format (yyyy-MM-dd)
            String formattedStartDate = dateFormat.format(startDate);
            String formattedEndDate = dateFormat.format(endDate);

            // Query to get monthly sales summary (grouped by product, year, and month)
            String sql = "SELECT \n"
                    + "    i.name AS product_name,\n"
                    + "    EXTRACT(YEAR FROM s.sale_date) AS sale_year,\n"
                    + "    EXTRACT(MONTH FROM s.sale_date) AS sale_month,\n"
                    + "    SUM(s.quantity) AS total_quantity_sold,\n"
                    + "    SUM(s.total_price) AS total_sales\n"
                    + "FROM \n"
                    + "    public.sales s\n"
                    + "JOIN \n"
                    + "    public.inventory i ON s.inventory_product_id = i.id\n"
                    + "WHERE \n"
                    + "    s.sale_date >= ?::DATE\n"
                    + "    AND s.sale_date < ?::DATE\n"
                    + "GROUP BY \n"
                    + "    i.name, sale_year, sale_month\n"
                    + "ORDER BY \n"
                    + "    sale_year DESC, sale_month DESC, total_sales DESC;";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, formattedStartDate);  // Set the start date parameter
            pstmt.setString(2, formattedEndDate);    // Set the end date parameter
            ResultSet rs = pstmt.executeQuery();

            // Populate the table with the summarized monthly sales data
            while (rs.next()) {
                String productName = rs.getString("product_name");
                int saleYear = rs.getInt("sale_year");
                int saleMonth = rs.getInt("sale_month");
                int totalQuantity = rs.getInt("total_quantity_sold");
                double totalSales = rs.getDouble("total_sales");

                // Add a row to the table
                model.addRow(new Object[]{productName, saleYear, saleMonth, totalQuantity, "P" + totalSales});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading monthly sales summary!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateTotalSales() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        double totalSales = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            Object value = model.getValueAt(i, 2); // Column index 2 is "Total Sales"
            if (value != null) {
                try {
                    totalSales += Double.parseDouble(value.toString().replace("P", ""));
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid number format at row " + i + ": " + value);
                }
            }
        }

        // Update the label with the total daily sales
        total_sales.setText("Total Daily Sales: " + String.format("P%.2f", totalSales));
    }

    // Calculate total monthly sales
    private void calculateTotalMonthlySales() {
        DefaultTableModel model = (DefaultTableModel) jTable3.getModel();
        double totalSales = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            Object value = model.getValueAt(i, 4); // Column index 4 is "Total Sales"
            if (value != null) {
                try {
                    totalSales += Double.parseDouble(value.toString().replace("P", ""));
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid number format at row " + i + ": " + value);
                }
            }
        }

        // Update the label with the total monthly sales
        total_sales2.setText("Total Monthly Sales: " + String.format("P%.2f", totalSales));
    }

    private void setupTableListener() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.addTableModelListener(e -> calculateTotalSales());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel4 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        total_sales = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        myButtonborderless3 = new components.MyButtonborderless();
        jLabel8 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        total_sales2 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();
        jPanel12 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        myButtonborderless4 = new components.MyButtonborderless();
        jLabel9 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new java.awt.GridLayout(1, 0, 10, 0));

        jPanel4.setPreferredSize(new java.awt.Dimension(500, 100));
        jPanel4.setLayout(new java.awt.BorderLayout());

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        total_sales.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        total_sales.setText("Total Daily Sales: ##");
        jPanel2.add(total_sales);

        jPanel4.add(jPanel2, java.awt.BorderLayout.PAGE_END);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Product", "Quantity Sold", "Total Sales"
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

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setPreferredSize(new java.awt.Dimension(200, 100));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Daily Sales Summary");

        myButtonborderless3.setBackground(new java.awt.Color(51, 54, 82));
        myButtonborderless3.setForeground(new java.awt.Color(255, 255, 255));
        myButtonborderless3.setText("Refresh");
        myButtonborderless3.setPreferredSize(new java.awt.Dimension(150, 30));
        myButtonborderless3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myButtonborderless3ActionPerformed(evt);
            }
        });

        jLabel8.setPreferredSize(new java.awt.Dimension(25, 27));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(myButtonborderless3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 350, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(myButtonborderless3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel3, java.awt.BorderLayout.PAGE_START);

        jPanel4.add(jPanel1, java.awt.BorderLayout.CENTER);

        add(jPanel4);

        jPanel5.setPreferredSize(new java.awt.Dimension(500, 100));
        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel10.setBackground(new java.awt.Color(255, 255, 255));
        jPanel10.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        total_sales2.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        total_sales2.setText("Total Daily Sales: ##");
        jPanel10.add(total_sales2);

        jPanel5.add(jPanel10, java.awt.BorderLayout.PAGE_END);

        jPanel11.setBackground(new java.awt.Color(255, 255, 255));
        jPanel11.setLayout(new java.awt.BorderLayout());

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Product", "Sale Year", "Sale Month", "Total Quantity Sold", "Total Sales"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, true, false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(jTable3);

        jPanel11.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jPanel12.setBackground(new java.awt.Color(255, 255, 255));
        jPanel12.setPreferredSize(new java.awt.Dimension(200, 100));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel3.setText("Monthly Sales Summary");

        myButtonborderless4.setBackground(new java.awt.Color(51, 54, 82));
        myButtonborderless4.setForeground(new java.awt.Color(255, 255, 255));
        myButtonborderless4.setText("Refresh");
        myButtonborderless4.setPreferredSize(new java.awt.Dimension(150, 30));
        myButtonborderless4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myButtonborderless4ActionPerformed(evt);
            }
        });

        jLabel9.setPreferredSize(new java.awt.Dimension(25, 27));

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addComponent(myButtonborderless4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addContainerGap(176, Short.MAX_VALUE))
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                .addComponent(myButtonborderless4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel11.add(jPanel12, java.awt.BorderLayout.PAGE_START);

        jPanel5.add(jPanel11, java.awt.BorderLayout.CENTER);

        add(jPanel5);
    }// </editor-fold>//GEN-END:initComponents

    private void myButtonborderless3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_myButtonborderless3ActionPerformed
        // TODO add your handling code here:
        populateSalesSummaryReport(today);
        calculateTotalSales();
    }//GEN-LAST:event_myButtonborderless3ActionPerformed

    private void myButtonborderless4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_myButtonborderless4ActionPerformed
        // TODO add your handling code here:
        populateMonthlySalesSummaryReport(today);
        calculateTotalMonthlySales();
    }//GEN-LAST:event_myButtonborderless4ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable3;
    private components.MyButtonborderless myButtonborderless3;
    private components.MyButtonborderless myButtonborderless4;
    private javax.swing.JLabel total_sales;
    private javax.swing.JLabel total_sales2;
    // End of variables declaration//GEN-END:variables
}
