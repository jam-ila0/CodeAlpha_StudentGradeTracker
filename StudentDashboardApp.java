import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import view.StatisticsPanel;
import model.Student;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

// --- 1. DATABASE HELPER CLASS (Database & Tables Initialization) ---
class DatabaseHelper {
    private static final String URL = "jdbc:sqlite:school_grades.db";

    
  private static Connection connect() throws SQLException {

    try {
        Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
        throw new SQLException("SQLite JDBC Driver not found!", e);
    }

    return DriverManager.getConnection(URL);
  }

  public static int[] getGradeDistribution() {
    int[] distribution = new int[3];

    String sql = "SELECT " +
                 "SUM(CASE WHEN grade >= 16 THEN 1 ELSE 0 END) AS excellent, " +
                 "SUM(CASE WHEN grade >= 14 AND grade < 16 THEN 1 ELSE 0 END) AS good, " +
                 "SUM(CASE WHEN grade < 14 THEN 1 ELSE 0 END) AS average " +
                 "FROM grades";

    try (Connection conn = connect();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        if (rs.next()) {
            distribution[0] = rs.getInt("excellent");
            distribution[1] = rs.getInt("good");
            distribution[2] = rs.getInt("average");
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
 System.out.println("Excellent: " + distribution[0]);
    System.out.println("Good: " + distribution[1]);
    System.out.println("Average: " + distribution[2]);
    return distribution;
 }


    public static void initDatabase() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            // Table Students (Information Personnelle)
            stmt.execute("CREATE TABLE IF NOT EXISTS students ("
                       + "id TEXT PRIMARY KEY,"
                       + "student_name TEXT NOT NULL,"
                       + "email TEXT,"
                       + "class TEXT"
                       + ");");

            // Table Grades (N9at w L-mawadd m-rbotin b Student ID)
            stmt.execute("CREATE TABLE IF NOT EXISTS grades ("
                       + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                       + "student_id TEXT NOT NULL,"
                       + "subject TEXT NOT NULL,"
                       + "grade REAL NOT NULL,"
                       + "status TEXT NOT NULL,"
                       + "date TEXT,"
                       + "FOREIGN KEY(student_id) REFERENCES students(id)"
                       + ");");
        } catch (SQLException e) {
            System.err.println("Erreur initialisation DB:");
            e.printStackTrace();
        }
    }

    // Returns true only when the student is really saved in SQLite.
    
 public static boolean insertStudent(Student s) {
    String sql = "INSERT INTO students(id, student_name, email, \"class\") VALUES(?, ?, ?, ?)";

    try (Connection conn = connect();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, s.getId());
        pstmt.setString(2, s.getName());
        pstmt.setString(3, s.getEmail());
        pstmt.setString(4, s.getClassName());

        return pstmt.executeUpdate() == 1;

    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
 }


    // Returns true only when the grade is really saved in SQLite.
    public static boolean insertGrade(Grade g) {
        String sql = "INSERT INTO grades(student_id, subject, grade, status, date) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, g.studentId);
            pstmt.setString(2, g.subject);
            pstmt.setDouble(3, g.grade);
            pstmt.setString(4, g.status);
            pstmt.setString(5, g.date);
            return pstmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ArrayList<Student> getAllStudents() {
        ArrayList<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Student(
                    rs.getString("id"),
                    rs.getString("student_name"),
                    rs.getString("email"),
                    rs.getString("class")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur Fetch Students:");
            e.printStackTrace();
        }
        return list;
    }

    public static ArrayList<GradeRecord> getAllGradeRecords() {
        ArrayList<GradeRecord> list = new ArrayList<>();
        String sql = "SELECT g.id, s.student_name, g.subject, g.grade, g.status, g.date, s.id as student_id "
                   + "FROM grades g JOIN students s ON g.student_id = s.id ORDER BY g.date DESC";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new GradeRecord(
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getString("subject"),
                    rs.getDouble("grade"),
                    rs.getString("status"),
                    rs.getString("date")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur Fetch Grades:");
            e.printStackTrace();
        }
        return list;
    }
    
 public static java.util.Map<String, Double> getSubjectAverages() {
    java.util.Map<String, Double> averages = new java.util.LinkedHashMap<>();

    String sql = "SELECT subject, AVG(grade) AS average " +
                 "FROM grades GROUP BY subject ORDER BY subject";

    try (Connection conn = connect();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            averages.put(
                rs.getString("subject"),
                rs.getDouble("average")
            );
        }

    } catch (SQLException e) {
        System.err.println("Erreur calculating subject averages:");
        e.printStackTrace();
    }

    return averages;
 }
public static boolean testConnection() {
    try (Connection conn = connect()) {
        return conn != null && !conn.isClosed();
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}
}



class Grade {
    String studentId;
    String subject;
    double grade;
    String status;
    String date;

