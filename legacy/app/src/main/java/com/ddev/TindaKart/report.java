/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.ddev.TindaKart;

import components.ImageScaler;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author W10
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;

public class report extends javax.swing.JPanel {

    Date today = new Date();
    ImageScaler scaler = new ImageScaler();
    private final Calendar selectedMonth = Calendar.getInstance();

    public report() {
        initComponents();
        URL imageUrl1 = getClass().getResource("/icons/chart-bard.png");
        scaler.scaleImage(jLabel8, imageUrl1);
        URL imageUrl2 = getClass().getResource("/icons/chart-bard.png");
        scaler.scaleImage(jLabel9, imageUrl2);
        URL imageUrl3 = getClass().getResource("/icons/chart-bard.png");
        scaler.scaleImage(jLabel10, imageUrl3);
        configureReportLayout();
        refreshAllReports();
    }

    private void configureReportLayout() {
        setBackground(new Color(245, 245, 245));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        Dimension headerSize = new Dimension(0, 95);
        Dimension footerSize = new Dimension(0, 50);
        fixHeaderPanel(jPanel3, headerSize);
        fixHeaderPanel(jPanel12, headerSize);
        fixHeaderPanel(jPanel15, headerSize);
        fixFooterPanel(jPanel2, footerSize);
        fixFooterPanel(jPanel10, footerSize);
        fixFooterPanel(jPanel13, footerSize);

        styleCardPanel(jPanel4);
        styleCardPanel(jPanel5);
        styleCardPanel(jPanel6);

        configureTable(jTable1);
        configureTable(jTable3);
        configureWeeklyTable(jTable2);
        configureMonthNavigation();
    }

    private void configureMonthNavigation() {
        Dimension arrowSize = new Dimension(28, 26);
        myButtonborderlessPrev.setPreferredSize(arrowSize);
        myButtonborderlessNext.setPreferredSize(arrowSize);
        myButtonborderlessPrev.setMargin(new Insets(0, 0, 0, 0));
        myButtonborderlessNext.setMargin(new Insets(0, 0, 0, 0));

        jLabelMonthNav.setFont(new Font("Segoe UI", Font.BOLD, 12));
        jLabelMonthNav.setPreferredSize(new Dimension(92, 26));

        jPanelMonthNav.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        jPanelMonthNav.setOpaque(false);
        Dimension monthNavSize = new Dimension(168, 30);
        jPanelMonthNav.setPreferredSize(monthNavSize);
        jPanelMonthNav.setMinimumSize(monthNavSize);
        jPanelMonthNav.setMaximumSize(monthNavSize);
    }

    private void fixHeaderPanel(javax.swing.JPanel panel, Dimension size) {
        panel.setPreferredSize(size);
        panel.setMinimumSize(size);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
    }

    private void fixFooterPanel(javax.swing.JPanel panel, Dimension size) {
        panel.setPreferredSize(size);
        panel.setMinimumSize(size);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
    }

    private void styleCardPanel(javax.swing.JPanel panel) {
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
    }

    private void configureTable(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
    }

    private void configureWeeklyTable(JTable table) {
        configureTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setPreferredWidth(70);
        columns.getColumn(1).setPreferredWidth(160);
        columns.getColumn(2).setPreferredWidth(160);
        columns.getColumn(3).setPreferredWidth(140);
        columns.getColumn(4).setPreferredWidth(110);
    }

    private void refreshAllReports() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy");
        SimpleDateFormat dayFormat = new SimpleDateFormat("MMMM dd");
        Date monthDate = selectedMonth.getTime();

        jLabel1.setText("Daily Sales Summary - " + dayFormat.format(today));
        jLabel3.setText("Monthly Sales Summary");
        jLabel4.setText("Weekly Sales Summary - " + monthFormat.format(monthDate));
        jLabelMonthNav.setText(monthFormat.format(monthDate));

