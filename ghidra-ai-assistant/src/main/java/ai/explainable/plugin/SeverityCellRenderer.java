package ai.explainable.plugin;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

class SeverityCellRenderer extends DefaultTableCellRenderer {
    private static final Color CRITICAL_BG = new Color(128, 90, 213); // purple
    private static final Color HIGH_BG = new Color(220, 53, 69);      // red
    private static final Color MEDIUM_BG = new Color(255, 140, 0);    // orange
    private static final Color LOW_BG = new Color(255, 235, 59);      // yellow

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {

        Component c = super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column
        );

        setHorizontalAlignment(SwingConstants.CENTER);
        setFont(getFont().deriveFont(Font.BOLD));

        if (isSelected) {
            c.setBackground(table.getSelectionBackground());
            c.setForeground(table.getSelectionForeground());
            return c;
        }

        String severity = value == null ? "" : value.toString().trim().toLowerCase();

        switch (severity) {
            case "critical" -> {
                c.setBackground(CRITICAL_BG);
                c.setForeground(Color.WHITE);
            }
            case "high" -> {
                c.setBackground(HIGH_BG);
                c.setForeground(Color.WHITE);
            }
            case "medium" -> {
                c.setBackground(MEDIUM_BG);
                c.setForeground(Color.BLACK);
            }
            case "low" -> {
                c.setBackground(LOW_BG);
                c.setForeground(Color.BLACK);
            }
            default -> {
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
            }
        }

        return c;
    }
}