    public Grade(String studentId, String subject, double grade, String date) {
        this.studentId = studentId;
        this.subject = subject;
        this.grade = grade;
        this.status = (grade >= 10) ? "Passed" : "Failed";
        this.date = (date != null) ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}

class GradeRecord {
    String studentId;
    String studentName;
    String subject;
    double grade;
    String status;
    String date;

    public GradeRecord(String studentId, String studentName, String subject, double grade, String status, String date) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.subject = subject;
        this.grade = grade;
        this.status = status;
        this.date = date;
    }
}

// --- 3. CUSTOM CHARTS ---


// --- 4. MAIN APPLICATION FRAME ---
public class StudentDashboardApp extends JFrame {

    private Color BG_MAIN = new Color(18, 22, 33);
private Color BG_SIDEBAR = new Color(24, 29, 43);
private Color BG_CARD = new Color(30, 36, 52);
private StatisticsPanel statisticsPanel;

private final Color ACCENT_PURPLE = new Color(108, 92, 231);
private final Color ACCENT_BLUE = new Color(52, 152, 219);
private final Color ACCENT_GREEN = new Color(46, 204, 113);
private final Color ACCENT_ORANGE = new Color(243, 156, 18);

private Color TEXT_WHITE = new Color(240, 244, 248);
private Color TEXT_GRAY = new Color(148, 163, 184);
private boolean isDarkMode = true;

    private ArrayList<Student> studentList = new ArrayList<>();
    private ArrayList<GradeRecord> gradeList = new ArrayList<>();

    private JLabel lblTotalStudents, lblAvgScore, lblHighestScore, lblLowestScore;
    private DefaultTableModel tableModelDashboard, tableModelStudents, tableModelRanking, tableModelGrades;
    
    private JPanel contentCards;
    private CardLayout cardLayout;
    private ArrayList<JButton> navButtons = new ArrayList<>();

    public StudentDashboardApp() {
        DatabaseHelper.initDatabase();
        loadDataFromDatabase();

        setTitle("Student Grade Tracker - Pro System");
        setSize(1280, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createSidebar(), BorderLayout.WEST);

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BG_MAIN);
        mainContainer.add(createHeader(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentCards = new JPanel(cardLayout);
        contentCards.setBackground(BG_MAIN);

        // All Dynamic Functional Pages
        contentCards.add(createDashboardPanel(), "Dashboard");
        contentCards.add(createStudentsPanel(), "Students");
        contentCards.add(createGradesManagementPanel(), "Grades Management");
        contentCards.add(createRankingPanel(), "Ranking");
        contentCards.add(createStatisticsPanel(), "Statistics");
        contentCards.add(createReportsPanel(), "Reports");
        contentCards.add(createSettingsPanel(), "Settings");

        mainContainer.add(contentCards, BorderLayout.CENTER);
        add(mainContainer, BorderLayout.CENTER);

        updateAllUI();
    }

    private void loadDataFromDatabase() {
        studentList = DatabaseHelper.getAllStudents();
        gradeList = DatabaseHelper.getAllGradeRecords();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, getHeight()));
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(15, 12, 15, 12));