        populateSalesSummaryReport(today);
        calculateTotalSales();
        populateMonthlySalesSummaryReport(monthDate);
        calculateTotalMonthlySales();
        populateWeeklySalesSummaryReport(monthDate);
        calculateTotalWeeklySales();
    }

    private void shiftMonth(int delta) {
        selectedMonth.add(Calendar.MONTH, delta);
        refreshAllReports();
    }

    private void populateSalesSummaryReport(Date selectedDate) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try (Connection conn = DatabaseConnection.getConnection()) {
            String formattedDate = dateFormat.format(selectedDate);

            String sql = "SELECT \n"
                    + "    i.name AS product_name,\n"
                    + "    SUM(s.quantity) AS total_quantity_sold,\n"
                    + "    SUM(s.total_price) AS total_sales\n"
                    + "FROM \n"
                    + "    public.sales s\n"
                    + "JOIN \n"
                    + "    public.inventory i ON s.inventory_product_id = i.id\n"
                    + "WHERE \n"
                    + "    s.sale_date::date = ?::date\n"
                    + "GROUP BY \n"
                    + "    i.name\n"
                    + "ORDER BY \n"
                    + "    total_sales DESC;";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, formattedDate);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String productName = rs.getString("product_name");
                int totalQuantity = rs.getInt("total_quantity_sold");
                double totalSales = rs.getDouble("total_sales");
                model.addRow(new Object[]{productName, totalQuantity, String.format("P%.2f", totalSales)});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading sales summary!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateMonthlySalesSummaryReport(Date monthDate) {
        DefaultTableModel model = (DefaultTableModel) jTable3.getModel();
        model.setRowCount(0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try (Connection conn = DatabaseConnection.getConnection()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(monthDate);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date startDate = calendar.getTime();

            calendar.add(Calendar.MONTH, 1);
            Date endExclusive = calendar.getTime();

            String formattedStartDate = dateFormat.format(startDate);
            String formattedEndExclusive = dateFormat.format(endExclusive);

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
            pstmt.setString(1, formattedStartDate);
            pstmt.setString(2, formattedEndExclusive);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String productName = rs.getString("product_name");
                int saleYear = rs.getInt("sale_year");
                int saleMonth = rs.getInt("sale_month");
                int totalQuantity = rs.getInt("total_quantity_sold");
                double totalSales = rs.getDouble("total_sales");
                model.addRow(new Object[]{productName, saleYear, saleMonth, totalQuantity, String.format("P%.2f", totalSales)});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading monthly sales summary!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Date[]> getWeekRangesForMonth(Date monthDate) {
        List<Date[]> ranges = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(monthDate);
        int targetMonth = cal.get(Calendar.MONTH);
        int targetYear = cal.get(Calendar.YEAR);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        cal.set(Calendar.DAY_OF_MONTH, 1);
        Calendar endOfMonth = Calendar.getInstance();
        endOfMonth.set(targetYear, targetMonth, lastDay, 0, 0, 0);
        endOfMonth.set(Calendar.MILLISECOND, 0);

        int weekNum = 1;
        while (cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear
                && cal.get(Calendar.DAY_OF_MONTH) <= lastDay) {
            Date weekStart = cal.getTime();
            Calendar weekEnd = (Calendar) cal.clone();

            if (weekNum == 1) {
                int dow = weekEnd.get(Calendar.DAY_OF_WEEK);
                int daysToSunday = (Calendar.SUNDAY - dow + 7) % 7;
                weekEnd.add(Calendar.DAY_OF_MONTH, daysToSunday);
            } else {
                int dow = weekEnd.get(Calendar.DAY_OF_WEEK);
                int daysToSunday = (Calendar.SUNDAY - dow + 7) % 7;
                weekEnd.add(Calendar.DAY_OF_MONTH, daysToSunday);
            }

            if (weekEnd.after(endOfMonth)) {
                weekEnd = endOfMonth;
            }

            ranges.add(new Date[]{weekStart, weekEnd.getTime()});
            weekNum++;

            cal.setTime(weekEnd.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return ranges;
    }

    private void populateWeeklySalesSummaryReport(Date monthDate) {
        DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
        model.setRowCount(0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy");

        List<Date[]> weeks = getWeekRangesForMonth(monthDate);
        int weekNumber = 1;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT \n"
                    + "    COALESCE(SUM(s.quantity), 0) AS total_quantity_sold,\n"
                    + "    COALESCE(SUM(s.total_price), 0) AS total_sales\n"
                    + "FROM \n"
                    + "    public.sales s\n"
                    + "WHERE \n"
                    + "    s.sale_date::date >= ?::date\n"
                    + "    AND s.sale_date::date <= ?::date;";

            for (Date[] week : weeks) {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, dateFormat.format(week[0]));
                pstmt.setString(2, dateFormat.format(week[1]));
                ResultSet rs = pstmt.executeQuery();

                int totalQuantity = 0;
                double totalSales = 0;
                if (rs.next()) {
                    totalQuantity = rs.getInt("total_quantity_sold");
                    totalSales = rs.getDouble("total_sales");
                }

                model.addRow(new Object[]{
                    "Week " + weekNumber,
                    displayFormat.format(week[0]),
                    displayFormat.format(week[1]),
                    totalQuantity,
                    String.format("P%.2f", totalSales)
                });
                weekNumber++;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading weekly sales summary!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateTotalSales() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        double totalSales = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            Object value = model.getValueAt(i, 2);
            if (value != null) {
                try {
                    totalSales += Double.parseDouble(value.toString().replace("P", ""));
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid number format at row " + i + ": " + value);
                }
            }
        }

        total_sales.setText("Total Daily Sales: " + String.format("P%.2f", totalSales));
    }

    private void calculateTotalMonthlySales() {
        DefaultTableModel model = (DefaultTableModel) jTable3.getModel();
        double totalSales = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            Object value = model.getValueAt(i, 4);
            if (value != null) {
                try {
                    totalSales += Double.parseDouble(value.toString().replace("P", ""));
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid number format at row " + i + ": " + value);
                }
            }
        }

        total_sales2.setText("Total Monthly Sales: " + String.format("P%.2f", totalSales));
    }

    private void calculateTotalWeeklySales() {
        DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
        double totalSales = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            Object value = model.getValueAt(i, 4);
            if (value != null) {
                try {
                    totalSales += Double.parseDouble(value.toString().replace("P", ""));
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid number format at row " + i + ": " + value);
                }
            }
        }

        total_sales3.setText("Total Weekly Sales: " + String.format("P%.2f", totalSales));
    }

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
        myButtonborderlessPrev = new components.MyButtonborderless();
        jLabelMonthNav = new javax.swing.JLabel();
        myButtonborderlessNext = new components.MyButtonborderless();
        jPanelMonthNav = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        total_sales3 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jPanel15 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        myButtonborderless5 = new components.MyButtonborderless();
        jLabel10 = new javax.swing.JLabel();
        jPanelTop = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new java.awt.GridBagLayout());

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setLayout(new java.awt.BorderLayout());

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        total_sales.setFont(new java.awt.Font("Segoe UI", 1, 24));
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

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24));
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
                .addGap(8))
        );

        jPanel1.add(jPanel3, java.awt.BorderLayout.PAGE_START);

        jPanel4.add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel10.setBackground(new java.awt.Color(255, 255, 255));
        jPanel10.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        total_sales2.setFont(new java.awt.Font("Segoe UI", 1, 24));
        total_sales2.setText("Total Monthly Sales: ##");
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
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(jTable3);

        jPanel11.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jPanel12.setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 24));
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

        myButtonborderlessPrev.setBackground(new java.awt.Color(51, 54, 82));
        myButtonborderlessPrev.setForeground(new java.awt.Color(255, 255, 255));
        myButtonborderlessPrev.setText("<");
        myButtonborderlessPrev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myButtonborderlessPrevActionPerformed(evt);
            }
        });

        jLabelMonthNav.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelMonthNav.setText("May 2026");

        myButtonborderlessNext.setBackground(new java.awt.Color(51, 54, 82));
        myButtonborderlessNext.setForeground(new java.awt.Color(255, 255, 255));
        myButtonborderlessNext.setText(">");
        myButtonborderlessNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myButtonborderlessNextActionPerformed(evt);
            }
        });

        jPanelMonthNav.setOpaque(false);
        jPanelMonthNav.add(myButtonborderlessPrev);
        jPanelMonthNav.add(jLabelMonthNav);
        jPanelMonthNav.add(myButtonborderlessNext);

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addComponent(myButtonborderless4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addGap(8)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addComponent(jPanelMonthNav, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8))
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanelMonthNav, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel3)
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                .addComponent(myButtonborderless4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel11.add(jPanel12, java.awt.BorderLayout.PAGE_START);

        jPanel5.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanelTop.setLayout(new GridLayout(1, 2, 10, 0));
        jPanelTop.setOpaque(false);
        jPanelTop.add(jPanel4);
        jPanelTop.add(jPanel5);

        java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        add(jPanelTop, gridBagConstraints);

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setLayout(new java.awt.BorderLayout());

        jPanel13.setBackground(new java.awt.Color(255, 255, 255));
        jPanel13.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        total_sales3.setFont(new java.awt.Font("Segoe UI", 1, 24));
        total_sales3.setText("Total Weekly Sales: ##");
        jPanel13.add(total_sales3);

        jPanel14.setBackground(new java.awt.Color(255, 255, 255));
        jPanel14.setLayout(new java.awt.BorderLayout());

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Week", "Start Date", "End Date", "Total Quantity Sold", "Total Sales"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(jTable2);

        jPanel14.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jPanel15.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 24));
        jLabel4.setText("Weekly Sales Summary");

        myButtonborderless5.setBackground(new java.awt.Color(51, 54, 82));
        myButtonborderless5.setForeground(new java.awt.Color(255, 255, 255));
        myButtonborderless5.setText("Refresh");
        myButtonborderless5.setPreferredSize(new java.awt.Dimension(150, 30));
        myButtonborderless5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myButtonborderless5ActionPerformed(evt);
            }
        });

        jLabel10.setPreferredSize(new java.awt.Dimension(25, 27));

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addComponent(myButtonborderless5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(myButtonborderless5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8))
        );

        jPanel14.add(jPanel15, java.awt.BorderLayout.PAGE_START);
        jPanel14.add(jPanel13, java.awt.BorderLayout.PAGE_END);

        jPanel6.add(jPanel14, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        add(jPanel6, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void myButtonborderless3ActionPerformed(java.awt.event.ActionEvent evt) {
        populateSalesSummaryReport(today);
        calculateTotalSales();
    }

    private void myButtonborderless4ActionPerformed(java.awt.event.ActionEvent evt) {
        populateMonthlySalesSummaryReport(selectedMonth.getTime());
        calculateTotalMonthlySales();
    }

    private void myButtonborderless5ActionPerformed(java.awt.event.ActionEvent evt) {
        populateWeeklySalesSummaryReport(selectedMonth.getTime());
        calculateTotalWeeklySales();
    }

    private void myButtonborderlessPrevActionPerformed(java.awt.event.ActionEvent evt) {
        shiftMonth(-1);
    }

    private void myButtonborderlessNextActionPerformed(java.awt.event.ActionEvent evt) {
        shiftMonth(1);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabelMonthNav;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanelMonthNav;
    private javax.swing.JPanel jPanelTop;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTable3;
    private components.MyButtonborderless myButtonborderless3;
    private components.MyButtonborderless myButtonborderless4;
    private components.MyButtonborderless myButtonborderless5;
    private components.MyButtonborderless myButtonborderlessPrev;
    private components.MyButtonborderless myButtonborderlessNext;
    private javax.swing.JLabel total_sales;
    private javax.swing.JLabel total_sales2;
    private javax.swing.JLabel total_sales3;
    // End of variables declaration//GEN-END:variables
}
