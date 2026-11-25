
package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;

public class StatisticsPanel extends JPanel {

    private Color BG_MAIN;
private Color BG_CARD;
private Color TEXT_WHITE;
private Color TEXT_GRAY;

private boolean isDarkMode;

public void updateTheme(boolean darkMode) {
    isDarkMode = darkMode;

    if (darkMode) {
        BG_MAIN = new Color(18, 22, 33);
        BG_CARD = new Color(30, 36, 52);
        TEXT_WHITE = new Color(240, 244, 248);
        TEXT_GRAY = new Color(148, 163, 184);
    } else {
        BG_MAIN = new Color(245, 247, 250);
        BG_CARD = Color.WHITE;
        TEXT_WHITE = new Color(30, 41, 59);
        TEXT_GRAY = new Color(100, 116, 139);
    }

    setBackground(BG_MAIN);

    repaint();
}

    
public StatisticsPanel(java.util.Map<String, Double> averages,int[] distribution) {
    setLayout(new BorderLayout(15, 15));
    setBackground(BG_MAIN);
    setBorder(new EmptyBorder(20, 20, 20, 20));

    JLabel title = new JLabel("Academic Statistics & Analytics");
    title.setFont(new Font("SansSerif", Font.BOLD, 22));
    title.setForeground(TEXT_WHITE);
    add(title, BorderLayout.NORTH);

    JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 15, 0));
    chartsPanel.setBackground(BG_MAIN);

    chartsPanel.add(new PerformanceBarChartPanel(averages));
    chartsPanel.add(new GradeDistributionChartPanel(distribution));
    add(chartsPanel, BorderLayout.CENTER);
}


    // Performance Bar Chart
    class PerformanceBarChartPanel extends JPanel {
        private java.util.Map<String, Double> averages;

public PerformanceBarChartPanel(java.util.Map<String, Double> averages) {
    this.averages = averages;
    setOpaque(false);
}
       
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(BG_CARD);
            g2.fill(new RoundRectangle2D.Double(
                0, 0, getWidth(), getHeight(), 16, 16
            ));

            g2.setColor(TEXT_WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString(
                "Performance Overview (Real-time Averages)", 20, 25
            );

            String[] subjects = averages.keySet().toArray(new String[0]);
            Double[] values = averages.values().toArray(new Double[0]);
            if (subjects.length == 0) {
              g2.setColor(TEXT_GRAY);
              g2.drawString("No grade data available", 50, 100);
              g2.dispose();
              return;
            }

            Color[] colors = {
    new Color(52, 152, 219),
    new Color(155, 89, 182),
    new Color(46, 204, 113),
    new Color(243, 156, 18),
    new Color(231, 76, 60)
};

           int startX = 55;
int bottomY = getHeight() - 340;
int maxHeight = 130;

int availableWidth = getWidth() - 80;
int barWidth = Math.min(45, availableWidth / (subjects.length * 2));

int gap = (subjects.length > 1)
        ? (availableWidth - (barWidth * subjects.length)) / (subjects.length - 1)
        : 0;
            g2.setColor(new Color(45, 55, 75));
            g2.drawLine(40, bottomY, getWidth() - 20, bottomY);

            for (int i = 0; i <= 4; i++) {
                int y = bottomY - (i * 32);
                g2.drawString((i * 5) + "", 15, y + 5);

                if (i > 0) {
                    g2.drawLine(40, y, getWidth() - 20, y);
                }
            }

            for (int i = 0; i < subjects.length; i++) {
                int x = startX + i * (barWidth + gap);
                int barHeight = (int) (
                    (values[i] / 20.0) * maxHeight
                );
                int y = bottomY - barHeight;

                g2.setColor(colors[i % colors.length]);
                g2.fillRoundRect(x, y, barWidth, barHeight, 6, 6);

                g2.setColor(TEXT_WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.drawString(
                    String.valueOf(values[i]), x + 6, y - 6
                );

                g2.setColor(TEXT_GRAY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                String subjectName = subjects[i].length() > 9
                ? subjects[i].substring(0, 9) + "..."
                : subjects[i];

                g2.drawString(subjectName, x - 5, bottomY + 18);
            }

            g2.dispose();
        }
    }

    // Grade Distribution Chart
    class GradeDistributionChartPanel extends JPanel {
    
    private int[] distribution;

    public GradeDistributionChartPanel(int[] distribution) {
        this.distribution = distribution;
        setOpaque(false);
    }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(BG_CARD);
            g2.fill(new RoundRectangle2D.Double(
                0, 0, getWidth(), getHeight(), 16, 16
            ));

            g2.setColor(TEXT_WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Grade Distribution", 20, 25);

            int cx = 110;
            int cy = 115;
            int r = 65;
            int total = distribution[0] + distribution[1] + distribution[2];

            if (total == 0) {
                g2.setColor(Color.WHITE);
                g2.drawString("No grade data available", 50, 100);
                g2.dispose();
                return;
            }

           double excellentAngle = distribution[0] * 360.0 / total;
           double goodAngle = distribution[1] * 360.0 / total;
           double averageAngle = 360.0 - excellentAngle - goodAngle;

            int startAngle = 0;

int excellentDegrees = (int) Math.round(excellentAngle);
int goodDegrees = (int) Math.round(goodAngle);
int averageDegrees = 360 - excellentDegrees - goodDegrees;

g2.setColor(new Color(52, 152, 219));
g2.fill(new Arc2D.Double(
    cx-r, cy-r, 2*r, 2*r,
    startAngle, excellentDegrees, Arc2D.PIE
));

startAngle += excellentDegrees;

g2.setColor(new Color(155, 89, 182));
g2.fill(new Arc2D.Double(
    cx-r, cy-r, 2*r, 2*r,
    startAngle, goodDegrees, Arc2D.PIE
));

startAngle += goodDegrees;

g2.setColor(new Color(46, 204, 113));
g2.fill(new Arc2D.Double(
    cx-r, cy-r, 2*r, 2*r,
    startAngle, averageDegrees, Arc2D.PIE
));

g2.setColor(BG_CARD);
g2.fillOval(cx-35, cy-35, 70, 70);

            int lx = 210;
            int ly = 75;

            double excellentPercent = distribution[0] * 100.0 / total;
double goodPercent = distribution[1] * 100.0 / total;
double averagePercent = distribution[2] * 100.0 / total;

drawLegend(g2, lx, ly,
    new Color(52, 152, 219),
    String.format("%.1f%% Excellent (16-20)", excellentPercent));

drawLegend(g2, lx, ly + 30,
    new Color(155, 89, 182),
    String.format("%.1f%% Good (14-16)", goodPercent));

drawLegend(g2, lx, ly + 60,
    new Color(46, 204, 113),
    String.format("%.1f%% Average (<14)", averagePercent));
            g2.dispose();
        }

        private void drawLegend(
            Graphics2D g2, int x, int y, Color c, String text
        ) {
            g2.setColor(c);
            g2.fillRoundRect(x, y, 12, 12, 4, 4);

            g2.setColor(TEXT_WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString(text, x + 20, y + 11);
        }
    }
}