        JLabel logoBox = new JLabel("  🎓 Student Tracker");
        logoBox.setFont(new Font("SansSerif", Font.BOLD, 14));
        logoBox.setForeground(TEXT_WHITE);
        logoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logoBox);

        JLabel subLogo = new JLabel("  Learn • Track • Excel");
        subLogo.setFont(new Font("SansSerif", Font.PLAIN, 10));
        subLogo.setForeground(TEXT_GRAY);
        subLogo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(subLogo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 25)));

        String[] menuItems = {"Dashboard", "Students", "Grades Management", "Ranking", "Statistics", "Reports", "Settings"};

        for (int i = 0; i < menuItems.length; i++) {
            boolean active = (i == 0);
            JButton btn = createNavButton(menuItems[i], active);
            navButtons.add(btn);
            
            String pageName = menuItems[i];
            btn.addActionListener(e -> {
                for (JButton b : navButtons) {
                    b.setBackground(BG_SIDEBAR);
                    b.setForeground(TEXT_GRAY);
                }
                btn.setBackground(ACCENT_PURPLE);
                btn.setForeground(Color.WHITE);
                
                loadDataFromDatabase();
                updateAllUI();
                cardLayout.show(contentCards, pageName);
            });

            sidebar.add(btn);
            sidebar.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        sidebar.add(Box.createVerticalGlue());
        JButton btnLogout = createNavButton("Logout", false);
        btnLogout.addActionListener(e -> System.exit(0));
        sidebar.add(btnLogout);

        return sidebar;
    }

    private JButton createNavButton(String text, boolean isActive) {
        JButton btn = new JButton("  " + text);
        btn.setMaximumSize(new Dimension(195, 36));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (isActive) {
            btn.setBackground(ACCENT_PURPLE);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(BG_SIDEBAR);
            btn.setForeground(TEXT_GRAY);
        }
        return btn;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_SIDEBAR);
        header.setPreferredSize(new Dimension(getWidth(), 60));
        header.setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftHeader.setBackground(BG_SIDEBAR);
        JLabel title = new JLabel("Academic System");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT_WHITE);
        
        JLabel subtitle = new JLabel("Welcome back, Jamila! 👋");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_GRAY);

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setBackground(BG_SIDEBAR);
        titleBox.add(title);
        titleBox.add(subtitle);
        leftHeader.add(titleBox);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        rightHeader.setBackground(BG_SIDEBAR);
        JLabel dateLabel = new JLabel("📅 " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")));
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dateLabel.setForeground(TEXT_WHITE);
        rightHeader.add(dateLabel);

        header.add(leftHeader, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);
        return header;
    }

    // --- DASHBOARD PANEL ---
    private JPanel createDashboardPanel() {
        JPanel content = new JPanel(new BorderLayout(0, 15));
        content.setBackground(BG_MAIN);
        content.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 15, 0));
        cardsGrid.setBackground(BG_MAIN);
        cardsGrid.setPreferredSize(new Dimension(getWidth(), 95));

        lblTotalStudents = new JLabel("0");
        lblAvgScore = new JLabel("0.0");
        lblHighestScore = new JLabel("0.0");
        lblLowestScore = new JLabel("0.0");

        cardsGrid.add(createKpiCard("Total Students", lblTotalStudents, "Registered", ACCENT_BLUE));
        cardsGrid.add(createKpiCard("Average Score", lblAvgScore, "Out of 20", ACCENT_PURPLE));
        cardsGrid.add(createKpiCard("Highest Score", lblHighestScore, "Top grade", ACCENT_GREEN));
        cardsGrid.add(createKpiCard("Lowest Score", lblLowestScore, "Bottom grade", ACCENT_ORANGE));

        JPanel chartsGrid = new JPanel(new GridLayout(1, 2, 15, 0));
        chartsGrid.setBackground(BG_MAIN);
        chartsGrid.setPreferredSize(new Dimension(getWidth(), 6));
        
        JPanel topAndCharts = new JPanel(new BorderLayout(0, 8));
        topAndCharts.setBackground(BG_MAIN);
        topAndCharts.add(cardsGrid, BorderLayout.NORTH);
        topAndCharts.add(chartsGrid, BorderLayout.CENTER);

        content.add(topAndCharts, BorderLayout.NORTH);

        JPanel bottomSec = new JPanel(new GridLayout(1, 2, 15, 0));
        bottomSec.setBackground(BG_MAIN);
        bottomSec.add(createDashboardTablePanel());
        bottomSec.add(createQuickActionsPanel());

        content.add(bottomSec, BorderLayout.CENTER);
        return content;
    }
    private void toggleTheme() {

    if (isDarkMode) {
        BG_MAIN = new Color(245, 247, 250);
        BG_SIDEBAR = Color.WHITE;
        BG_CARD = Color.WHITE;
        TEXT_WHITE = new Color(30, 41, 59);
        TEXT_GRAY = new Color(100, 116, 139);

        isDarkMode = false;

    } else {
        BG_MAIN = new Color(18, 22, 33);
        BG_SIDEBAR = new Color(24, 29, 43);
        BG_CARD = new Color(30, 36, 52);
        TEXT_WHITE = new Color(240, 244, 248);
        TEXT_GRAY = new Color(148, 163, 184);

        isDarkMode = true;
    }
if (statisticsPanel != null) {
    statisticsPanel.updateTheme(isDarkMode);
}
    applyTheme(getContentPane());

revalidate();
repaint();
}
private void applyTheme(Component component) {

    if (component instanceof JPanel) {
        component.setBackground(BG_MAIN);
    }

    if (component instanceof JLabel) {
        component.setForeground(TEXT_WHITE);
    }
    
    if (component instanceof JViewport) {
    component.setBackground(BG_CARD);
    }
    if (component instanceof JTable) {
        JTable table = (JTable) component;
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_WHITE);
        table.setGridColor(BG_MAIN);
        table.setSelectionBackground(ACCENT_PURPLE);
        table.setSelectionForeground(Color.WHITE);

        table.getTableHeader().setBackground(BG_SIDEBAR);
        table.getTableHeader().setForeground(TEXT_WHITE);
    }

    if (component instanceof JButton) {
        component.setForeground(Color.WHITE);
    }

    if (component instanceof Container) {
        for (Component child : ((Container) component).getComponents()) {
            applyTheme(child);
        }
    }
}

    // --- 1. STUDENTS MANAGEMENT PANEL (Real Directory) ---
    
private JPanel createStudentsPanel() {
    JPanel panel = new JPanel(new BorderLayout(0, 15));
    panel.setBackground(BG_MAIN);
    panel.setBorder(new EmptyBorder(20, 20, 20, 20));

    // Header
    JPanel topHeader = new JPanel(new BorderLayout());
    topHeader.setBackground(BG_MAIN);

    JLabel title = new JLabel("Student Directory (Personal Information)");
    title.setFont(new Font("SansSerif", Font.BOLD, 22));
    title.setForeground(TEXT_WHITE);
    topHeader.add(title, BorderLayout.WEST);

    JButton btnAdd = new JButton("+ Register Student");
    btnAdd.setBackground(ACCENT_PURPLE);
    btnAdd.setForeground(Color.WHITE);
    btnAdd.setFont(new Font("SansSerif", Font.BOLD, 12));
    btnAdd.setFocusPainted(false);
    btnAdd.addActionListener(e -> openAddStudentDialog());
    topHeader.add(btnAdd, BorderLayout.EAST);

    // Search Bar
    JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
    searchPanel.setBackground(BG_MAIN);

    JLabel searchLabel = new JLabel("Search Student:");
    searchLabel.setForeground(TEXT_GRAY);

    JTextField searchField = new JTextField();
    searchField.setPreferredSize(new Dimension(250, 35));
    searchField.setToolTipText("Search by ID or student name");

    searchPanel.add(searchLabel, BorderLayout.WEST);
    searchPanel.add(searchField, BorderLayout.CENTER);

    JPanel northPanel = new JPanel(new BorderLayout(0, 15));
    northPanel.setBackground(BG_MAIN);
    northPanel.add(topHeader, BorderLayout.NORTH);
    northPanel.add(searchPanel, BorderLayout.SOUTH);

    panel.add(northPanel, BorderLayout.NORTH);

    // Table
    JPanel card = new JPanel(new BorderLayout());
    card.setBackground(BG_CARD);
    card.setBorder(new EmptyBorder(15, 15, 15, 15));

    String[] columns = {"ID", "Full Name", "Email Address", "Class / Program"};
    tableModelStudents = new DefaultTableModel(columns, 0);

    JTable table = new JTable(tableModelStudents);

    TableRowSorter<DefaultTableModel> sorter =
        new TableRowSorter<>(tableModelStudents);

    table.setRowSorter(sorter);

    // Search functionality
    searchField.getDocument().addDocumentListener(new DocumentListener() {

        private void search() {
            String text = searchField.getText().trim();

            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(
                    RowFilter.regexFilter(
                        "(?i)" + java.util.regex.Pattern.quote(text), 0, 1
                    )
                );
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            search();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            search();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            search();
        }
    });

    table.setBackground(BG_SIDEBAR);
    table.setForeground(TEXT_WHITE);
    table.setRowHeight(30);
    table.setFont(new Font("SansSerif", Font.PLAIN, 13));
    table.getTableHeader().setBackground(BG_MAIN);
    table.getTableHeader().setForeground(TEXT_GRAY);
    table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.getViewport().setBackground(BG_SIDEBAR);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());

    card.add(scrollPane, BorderLayout.CENTER);
    panel.add(card, BorderLayout.CENTER);

    return panel;
}

private JPanel createGradesManagementPanel() {

    JPanel panel = new JPanel(new BorderLayout(0, 15));
    panel.setBackground(BG_MAIN);
    panel.setBorder(new EmptyBorder(20, 20, 20, 20));

    // Header
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(BG_MAIN);

    JLabel title = new JLabel("Grades Management");
    title.setFont(new Font("SansSerif", Font.BOLD, 22));
    title.setForeground(TEXT_WHITE);

    JButton btnAddGrade = new JButton("+ Add Grade");
    btnAddGrade.setBackground(ACCENT_BLUE);
    btnAddGrade.setForeground(Color.WHITE);
    btnAddGrade.setFont(new Font("SansSerif", Font.BOLD, 12));
    btnAddGrade.setFocusPainted(false);
    btnAddGrade.addActionListener(e -> openAddGradeDialog());

    header.add(title, BorderLayout.WEST);
    header.add(btnAddGrade, BorderLayout.EAST);

    panel.add(header, BorderLayout.NORTH);

    // Grades Table Card
    JPanel card = new JPanel(new BorderLayout());
    card.setBackground(BG_CARD);
    card.setBorder(new EmptyBorder(15, 15, 15, 15));

    String[] columns = {
        "Student ID", "Student Name", "Subject",
        "Grade (/20)", "Status", "Date"
    };

    tableModelGrades = new DefaultTableModel(columns, 0);

    JTable table = new JTable(tableModelGrades);
    table.setBackground(BG_SIDEBAR);
    table.setForeground(TEXT_WHITE);
    table.setRowHeight(30);
    table.setFont(new Font("SansSerif", Font.PLAIN, 13));

    table.getTableHeader().setBackground(BG_MAIN);
    table.getTableHeader().setForeground(TEXT_GRAY);
    table.getTableHeader().setFont(
        new Font("SansSerif", Font.BOLD, 12)
    );

    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.getViewport().setBackground(BG_SIDEBAR);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());

    card.add(scrollPane, BorderLayout.CENTER);
    panel.add(card, BorderLayout.CENTER);

    return panel;
}


    // --- 3. RANKING PANEL ---
    private JPanel createRankingPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("🏆 Top Performing Students Ranking");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);
        panel.add(title, BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] columns = {"Rank", "Student Name", "Average Score (/20)", "Performance Status"};
        tableModelRanking = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModelRanking);
        table.setBackground(BG_SIDEBAR);
        table.setForeground(TEXT_WHITE);
        table.setRowHeight(30);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setBackground(BG_MAIN);
        table.getTableHeader().setForeground(TEXT_GRAY);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(BG_SIDEBAR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        card.add(scrollPane, BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // --- 4. STATISTICS PANEL ---
    private JPanel createStatisticsPanel() {
    statisticsPanel = new StatisticsPanel(
        DatabaseHelper.getSubjectAverages(),
        DatabaseHelper.getGradeDistribution()
    );

    statisticsPanel.updateTheme(isDarkMode);

    return statisticsPanel;
}
    // --- 5. REPORTS PANEL ---
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel box = new JPanel();
        box.setBackground(BG_CARD);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("📊 Automated Examination Reports");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);

        JLabel sub = new JLabel("Generate comprehensive PDF/CSV grade reports and class transcripts instantly.");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sub.setForeground(TEXT_GRAY);

        JButton btnGenerate = new JButton("Generate Academic Report");
        btnGenerate.setBackground(ACCENT_GREEN);
        btnGenerate.setForeground(Color.WHITE);
        btnGenerate.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnGenerate.setFocusPainted(false);
        btnGenerate.addActionListener(e -> generateAcademicReport());
        box.add(title);
        box.add(Box.createRigidArea(new Dimension(0, 10)));
        box.add(sub);
        box.add(Box.createRigidArea(new Dimension(0, 20)));
        box.add(btnGenerate);

        panel.add(box, BorderLayout.NORTH);
        return panel;
    }

private void generateAcademicReport() {

    ArrayList<GradeRecord> records = DatabaseHelper.getAllGradeRecords();

    if (records.isEmpty()) {
        JOptionPane.showMessageDialog(
            this,
            "No grade records available to generate a report.",
            "Empty Report",
            JOptionPane.INFORMATION_MESSAGE
        );
        return;
    }

    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Save Academic Report");
    chooser.setSelectedFile(new File("Academic_Report.csv"));

    int result = chooser.showSaveDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {

        File file = chooser.getSelectedFile();

        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (FileWriter writer = new FileWriter(file)) {

            writer.write("Student ID,Student Name,Subject,Grade,Status,Date\n");

            for (GradeRecord record : records) {

                writer.write(
                    "\"" + record.studentId + "\"," +
                    "\"" + record.studentName + "\"," +
                    "\"" + record.subject + "\"," +
                    record.grade + "," +
                    "\"" + record.status + "\"," +
                    "\"" + record.date + "\"\n"
                );
            }

            JOptionPane.showMessageDialog(
                this,
                "Academic report generated successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException e) {

            JOptionPane.showMessageDialog(
                this,
                "Error generating report: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}


    private JPanel createSettingsPanel() {

    JPanel panel = new JPanel(new BorderLayout(0, 20));
    panel.setBackground(BG_MAIN);
    panel.setBorder(new EmptyBorder(30, 30, 30, 30));

    JLabel title = new JLabel("Application Settings");
    title.setFont(new Font("SansSerif", Font.BOLD, 24));
    title.setForeground(TEXT_WHITE);

    JPanel themeCard = new JPanel(new BorderLayout(15, 10));
    themeCard.setBackground(BG_CARD);
    themeCard.setBorder(new EmptyBorder(25, 25, 25, 25));

    JPanel textPanel = new JPanel();
    textPanel.setOpaque(false);
    textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

    JLabel themeTitle = new JLabel("Appearance");
    themeTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
    themeTitle.setForeground(TEXT_WHITE);

    JLabel themeDescription = new JLabel("Switch between Dark and Light mode.");
    themeDescription.setForeground(TEXT_GRAY);

    textPanel.add(themeTitle);
    textPanel.add(Box.createRigidArea(new Dimension(0, 8)));
    textPanel.add(themeDescription);

    JButton btnTheme = new JButton(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode");
    btnTheme.setBackground(ACCENT_PURPLE);
    btnTheme.setForeground(Color.WHITE);
    btnTheme.setFocusPainted(false);

    btnTheme.addActionListener(e -> {
        toggleTheme();
        btnTheme.setText(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode");
    });
    btnTheme.setHorizontalAlignment(SwingConstants.CENTER);
    themeCard.add(textPanel, BorderLayout.CENTER);
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
buttonPanel.setOpaque(false);
buttonPanel.add(btnTheme);

themeCard.add(buttonPanel, BorderLayout.EAST);
// Database Configuration Card
JPanel databaseCard = new JPanel(new BorderLayout(15, 10));
databaseCard.setBackground(BG_CARD);
databaseCard.setBorder(new EmptyBorder(25, 25, 25, 25));

JPanel databaseInfo = new JPanel();
databaseInfo.setOpaque(false);
databaseInfo.setLayout(new BoxLayout(databaseInfo, BoxLayout.Y_AXIS));

JLabel databaseTitle = new JLabel("Database Configuration");
databaseTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
databaseTitle.setForeground(TEXT_WHITE);

JLabel databaseName = new JLabel("Database: school_grades.db");
databaseName.setForeground(TEXT_GRAY);

JLabel databaseType = new JLabel("Type: SQLite");
databaseType.setForeground(TEXT_GRAY);

databaseInfo.add(databaseTitle);
databaseInfo.add(Box.createRigidArea(new Dimension(0, 10)));
databaseInfo.add(databaseName);
databaseInfo.add(Box.createRigidArea(new Dimension(0, 5)));
databaseInfo.add(databaseType);

JButton btnTestConnection = new JButton("Test Database Connection");
btnTestConnection.setBackground(ACCENT_BLUE);
btnTestConnection.setForeground(Color.WHITE);
btnTestConnection.setFocusPainted(false);

btnTestConnection.addActionListener(e -> {
    boolean connected = DatabaseHelper.testConnection();

    if (connected) {
        JOptionPane.showMessageDialog(
            this,
            "Database connection successful!",
            "Connection Status",
            JOptionPane.INFORMATION_MESSAGE
        );
    } else {
        JOptionPane.showMessageDialog(
            this,
            "Database connection failed!",
            "Connection Status",
            JOptionPane.ERROR_MESSAGE
        );
    }
});

JPanel databaseButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
databaseButtonPanel.setOpaque(false);
databaseButtonPanel.add(btnTestConnection);

databaseCard.add(databaseInfo, BorderLayout.CENTER);
databaseCard.add(databaseButtonPanel, BorderLayout.EAST);
    panel.add(title, BorderLayout.NORTH);
    JPanel settingsContent = new JPanel();
settingsContent.setBackground(BG_MAIN);
settingsContent.setLayout(new BoxLayout(settingsContent, BoxLayout.Y_AXIS));

settingsContent.add(themeCard);
settingsContent.add(Box.createRigidArea(new Dimension(0, 20)));
settingsContent.add(databaseCard);
settingsContent.add(Box.createRigidArea(new Dimension(0, 20)));

// About Application Card
JPanel aboutCard = new JPanel();
aboutCard.setBackground(BG_CARD);
aboutCard.setLayout(new BoxLayout(aboutCard, BoxLayout.Y_AXIS));
aboutCard.setBorder(new EmptyBorder(25, 25, 25, 25));

JLabel aboutTitle = new JLabel("About Application");
aboutTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
aboutTitle.setForeground(TEXT_WHITE);

JLabel appName = new JLabel("Student Grade Tracker");
appName.setForeground(TEXT_WHITE);

JLabel version = new JLabel("Version: 1.0");
version.setForeground(TEXT_GRAY);

JLabel description = new JLabel("An academic management system for students' grades and performance.");
description.setForeground(TEXT_GRAY);

JLabel technologies = new JLabel("Technologies: Java Swing, SQLite, JDBC");
technologies.setForeground(TEXT_GRAY);

aboutCard.add(aboutTitle);
aboutCard.add(Box.createRigidArea(new Dimension(0, 12)));
aboutCard.add(appName);
aboutCard.add(Box.createRigidArea(new Dimension(0, 5)));
aboutCard.add(version);
aboutCard.add(Box.createRigidArea(new Dimension(0, 5)));
aboutCard.add(description);
aboutCard.add(Box.createRigidArea(new Dimension(0, 5)));
aboutCard.add(technologies);

settingsContent.add(aboutCard);

panel.add(settingsContent, BorderLayout.CENTER);

    return panel;
}

    private JPanel createGenericPagePanel(String titleText, String subtitleText) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));
        JPanel box = new JPanel();
        box.setBackground(BG_CARD);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(30, 30, 30, 30));
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT_WHITE);
        JLabel sub = new JLabel(subtitleText);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sub.setForeground(TEXT_GRAY);
        box.add(title);
        box.add(Box.createRigidArea(new Dimension(0, 10)));
        box.add(sub);
        panel.add(box, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createDashboardTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_CARD);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel tableTitle = new JLabel("Recent Grade Records");
        tableTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        tableTitle.setForeground(TEXT_WHITE);
        panel.add(tableTitle, BorderLayout.NORTH);

        String[] columns = {"Student Name", "Subject", "Grade", "Date"};
        tableModelDashboard = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModelDashboard);
        table.setBackground(BG_SIDEBAR);
        table.setForeground(TEXT_WHITE);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setBackground(BG_MAIN);
        table.getTableHeader().setForeground(TEXT_GRAY);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(BG_SIDEBAR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createQuickActionsPanel() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 20, 140, 20));
        JLabel title = new JLabel("⚡ Quick Actions");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(TEXT_WHITE);
        card.add(title, BorderLayout.NORTH);

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        btnGrid.setOpaque(false);
        btnGrid.setBorder(new EmptyBorder(20, 0, 10, 0));

        JButton btnAddStudent = createActionButton("+ Register Student", ACCENT_PURPLE);
        JButton btnAddGrade = createActionButton("📖 Add Grade", ACCENT_BLUE);
        JButton btnReports = createActionButton("📊 View Reports", ACCENT_GREEN);
        JButton btnRanking = createActionButton("🏆 View Ranking", ACCENT_ORANGE);

        btnAddStudent.addActionListener(e -> openAddStudentDialog());
        btnAddGrade.addActionListener(e -> openAddGradeDialog());
        btnReports.addActionListener(e -> cardLayout.show(contentCards, "Reports"));
        btnRanking.addActionListener(e -> cardLayout.show(contentCards, "Ranking"));

        btnGrid.add(btnAddStudent);
        btnGrid.add(btnAddGrade);
        btnGrid.add(btnReports);
        btnGrid.add(btnRanking);
        card.add(btnGrid, BorderLayout.CENTER);
        return card;
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, String subtitle, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new GridLayout(3, 1, 0, 2));
        card.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblTitle.setForeground(TEXT_GRAY);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);
        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblSub.setForeground(accentColor);
        card.add(lblTitle);
        card.add(valueLabel);
        card.add(lblSub);
        return card;
    }

    // --- DIALOG: Register Student ---
    private void openAddStudentDialog() {
        JDialog dialog = new JDialog(this, "Register Student", true);
        dialog.setSize(380, 280);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        dialog.getContentPane().setBackground(BG_SIDEBAR);
        ((JComponent) dialog.getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField txtId = new JTextField("STU-" + (System.currentTimeMillis() % 10000));
        JTextField txtName = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtClass = new JTextField("Software Engineering");

        JLabel l1 = new JLabel("Student ID:"); l1.setForeground(TEXT_WHITE);
        JLabel l2 = new JLabel("Full Name:"); l2.setForeground(TEXT_WHITE);
        JLabel l3 = new JLabel("Email:"); l3.setForeground(TEXT_WHITE);
        JLabel l4 = new JLabel("Class / Prog:"); l4.setForeground(TEXT_WHITE);

        JButton btnSave = new JButton("Save Student");
        btnSave.setBackground(ACCENT_PURPLE);
        btnSave.setForeground(Color.WHITE);

        dialog.add(l1); dialog.add(txtId);
        dialog.add(l2); dialog.add(txtName);
        dialog.add(l3); dialog.add(txtEmail);
        dialog.add(l4); dialog.add(txtClass);
        dialog.add(new JLabel()); dialog.add(btnSave);

        btnSave.addActionListener(e -> {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();
            String className = txtClass.getText().trim();

            if (id.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill required fields!");
                return;
            }

            // Save to the database and check the real result before showing success.
            boolean saved = DatabaseHelper.insertStudent(
                new Student(id, name, email, className)
            );

            if (!saved) {
                JOptionPane.showMessageDialog(
                    dialog,
                    "The student could not be saved.\n"
                    + "Check the VS Code Terminal / Debug Console for the database error.\n"
                    + "If this ID already exists, use another Student ID.",
                    "Registration Failed",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            loadDataFromDatabase();
            updateAllUI();
            dialog.dispose();
            JOptionPane.showMessageDialog(
                this,
                "Student registered successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
        dialog.setVisible(true);
    }

    // --- DIALOG: Add Grade ---
    private void openAddGradeDialog() {
        if (studentList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please register a student first!");
            return;
        }

        JDialog dialog = new JDialog(this, "Add Subject Grade", true);
        dialog.setSize(380, 280);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        dialog.getContentPane().setBackground(BG_SIDEBAR);
        ((JComponent) dialog.getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        // Combo box of registered students
        String[] studentNames = new String[studentList.size()];
        
for (int i = 0; i < studentList.size(); i++) {
    Student s = studentList.get(i);

    studentNames[i] = s.getId() + " - " + s.getName();
}

        JComboBox<String> cmbStudents = new JComboBox<>(studentNames);
        JTextField txtSubject = new JTextField();
        JTextField txtGrade = new JTextField();

        JLabel l1 = new JLabel("Student:"); l1.setForeground(TEXT_WHITE);
        JLabel l2 = new JLabel("Subject:"); l2.setForeground(TEXT_WHITE);
        JLabel l3 = new JLabel("Grade (/20):"); l3.setForeground(TEXT_WHITE);

        JButton btnSave = new JButton("Save Grade");
        btnSave.setBackground(ACCENT_BLUE);
        btnSave.setForeground(Color.WHITE);

        dialog.add(l1); dialog.add(cmbStudents);
        dialog.add(l2); dialog.add(txtSubject);
        dialog.add(l3); dialog.add(txtGrade);
        dialog.add(new JLabel()); dialog.add(btnSave);

        btnSave.addActionListener(e -> {
            try {
                String selectedCombo = (String) cmbStudents.getSelectedItem();
                String studentId = selectedCombo.split(" - ")[0];
                String subject = txtSubject.getText().trim();
                double gradeVal = Double.parseDouble(txtGrade.getText().trim());

                if (subject.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Please enter a subject!");
                    return;
                }
                if (gradeVal < 0 || gradeVal > 20) {
                    JOptionPane.showMessageDialog(dialog, "Grade must be between 0 and 20!");
                    return;
                }

                boolean saved = DatabaseHelper.insertGrade(
                    new Grade(studentId, subject, gradeVal, null)
                );

                if (!saved) {
                    JOptionPane.showMessageDialog(
                        dialog,
                        "The grade could not be saved. Check the VS Code Terminal / Debug Console for details.",
                        "Save Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                loadDataFromDatabase();
                updateAllUI();
                dialog.dispose();
                JOptionPane.showMessageDialog(
                    this,
                    "Grade recorded successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid grade format!");
            }
        });
        dialog.setVisible(true);
    }

    // --- REFRESH ALL TABLES & STATS ---
    private void updateAllUI() {
        if (tableModelDashboard != null) tableModelDashboard.setRowCount(0);
        if (tableModelStudents != null) tableModelStudents.setRowCount(0);
        if (tableModelGrades != null) tableModelGrades.setRowCount(0);
        if (tableModelRanking != null) tableModelRanking.setRowCount(0);

        // Populate Students Directory Table
        
        for (Student s : studentList) {
          if (tableModelStudents != null) {
              tableModelStudents.addRow(new Object[]{
                s.getId(),
                s.getName(),
                s.getEmail(),
                s.getClassName()
              });
            }
        }

        // Populate Grades Management & Dashboard Tables
        double sum = 0;
        double highest = gradeList.isEmpty() ? 0 : gradeList.get(0).grade;
        double lowest = gradeList.isEmpty() ? 0 : gradeList.get(0).grade;

        for (GradeRecord g : gradeList) {
            if (tableModelDashboard != null) {
                tableModelDashboard.addRow(new Object[]{g.studentName, g.subject, String.format("%.2f", g.grade), g.date});
            }
            if (tableModelGrades != null) {
                tableModelGrades.addRow(new Object[]{g.studentId, g.studentName, g.subject, String.format("%.2f", g.grade), g.status, g.date});
            }
            sum += g.grade;
            if (g.grade > highest) highest = g.grade;
            if (g.grade < lowest) lowest = g.grade;
        }

        // Populate Ranking Table (Group by student average)
        if (tableModelRanking != null && !studentList.isEmpty()) {
            int rank = 1;
           
         for (Student s : studentList) {
          double studentSum = 0;
          int count = 0;

          for (GradeRecord g : gradeList) {
           if (g.studentId.equals(s.getId())) {
            studentSum += g.grade;
            count++;
           }
          }

         double avg = count > 0 ? (studentSum / count) : 0.0;

         String status = avg >= 10 ? "Honors / Passed" : "At Risk";

          tableModelRanking.addRow(new Object[]{
          "#" + (rank++),
          s.getName(),
          String.format("%.2f", avg),
          status
          });
         }

         }

         // KPI Updates
         if (lblTotalStudents != null) lblTotalStudents.setText(String.valueOf(studentList.size()));
         if (!gradeList.isEmpty()) {
            double avgScore = sum / gradeList.size();
            if (lblAvgScore != null) lblAvgScore.setText(String.format("%.2f", avgScore));
            if (lblHighestScore != null) lblHighestScore.setText(String.format("%.2f", highest));
            if (lblLowestScore != null) lblLowestScore.setText(String.format("%.2f", lowest));
         } else {
            if (lblAvgScore != null) lblAvgScore.setText("0.0");
            if (lblHighestScore != null) lblHighestScore.setText("0.0");
            if (lblLowestScore != null) lblLowestScore.setText("0.0");
         }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new StudentDashboardApp().setVisible(true);
        });
    }
}