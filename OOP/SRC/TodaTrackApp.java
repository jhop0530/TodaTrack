import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Comparator;

/**
 * Main application class for the TodaTrack GUI.
 * This class builds the JFrame and all Swing components, manages themes,
 * loads/saves data, and connects the UI to the TodaTrackSystem logic.
 *
 * This file contains the main() method and is the entry point for the
 * application.
 */
public class TodaTrackApp extends JFrame {

    // Core System
    private TodaTrackSystem todaSystem;
    private static final String SAVE_FILE_PREFIX = "todatrack_";
    private static final String SAVE_FILE_SUFFIX = ".dat";
    private static final SimpleDateFormat saveFileFormatter = new SimpleDateFormat("yyyy-MM-dd");

    // Theme Management
    private static boolean isDarkMode = false;
    private final ColorSet currentTheme;

    private ImageIcon appLogo;
    private ImageIcon splashLogo;
    private Timer autoRefreshTimer;
    private Timer clockTimer;
    private static final SimpleDateFormat clockFormatter = new SimpleDateFormat("h:mm:ss a");
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("h:mm:ss a");

    // GUI Components
    private JTabbedPane mainTabbedPane;
    private JList<Tricycle> studentWaitingList;
    private DefaultListModel<Tricycle> studentWaitingListModel;
    private JLabel totalLabel, activeLabel, waitingLabel;
    private JLabel studentClockLabel;
    private JTextArea broadcastArea;
    private JTabbedPane driverTabbedPane;
    private JLabel driverClockLabel;

    private DefaultListModel<Tricycle> registeredListModel, waitingListModel;
    private DefaultListModel<Trip> activeListModel, completedListModel;
    private JList<Tricycle> registeredList, waitingList;
    private JList<Trip> activeList, completedList;
    private JTextArea systemLogArea;
    private DefaultListModel<String> systemLogModel;
    private JTextField searchField;

    // Fonts
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_STAT_VALUE = new Font("Segoe UI", Font.BOLD, 36);
    private static final Font FONT_CLOCK = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_LOG = new Font("Monospaced", Font.PLAIN, 12);

    /**
     * Main constructor.
     * * @param existingSystem The data system (loaded from file or new)
     */
    public TodaTrackApp(TodaTrackSystem existingSystem) {
        this.todaSystem = existingSystem;
        this.currentTheme = isDarkMode ? Theme.DARK : Theme.LIGHT;

        loadAppLogo();

        systemLogModel = new DefaultListModel<>();
        logEvent("Application started and data loaded.");
        setTitle("TodaTrack: A Real-Time Digital Monitoring System");

        if (appLogo != null) {
            setIconImage(appLogo.getImage());
        }

        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        this.setBackground(currentTheme.background);

        // Add a custom window closing listener to ensure data is saved
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        this.setJMenuBar(createAppMenuBar());

        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(FONT_LABEL);

        // Force tab colors to respect theme
        UIManager.put("TabbedPane.foreground", currentTheme.foreground);
        UIManager.put("TabbedPane.background", currentTheme.panelBackground);
        UIManager.put("TabbedPane.selected", currentTheme.accentBlue);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.tabAreaInsets", new Insets(5, 5, 5, 5));

        mainTabbedPane.addTab("Student View", createStudentPanel());
        mainTabbedPane.addTab("Driver Dashboard", createDriverDashboard()); // Renamed method

        // Lock Driver Dashboard by default
        mainTabbedPane.setEnabledAt(1, false);
        mainTabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickedIndex = mainTabbedPane.indexAtLocation(e.getX(), e.getY());
                if (clickedIndex == 1 && !mainTabbedPane.isEnabledAt(1)) {
                    showLoginDialog();
                }
            }
        });

        add(mainTabbedPane);

        startTimers();
        updateAllGUILists();
    }

    /**
     * Logs a message to the in-memory system log.
     * * @param message The message to log.
     */
    private void logEvent(String message) {
        String timestamp = logFormatter.format(new Date());
        String logEntry = String.format("[%s] %s", timestamp, message);
        systemLogModel.add(0, logEntry);
        System.out.println(logEntry);
    }

    /**
     * Creates the main menu bar for the application.
     * * @return A JMenuBar with "File" and "Help" menus.
     */
    private JMenuBar createAppMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Save and Exit");
        exitItem.addActionListener(e -> exitApplication());
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About TodaTrack");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    /**
     * Handles the application shutdown gracefully.
     * Saves data, stops timers, and disposes of the frame.
     */
    private void exitApplication() {
        logEvent("Saving data and exiting...");
        saveData();
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
        if (clockTimer != null) {
            clockTimer.stop();
        }
        this.dispose();
        System.exit(0);
    }

    /**
     * Displays the "About" dialog with project and author information.
     */
    private void showAboutDialog() {
        JPanel aboutPanel = new JPanel(new BorderLayout(15, 15));
        aboutPanel.setBackground(UIManager.getColor("OptionPane.background"));
        aboutPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        if (appLogo != null) {
            aboutPanel.add(new JLabel(appLogo), BorderLayout.WEST);
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(UIManager.getColor("OptionPane.background"));

        JLabel titleLabel = new JLabel("TodaTrack: A Real-Time Digital Monitoring System");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(UIManager.getColor("OptionPane.messageForeground"));

        JLabel subtitleLabel = new JLabel("Case Study in Object Oriented Programming");
        subtitleLabel.setFont(FONT_LABEL.deriveFont(Font.ITALIC));
        subtitleLabel.setForeground(UIManager.getColor("OptionPane.messageForeground"));

        JLabel creatorsLabel = new JLabel("Submitted By:");
        creatorsLabel.setFont(FONT_LABEL.deriveFont(Font.BOLD));
        creatorsLabel.setForeground(UIManager.getColor("OptionPane.messageForeground"));

        JLabel namesLabel = new JLabel(
                "<html>" +
                        "Palapos, John Henry O.<br>" +
                        "Martinez, Prinze Jhemar C.<br>" +
                        "Ligsay, Allen Cardeck P.<br>" +
                        "Quiambao, CJ G.<br>" +
                        "Sarmiento, John David C." +
                        "</html>");
        namesLabel.setFont(FONT_LABEL);
        namesLabel.setForeground(UIManager.getColor("OptionPane.messageForeground"));

        JLabel instructorLabel = new JLabel("Submitted To: Ms. Jiane Monique A. Diamzon");
        instructorLabel.setFont(FONT_LABEL.deriveFont(Font.BOLD));
        instructorLabel.setForeground(UIManager.getColor("OptionPane.messageForeground"));

        textPanel.add(titleLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        textPanel.add(subtitleLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        textPanel.add(creatorsLabel);
        textPanel.add(namesLabel);
        textPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        textPanel.add(instructorLabel);

        aboutPanel.add(textPanel, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this,
                aboutPanel,
                "About TodaTrack",
                JOptionPane.PLAIN_MESSAGE);

        logEvent("User viewed the 'About' dialog.");
    }

    /**
     * Simple authentication logic.
     * * @param username The entered username.
     * 
     * @param password The entered password.
     * @return true if credentials are correct, false otherwise.
     */
    private boolean authenticate(String username, char[] password) {
        // Hardcoded credentials for this project
        boolean isCorrect = username.equals("admin") && Arrays.equals(password, "pass123".toCharArray());
        Arrays.fill(password, '0'); // Clear password from memory
        return isCorrect;
    }

    /**
     * Displays the login dialog to unlock the driver dashboard.
     */
    private void showLoginDialog() {
        logEvent("Driver login prompt displayed.");
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(UIManager.getColor("OptionPane.background"));
        JLabel label = new JLabel("Enter Admin Credentials to Unlock Dashboard:");
        label.setForeground(UIManager.getColor("OptionPane.messageForeground"));
        label.setFont(FONT_LABEL);
        panel.add(label, BorderLayout.NORTH);

        JPanel inputGrid = new JPanel(new GridLayout(2, 2, 5, 5));
        inputGrid.setBackground(UIManager.getColor("OptionPane.background"));

        JLabel userLabel = createDialogLabel("Username:");
        JTextField userField = new JTextField("admin"); // Pre-fill for convenience
        userField.setFont(FONT_LABEL);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(FONT_LABEL);
        passLabel.setForeground(UIManager.getColor("OptionPane.messageForeground"));

        JPasswordField passField = new JPasswordField("pass123"); // Pre-fill for convenience
        passField.setFont(FONT_LABEL);

        inputGrid.add(userLabel);
        inputGrid.add(userField);
        inputGrid.add(passLabel);
        inputGrid.add(passField);
        panel.add(inputGrid, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, "Driver Login", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            if (authenticate(userField.getText(), passField.getPassword())) {
                mainTabbedPane.setEnabledAt(1, true); // Unlock the tab
                mainTabbedPane.setSelectedIndex(1); // Switch to it
                JOptionPane.showMessageDialog(this, "Login Successful. Driver Dashboard unlocked.", "Access Granted",
                        JOptionPane.INFORMATION_MESSAGE);
                logEvent("Admin login successful.");
            } else {
                JOptionPane.showMessageDialog(this, "Access Denied. Invalid username or password.", "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
                logEvent("Admin login failed.");
            }
        }
    }

    /**
     * Loads the 'logo.png' file and creates scaled ImageIcons.
     */
    private void loadAppLogo() {
        try {
            // Assumes 'logo.png' is in the root directory
            Image originalImage = ImageIO.read(new File("logo.png"));
            Image scaledAppImage = originalImage.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            this.appLogo = new ImageIcon(scaledAppImage);
            Image scaledSplashImage = originalImage.getScaledInstance(128, 128, Image.SCALE_SMOOTH);
            this.splashLogo = new ImageIcon(scaledSplashImage);

        } catch (IOException e) {
            System.err.println("Error: 'logo.png' not found. App will run without logo.");
            this.appLogo = null;
            this.splashLogo = null;
        }
    }

    /**
     * Starts the timers for auto-refreshing the UI and updating the clocks.
     */
    private void startTimers() {
        // Auto-refreshes student view every 10 seconds
        autoRefreshTimer = new Timer(10000, e -> {
            if (mainTabbedPane.getSelectedIndex() == 0) {
                updateAllGUILists();
                logEvent("Student view auto-refreshed.");
            }
        });
        autoRefreshTimer.start();

        // Updates the clocks every second
        clockTimer = new Timer(1000, e -> {
            String currentTime = clockFormatter.format(new Date());
            if (studentClockLabel != null) {
                studentClockLabel.setText(currentTime);
            }
            if (driverClockLabel != null) {
                driverClockLabel.setText(currentTime);
            }
        });
        clockTimer.start();
    }

    /**
     * Creates the main panel for the "Student View" tab.
     * * @return A JPanel representing the student interface.
     */
    private JPanel createStudentPanel() {
        JPanel studentMainPanel = new JPanel(new BorderLayout(10, 10));
        studentMainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        studentMainPanel.setBackground(currentTheme.background);

        // Header
        JPanel studentHeaderPanel = new JPanel(new BorderLayout());
        studentHeaderPanel.setBackground(currentTheme.headerBackground);
        JLabel headerLabel = new JLabel("TodaTrack Student View");
        headerLabel.setFont(FONT_HEADER);
        headerLabel.setForeground(currentTheme.headerForeground);
        headerLabel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Right side of header (Clock + Theme)
        JPanel studentHeaderRightPanel = new JPanel();
        studentHeaderRightPanel.setOpaque(false);
        studentClockLabel = new JLabel(" ");
        studentClockLabel.setFont(FONT_CLOCK);
        studentClockLabel.setForeground(currentTheme.headerForeground);
        studentClockLabel.setBorder(new EmptyBorder(0, 0, 0, 15));
        JToggleButton toggleButton = createThemeToggleButton();
        studentHeaderRightPanel.add(studentClockLabel);
        studentHeaderRightPanel.add(toggleButton);

        if (appLogo != null) {
            JLabel logoLabel = new JLabel(appLogo);
            logoLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
            studentHeaderPanel.add(logoLabel, BorderLayout.WEST);
        }
        studentHeaderPanel.add(headerLabel, BorderLayout.CENTER);
        studentHeaderPanel.add(studentHeaderRightPanel, BorderLayout.EAST);

        // Stat Cards
        JPanel studentStatsPanel = new JPanel(new GridLayout(1, 3, 15, 15));
        studentStatsPanel.setBackground(currentTheme.background);
        totalLabel = createStatValueLabel();
        activeLabel = createStatValueLabel();
        waitingLabel = createStatValueLabel();
        studentStatsPanel.add(createStatCard("Total Tricycles", totalLabel));
        studentStatsPanel.add(createStatCard("Active Trips", activeLabel));
        studentStatsPanel.add(createStatCard("Waiting Trips", waitingLabel));

        // Waiting List
        studentWaitingListModel = new DefaultListModel<>();
        studentWaitingList = new JList<>(studentWaitingListModel);
        studentWaitingList.setCellRenderer(new StudentListRenderer());
        studentWaitingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentWaitingList.setBackground(currentTheme.panelBackground);
        JScrollPane scrollPane = new JScrollPane(studentWaitingList);
        scrollPane.setBackground(currentTheme.panelBackground);
        scrollPane.getViewport().setBackground(currentTheme.panelBackground);
        TitledBorder queueBorder = new TitledBorder("Waiting Trips (Available Drivers)");
        queueBorder.setTitleFont(FONT_TITLE);
        queueBorder.setTitleColor(currentTheme.foreground);
        scrollPane.setBorder(queueBorder);

        // Bottom Button Panel
        JPanel studentBottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        studentBottomPanel.setBackground(currentTheme.background);
        JButton viewContactButton = new JButton("View Driver's Contact");
        setupButton(viewContactButton, currentTheme.accentBlue);
        JButton refreshButton = new JButton("Refresh");
        setupButton(refreshButton, currentTheme.accentGray);
        studentBottomPanel.add(viewContactButton);
        studentBottomPanel.add(refreshButton);

        // Action Listeners
        viewContactButton.addActionListener(e -> viewDriverContact());
        refreshButton.addActionListener(e -> {
            updateAllGUILists();
            logEvent("Student view manually refreshed.");
        });

        // Assemble Main Panel
        studentMainPanel.add(studentHeaderPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(currentTheme.background);
        centerPanel.add(studentStatsPanel, BorderLayout.NORTH);
        centerPanel.add(createBroadcastPanel(), BorderLayout.CENTER);

        studentMainPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(currentTheme.background);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        bottomPanel.add(studentBottomPanel, BorderLayout.SOUTH);

        studentMainPanel.add(bottomPanel, BorderLayout.SOUTH);

        return studentMainPanel;
    }

    /**
     * Creates the read-only broadcast panel for announcements.
     * * @return A JPanel containing the broadcast message.
     */
    private JPanel createBroadcastPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(currentTheme.panelBackground);

        TitledBorder border = new TitledBorder("TODA Announcements");
        border.setTitleFont(FONT_TITLE);
        border.setTitleColor(currentTheme.foreground);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(10, 0, 0, 0),
                border));

        broadcastArea = new JTextArea(todaSystem.getBroadcastMessage());
        broadcastArea.setEditable(false);
        broadcastArea.setOpaque(false);
        broadcastArea.setFont(FONT_LABEL.deriveFont(Font.ITALIC));
        broadcastArea.setForeground(currentTheme.foreground);
        broadcastArea.setLineWrap(true);
        broadcastArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(broadcastArea);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the main panel for the "Driver Dashboard" tab.
     * * @return A JPanel representing the driver/admin interface.
     */
    private JPanel createDriverDashboard() {
        JPanel driverMainPanel = new JPanel(new BorderLayout(10, 10));
        driverMainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        driverMainPanel.setBackground(currentTheme.background);

        // Header Panel
        JPanel driverHeaderPanel = new JPanel(new BorderLayout());
        driverHeaderPanel.setBackground(currentTheme.headerBackground);
        JLabel headerLabel = new JLabel("TodaTrack Driver Dashboard");
        headerLabel.setFont(FONT_HEADER);
        headerLabel.setForeground(currentTheme.headerForeground);
        headerLabel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header Buttons/Controls
        JPanel driverHeaderRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        driverHeaderRightPanel.setOpaque(false);
        JButton registerButton = new JButton("Register New Driver");
        setupButton(registerButton, currentTheme.accentBlue);
        registerButton.addActionListener(e -> registerNewTricycle());
        JButton postMessageButton = new JButton("Post Message");
        setupButton(postMessageButton, currentTheme.accentGreen);
        postMessageButton.addActionListener(e -> postBroadcastMessage());
        driverClockLabel = new JLabel(" ");
        driverClockLabel.setFont(FONT_CLOCK);
        driverClockLabel.setForeground(currentTheme.headerForeground);
        driverClockLabel.setBorder(new EmptyBorder(0, 10, 0, 10));
        JToggleButton toggleButton = createThemeToggleButton();
        driverHeaderRightPanel.add(registerButton);
        driverHeaderRightPanel.add(postMessageButton);
        driverHeaderRightPanel.add(driverClockLabel);
        driverHeaderRightPanel.add(toggleButton);

        if (appLogo != null) {
            JLabel logoLabel = new JLabel(appLogo);
            logoLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
            driverHeaderPanel.add(logoLabel, BorderLayout.WEST);
        }
        driverHeaderPanel.add(headerLabel, BorderLayout.CENTER);
        driverHeaderPanel.add(driverHeaderRightPanel, BorderLayout.EAST);

        // Inner Tabbed Pane (Main Dashboard + System Log)
        driverTabbedPane = new JTabbedPane();
        driverTabbedPane.setFont(FONT_LABEL);
        driverTabbedPane.addTab("Main Dashboard", createDriverMainPanel());
        driverTabbedPane.addTab("System Log", createSystemLogPanel());

        // Assemble Panel
        driverMainPanel.add(driverHeaderPanel, BorderLayout.NORTH);
        driverMainPanel.add(driverTabbedPane, BorderLayout.CENTER);
        return driverMainPanel;
    }

    /**
     * Creates the panel for the "System Log" tab.
     * * @return A JPanel showing all logged events.
     */
    private JPanel createSystemLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(currentTheme.background);

        systemLogArea = new JTextArea(); // This component isn't actually used, JList is.
        systemLogArea.setEditable(false);
        systemLogArea.setFont(FONT_LOG);
        systemLogArea.setBackground(currentTheme.panelBackground);
        systemLogArea.setForeground(currentTheme.foreground);
        systemLogArea.setMargin(new Insets(10, 10, 10, 10));

        JList<String> logList = new JList<>(systemLogModel);
        logList.setFont(FONT_LOG);
        logList.setBackground(currentTheme.panelBackground);
        logList.setForeground(currentTheme.foreground);
        logList.setSelectionBackground(currentTheme.listSelectionBackground);
        logList.setSelectionForeground(currentTheme.listSelectionForeground);
        JScrollPane scrollPane = new JScrollPane(logList);
        scrollPane.setBorder(new LineBorder(currentTheme.borderColor, 1));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the 2x2 grid panel for the "Main Dashboard" tab.
     * * @return A JPanel containing the four list panels.
     */
    private JPanel createDriverMainPanel() {
        JPanel driverListGridPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        driverListGridPanel.setBackground(currentTheme.background);
        driverListGridPanel.setBorder(new EmptyBorder(15, 0, 0, 0)); // Space from top

        // Initialize models and renderers
        registeredListModel = new DefaultListModel<>();
        waitingListModel = new DefaultListModel<>();
        activeListModel = new DefaultListModel<>();
        completedListModel = new DefaultListModel<>();
        DashboardListRenderer renderer = new DashboardListRenderer();

        // Setup JLists
        registeredList = new JList<>(registeredListModel);
        registeredList.setCellRenderer(renderer);
        registeredList.setBackground(currentTheme.panelBackground);
        waitingList = new JList<>(waitingListModel);
        waitingList.setCellRenderer(renderer);
        waitingList.setBackground(currentTheme.panelBackground);
        activeList = new JList<>(activeListModel);
        activeList.setCellRenderer(new TripListRenderer());
        activeList.setBackground(currentTheme.panelBackground);
        completedList = new JList<>(completedListModel);
        completedList.setCellRenderer(new TripListRenderer());
        completedList.setBackground(currentTheme.panelBackground);

        // Add the four panels to the grid
        driverListGridPanel.add(createRegisteredPanelWithSearch());
        driverListGridPanel.add(createListPanel("Waiting Trips (On Duty)", waitingList, createWaitingButtons()));
        driverListGridPanel.add(createListPanel("Active Trips", activeList, createActiveButtons()));
        driverListGridPanel.add(createListPanel("Completed Trips (Today)", completedList, createCompletedButtons()));

        return driverListGridPanel;
    }

    /**
     * Creates the "Registered Tricycles" panel, which includes a search bar.
     * * @return A JPanel for the registered list and its controls.
     */
    private JPanel createRegisteredPanelWithSearch() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBackground(currentTheme.panelBackground);

        TitledBorder border = new TitledBorder("Registered Tricycles (Off Duty)");
        border.setTitleFont(FONT_TITLE);
        border.setTitleColor(currentTheme.foreground);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(5, 5, 5, 5),
                border));

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchPanel.setBackground(currentTheme.panelBackground);
        JLabel searchLabel = createDialogLabel("Search Driver:");
        searchPanel.add(searchLabel);
        searchField = new JTextField(25);
        searchField.setFont(FONT_LABEL);
        // DocumentListener to filter list as user types
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateAllGUILists();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateAllGUILists();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateAllGUILists();
            }
        });
        searchPanel.add(searchField);
        mainPanel.add(searchPanel, BorderLayout.NORTH);

        // List Scroll Pane
        JScrollPane scrollPane = new JScrollPane(registeredList);
        scrollPane.setBackground(currentTheme.panelBackground);
        scrollPane.getViewport().setBackground(currentTheme.panelBackground);
        scrollPane.setBorder(new LineBorder(currentTheme.borderColor, 1));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        mainPanel.add(createRegisteredButtons(), BorderLayout.SOUTH);

        return mainPanel;
    }

    /**
     * Creates the button panel for the "Registered" list.
     * * @return A JPanel with "Go On Duty", "Edit", and "Delete" buttons.
     */
    private JPanel createRegisteredButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBackground(currentTheme.panelBackground);
        JButton toggleDutyButton = new JButton("Go On Duty (Add to Queue)");
        setupButton(toggleDutyButton, currentTheme.accentGreen);
        JButton editButton = new JButton("Edit Driver");
        setupButton(editButton, currentTheme.accentBlue);
        JButton deleteButton = new JButton("Delete Driver");
        setupButton(deleteButton, currentTheme.accentRed);

        // Action Listeners
        toggleDutyButton.addActionListener(e -> toggleDuty(true));
        editButton.addActionListener(e -> editDriver());
        deleteButton.addActionListener(e -> deleteDriver());

        panel.add(toggleDutyButton);
        panel.add(editButton);
        panel.add(deleteButton);
        return panel;
    }

    /**
     * Creates the button panel for the "Waiting" list.
     * * @return A JPanel with "Start Trip" and "Go Off Duty" buttons.
     */
    private JPanel createWaitingButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBackground(currentTheme.panelBackground);
        JButton startTripButton = new JButton("Start Trip for Selected Driver"); // MODIFIED
        setupButton(startTripButton, currentTheme.accentGreen);
        JButton goOffDutyButton = new JButton("Go Off Duty (Remove)");
        setupButton(goOffDutyButton, currentTheme.accentRed);

        // Action Listeners
        startTripButton.addActionListener(e -> startTrip());
        goOffDutyButton.addActionListener(e -> toggleDuty(false));

        panel.add(startTripButton);
        panel.add(goOffDutyButton);
        return panel;
    }

    /**
     * Creates the button panel for the "Active" list.
     * * @return A JPanel with a "Complete Trip" button.
     */
    private JPanel createActiveButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBackground(currentTheme.panelBackground);
        JButton completeTripButton = new JButton("Complete Trip");
        setupButton(completeTripButton, currentTheme.accentBlue);

        // Action Listener
        completeTripButton.addActionListener(e -> completeTrip());

        panel.add(completeTripButton);
        return panel;
    }

    /**
     * Creates the button panel for the "Completed" list.
     * * @return A JPanel with an "End of Day Report" button.
     */
    private JPanel createCompletedButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBackground(currentTheme.panelBackground);
        JButton endOfDayButton = new JButton("Run End of Day Report");
        setupButton(endOfDayButton, currentTheme.accentGreen);

        // Action Listener
        endOfDayButton.addActionListener(e -> runEndOfDayReport());

        panel.add(endOfDayButton);
        return panel;
    }

    /**
     * Shows a dialog to post a new broadcast message.
     */
    private void postBroadcastMessage() {
        String currentMessage = todaSystem.getBroadcastMessage();
        if (currentMessage.equals("No announcements at this time.")) {
            currentMessage = "";
        }
        JTextArea messageInputArea = new JTextArea(currentMessage, 5, 30);
        messageInputArea.setFont(FONT_LABEL);
        messageInputArea.setLineWrap(true);
        messageInputArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageInputArea);

        int result = JOptionPane.showConfirmDialog(this,
                scrollPane,
                "Post Announcement",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newMessage = messageInputArea.getText();
            todaSystem.setBroadcastMessage(newMessage);
            updateAllGUILists(); // This will update the broadcastArea on the student tab
            logEvent("Admin posted new broadcast message.");
        }
    }

    /**
     * Runs the End of Day report, archives completed trips, and shows a summary.
     */
    private void runEndOfDayReport() {
        if (completedListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no completed trips to report.", "End of Day Report",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to end the day?\nThis will archive all completed trips and clear the list.",
                "Confirm End of Day",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            String report = todaSystem.endOfDayReportAndClear();

            JTextArea textArea = new JTextArea(report);
            textArea.setFont(FONT_LABEL);
            textArea.setEditable(false);
            textArea.setOpaque(false);
            textArea.setForeground(UIManager.getColor("OptionPane.messageForeground"));
            JOptionPane.showMessageDialog(this, textArea, "End of Day Report", JOptionPane.INFORMATION_MESSAGE);

            logEvent("'End of Day' report was run.");
            updateAllGUILists();
        }
    }

    /**
     * Displays the contact information for the selected driver in the student view.
     */
    private void viewDriverContact() {
        Tricycle selectedTricycle = studentWaitingList.getSelectedValue();
        if (selectedTricycle == null) {
            JOptionPane.showMessageDialog(this, "Please select a driver from the list first.", "No Driver Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Driver driver = selectedTricycle.getDriver();
        JOptionPane.showMessageDialog(this, driver.getContactInfo(), "Driver Contact", JOptionPane.INFORMATION_MESSAGE);
        logEvent("User viewed contact for " + driver.getName());
    }

    /**
     * Shows a dialog to edit the details of a selected registered driver.
     */
    private void editDriver() {
        Tricycle selectedTricycle = registeredList.getSelectedValue();
        if (selectedTricycle == null) {
            JOptionPane.showMessageDialog(this, "Please select a driver from the 'Registered' list to edit.",
                    "No Driver Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // *** MODIFIED ***
        // Use the new, dedicated dialog
        EditDriverDialog dialog = new EditDriverDialog(this, selectedTricycle);
        dialog.setVisible(true);

        if (dialog.isSucceeded()) {
            // Get data from the dialog and update the model
            String newName = dialog.getDriverName();
            String newContact = dialog.getContactNum();
            String newPlate = dialog.getPlateNum();

            // Update the model
            selectedTricycle.getDriver().setName(newName);
            selectedTricycle.getDriver().setContactNum(newContact);
            selectedTricycle.setPlateNumber(newPlate);

            updateAllGUILists(); // Refresh the UI
            logEvent("Updated details for " + newName);
            JOptionPane.showMessageDialog(this, "Driver details updated successfully.", "Update Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        // *** END OF MODIFICATION ***
    }

    /**
     * Shows a confirmation dialog and deletes a selected registered driver.
     */
    private void deleteDriver() {
        Tricycle selectedTricycle = registeredList.getSelectedValue();
        if (selectedTricycle == null) {
            JOptionPane.showMessageDialog(this, "Please select a driver from the 'Registered' list to delete.",
                    "No Driver Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to permanently delete this driver?\nDriver: "
                        + selectedTricycle.getDriver().getName() + "\nPlate: " + selectedTricycle.getPlateNumber(),
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            String deletedName = selectedTricycle.getDriver().getName();
            todaSystem.removeTricycle(selectedTricycle); // Remove from the system
            updateAllGUILists(); // Refresh the UI
            logEvent("Deleted driver: " + deletedName);
            JOptionPane.showMessageDialog(this, "Driver has been deleted.", "Deletion Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Shows a dialog to register a new driver and tricycle.
     */
    private void registerNewTricycle() {
        // *** MODIFIED ***
        // Use the new, dedicated dialog
        RegisterDriverDialog dialog = new RegisterDriverDialog(this);
        dialog.setVisible(true);

        if (dialog.isSucceeded()) {
            // Get data from the dialog
            String driverName = dialog.getDriverName();
            String contactNum = dialog.getContactNum();
            String plateNum = dialog.getPlateNum();

            // Create and register the new objects
            Driver newDriver = new Driver(driverName, contactNum);
            Tricycle newTricycle = new Tricycle(plateNum, "Regular", 20.00);
            newTricycle.setDriver(newDriver);
            todaSystem.registerTricycle(newTricycle); // Add to the system

            updateAllGUILists(); // Refresh the UI
            logEvent("Registered new driver: " + driverName);
        }
        // *** END OF MODIFICATION ***
    }

    /**
     * Toggles a driver's duty status (On Duty vs. Off Duty).
     * * @param goOnDuty true to move from Registered to Waiting, false to move from
     * Waiting to Registered.
     */
    private void toggleDuty(boolean goOnDuty) {
        Tricycle selectedTricycle;

        if (goOnDuty) {
            selectedTricycle = registeredList.getSelectedValue();
            if (selectedTricycle == null) {
                JOptionPane.showMessageDialog(this, "Please select a tricycle from the 'Registered' list.");
                return;
            }

            int choice = JOptionPane.showConfirmDialog(this,
                    "Add this driver to the end of the waiting queue?\n\nDriver: "
                            + selectedTricycle.getDriver().getName(),
                    "Confirm Go On Duty",
                    JOptionPane.YES_NO_OPTION);

            if (choice != JOptionPane.YES_OPTION)
                return;

            // Update Model
            selectedTricycle.getDriver().setAvailability(true);
            selectedTricycle.setStatus("Waiting");
            todaSystem.getWaitingQueue().add(selectedTricycle); // Add to end of queue
            logEvent(selectedTricycle.getDriver().getName() + " went ON DUTY.");

        } else {
            selectedTricycle = waitingList.getSelectedValue();
            if (selectedTricycle == null) {
                JOptionPane.showMessageDialog(this,
                        "Please select a tricycle from the 'Waiting' list to move off duty.");
                return;
            }

            int choice = JOptionPane.showConfirmDialog(this,
                    "Remove this driver from the queue and set as Off Duty?\n\nDriver: "
                            + selectedTricycle.getDriver().getName(),
                    "Confirm Go Off Duty",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (choice != JOptionPane.YES_OPTION)
                return;

            // Update Model
            selectedTricycle.getDriver().setAvailability(false);
            selectedTricycle.setStatus("Unavailable");
            todaSystem.getWaitingQueue().remove(selectedTricycle); // Remove from queue
            logEvent(selectedTricycle.getDriver().getName() + " went OFF DUTY.");
        }
        updateAllGUILists(); // Refresh the UI
    }

    /**
     * Shows a dialog to start a new trip for the *selected* driver in the waiting
     * list.
     */
    private void startTrip() {
        // MODIFIED: Get the *selected* tricycle from the waiting list
        Tricycle selectedTricycle = waitingList.getSelectedValue();

        if (selectedTricycle == null) {
            JOptionPane.showMessageDialog(this, "Please select a driver from the 'Waiting' list first.",
                    "No Driver Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // *** MODIFIED ***
        // Use the new, dedicated dialog
        StartTripDialog dialog = new StartTripDialog(this, selectedTricycle);
        dialog.setVisible(true);

        if (dialog.isSucceeded()) {
            // Get data from the dialog
            String from = dialog.getFromLocation();
            int passengerCount = dialog.getPassengerCount();
            double farePerPassenger = dialog.getFarePerPassenger();
            String destination = dialog.getDestination();
            double totalFare = farePerPassenger * passengerCount;

            String driverName = selectedTricycle.getDriver().getName();

            // Call the system logic
            todaSystem.startTrip(selectedTricycle, passengerCount, from, totalFare, destination);

            updateAllGUILists(); // Refresh UI
            logEvent(driverName + " started a trip to " + destination);
        }
        // *** END OF MODIFICATION ***
    }

    /**
     * Completes the selected active trip and prompts for the driver's next status.
     */
    private void completeTrip() {
        Trip selectedTrip = activeList.getSelectedValue();
        if (selectedTrip == null) {
            JOptionPane.showMessageDialog(this, "Please select a trip from the 'Active' list.");
            return;
        }

        // Update Model
        todaSystem.endTrip(selectedTrip);

        Tricycle driverTricycle = selectedTrip.getTricycle();
        logEvent(driverTricycle.getDriver().getName() + " completed Trip #" + selectedTrip.getTripId());

        // Ask for next action
        Object[] options = { "Stay On Duty (Go to end of queue)", "Go Off Duty" };
        int choice = JOptionPane.showOptionDialog(this,
                "Trip for " + driverTricycle.getDriver().getName() + " is complete.\nWhat's next?", "Trip Completed",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            // Stay On Duty
            todaSystem.getWaitingQueue().add(driverTricycle);
            logEvent(driverTricycle.getDriver().getName() + " returned to queue.");
        } else {
            // Go Off Duty
            driverTricycle.getDriver().setAvailability(false);
            driverTricycle.setStatus("Unavailable");
            logEvent(driverTricycle.getDriver().getName() + " went OFF DUTY.");
        }

        updateAllGUILists(); // Refresh UI
    }

    /**
     * Refreshes all JLists and JLabels with data from the TodaTrackSystem.
     * This is the central method for keeping the GUI in sync with the model.
     */
    private void updateAllGUILists() {
        String filterText = "";
        if (searchField != null) {
            filterText = searchField.getText().toLowerCase();
        }

        // Clear all models
        registeredListModel.clear();
        waitingListModel.clear();
        activeListModel.clear();
        completedListModel.clear();
        studentWaitingListModel.clear();

        if (broadcastArea != null) {
            broadcastArea.setText(todaSystem.getBroadcastMessage());
        }

        // Repopulate Registered list (Off Duty drivers)
        for (Tricycle t : todaSystem.getAllTricycles()) {
            if ("Unavailable".equals(t.getStatus()) || t.getStatus() == null) {
                if (t.getDriver().getName().toLowerCase().startsWith(filterText)) {
                    registeredListModel.addElement(t);
                }
            }
        }

        // Repopulate Waiting lists (Student and Driver)
        for (Tricycle t : todaSystem.getWaitingQueue()) {
            waitingListModel.addElement(t);
            studentWaitingListModel.addElement(t);
        }

        // Repopulate Active and Completed lists
        for (Trip trip : todaSystem.getTrips()) {
            if (trip.isActive()) {
                activeListModel.addElement(trip);
            } else {
                completedListModel.addElement(trip);
            }
        }

        // Update stat labels
        totalLabel.setText(String.valueOf(todaSystem.getAllTricycles().size()));
        activeLabel.setText(String.valueOf(activeListModel.getSize()));
        waitingLabel.setText(String.valueOf(waitingListModel.getSize()));
    }

    /**
     * Saves the current TodaTrackSystem object to a file.
     * The filename is based on the current date.
     */
    private void saveData() {
        String todayDate = saveFileFormatter.format(new Date());
        String saveFileName = SAVE_FILE_PREFIX + todayDate + SAVE_FILE_SUFFIX;

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFileName))) {
            oos.writeObject(todaSystem);
            logEvent("Data saved successfully to " + saveFileName);
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
            e.printStackTrace();
            logEvent("CRITICAL: Failed to save data!");
        }
    }

    /**
     * Loads the most recent TodaTrackSystem object from a save file.
     * * @return A loaded TodaTrackSystem, or a new one if no save file is found.
     */
    private static TodaTrackSystem loadData() {
        File latestSaveFile = getLatestSaveFile();
        if (latestSaveFile == null) {
            System.out.println("No save data found. Starting a new system.");
            return new TodaTrackSystem(); // Return a fresh system
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(latestSaveFile))) {
            TodaTrackSystem loadedSystem = (TodaTrackSystem) ois.readObject();
            System.out.println("Data loaded successfully from " + latestSaveFile.getName());

            // Re-initialize the static trip counter to prevent ID conflicts
            int maxTripId = 0;
            for (Trip trip : loadedSystem.getTrips()) {
                if (trip.getTripId() > maxTripId)
                    maxTripId = trip.getTripId();
            }
            if (loadedSystem.getTripArchive() != null) {
                for (Trip trip : loadedSystem.getTripArchive()) {
                    if (trip.getTripId() > maxTripId)
                        maxTripId = trip.getTripId();
                }
            }
            Trip.setTripCounter(maxTripId + 1);

            return loadedSystem;
        } catch (Exception e) {
            System.err.println("Error reading save file '" + latestSaveFile.getName() + "': " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Error loading data file: " + latestSaveFile.getName() + "\n\nStarting a new, empty system.",
                    "Data Load Error", JOptionPane.ERROR_MESSAGE);
            return new TodaTrackSystem();
        }
    }

    /**
     * Finds the most recent save file in the current directory.
     * * @return A File object, or null if no save files are found.
     */
    private static File getLatestSaveFile() {
        File directory = new File(".");
        File[] files = directory
                .listFiles((dir, name) -> name.startsWith(SAVE_FILE_PREFIX) && name.endsWith(SAVE_FILE_SUFFIX));

        if (files == null || files.length == 0) {
            return null;
        }

        // Sort files by name in reverse order to get the latest date
        Arrays.sort(files, Comparator.comparing(File::getName).reversed());

        return files[0]; // Return the first file (latest date)
    }

    /**
     * Helper method to create a standardized JLabel for dialogs.
     * * @param text The text for the label.
     * 
     * @return A JLabel formatted for dialogs.
     */
    private JLabel createDialogLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(UIManager.getColor("OptionPane.messageForeground"));
        return label;
    }

    /**
     * Creates the theme toggle button.
     * * @return A JToggleButton for switching themes.
     */
    private JToggleButton createThemeToggleButton() {
        JToggleButton toggleButton = new JToggleButton();
        toggleButton.setFont(FONT_BUTTON);
        toggleButton.setOpaque(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setBorderPainted(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setForeground(currentTheme.headerForeground);

        if (isDarkMode) {
            toggleButton.setText("Light Mode");
            toggleButton.setSelected(true);
        } else {
            toggleButton.setText("Dark Mode");
            toggleButton.setSelected(false);
        }

        // Action listener to restart the app with the new theme
        toggleButton.addActionListener(e -> {
            TodaTrackApp.isDarkMode = toggleButton.isSelected();
            this.dispose(); // Close the current window
            autoRefreshTimer.stop();
            clockTimer.stop();
            logEvent("Theme switched to " + (isDarkMode ? "Dark" : "Light"));
            new TodaTrackApp(this.todaSystem).setVisible(true); // Create a new app instance
        });
        return toggleButton;
    }

    /**
     * Helper method to create a standardized JLabel for stat cards.
     * * @return A JLabel formatted for stat values.
     */
    private JLabel createStatValueLabel() {
        JLabel label = new JLabel("0", SwingConstants.CENTER);
        label.setFont(FONT_STAT_VALUE);
        label.setForeground(currentTheme.foreground);
        return label;
    }

    /**
     * Helper method to create a styled statistic card.
     * * @param title The title for the card (e.g., "Total Tricycles").
     * 
     * @param valueLabel The JLabel that will hold the data.
     * @return A JPanel formatted as a card.
     */
    private JPanel createStatCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(currentTheme.panelBackground);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(currentTheme.borderColor, 1),
                new EmptyBorder(15, 15, 15, 15)));
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(currentTheme.foreground);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    /**
     * Helper method to create a standardized list panel with a title and buttons.
     * * @param title The title for the TitledBorder.
     * 
     * @param list        The JList to display.
     * @param buttonPanel The JPanel containing buttons for this list.
     * @return A JPanel formatted as a list panel.
     */
    private JPanel createListPanel(String title, JList<?> list, JPanel buttonPanel) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(currentTheme.panelBackground);

        TitledBorder border = new TitledBorder(title);
        border.setTitleFont(FONT_TITLE);
        border.setTitleColor(currentTheme.foreground);

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBackground(currentTheme.panelBackground);
        scrollPane.getViewport().setBackground(currentTheme.panelBackground);
        scrollPane.setBorder(new LineBorder(currentTheme.borderColor, 1));

        panel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(5, 5, 5, 5),
                border));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(scrollPane, BorderLayout.CENTER);
        buttonPanel.setBackground(currentTheme.panelBackground);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Helper method to style a JButton with theme colors.
     * * @param button The button to style.
     * 
     * @param color The background color for the button.
     */
    private void setupButton(JButton button, Color color) {
        button.setFont(FONT_BUTTON);
        button.setBackground(color);
        button.setForeground(currentTheme.accentForeground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(8, 15, 8, 15));
    }

    /**
     * Helper method to convert a Color object to a hex string for HTML rendering.
     * * @param c The Color to convert.
     * 
     * @return A hex string (e.g., "#FF0000").
     */
    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * Custom ListCellRenderer for the Student View's waiting list.
     */
    class StudentListRenderer extends JLabel implements ListCellRenderer<Tricycle> {
        public StudentListRenderer() {
            setOpaque(true);
            setBorder(new EmptyBorder(5, 10, 5, 10));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Tricycle> list, Tricycle tricycle, int index,
                boolean isSelected, boolean cellHasFocus) {
            String text = String.format("#%d %s (Plate: %s)", (index + 1), tricycle.getDriver().getName(),
                    tricycle.getPlateNumber());
            setText(text);
            setFont(FONT_LABEL);

            if (isSelected) {
                setBackground(currentTheme.listSelectionBackground);
                setForeground(currentTheme.listSelectionForeground);
            } else {
                setBackground(currentTheme.panelBackground);
                setForeground(currentTheme.foreground);
            }
            return this;
        }
    }

    /**
     * Custom ListCellRenderer for the Driver Dashboard's "Registered" and "Waiting"
     * lists.
     * Uses HTML for multi-line formatting.
     */
    class DashboardListRenderer extends JLabel implements ListCellRenderer<Tricycle> {
        public DashboardListRenderer() {
            setOpaque(true); // <--- THIS WAS THE FIX
            setBorder(new EmptyBorder(5, 10, 5, 10));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Tricycle> list, Tricycle tricycle, int index,
                boolean isSelected, boolean cellHasFocus) {

            String driverName = tricycle.getDriver().getName();
            String plate = tricycle.getPlateNumber();
            String status = tricycle.getStatus();
            String textColor, secondaryTextColor;

            if (isSelected) {
                setBackground(currentTheme.listSelectionBackground);
                textColor = toHex(currentTheme.listSelectionForeground);
                secondaryTextColor = toHex(currentTheme.listSelectionForeground.brighter());
            } else {
                // Alternating row colors
                setBackground(index % 2 == 0 ? currentTheme.panelBackground : currentTheme.background);
                textColor = toHex(currentTheme.foreground);
                secondaryTextColor = toHex(currentTheme.foreground.darker());
            }

            // HTML for multi-line rendering in the JList
            String html = String.format(
                    "<html><body style='color: %s; width: 250px;'>" +
                            "<strong style='font-size: 1.2em;'>%s</strong>" +
                            "<br>Plate: %s" +
                            "<br><i style='color: %s;'>Status: %s</i>" +
                            "</div></html>",
                    textColor, driverName, plate, secondaryTextColor, status);
            setText(html);
            return this;
        }
    }

    /**
     * Custom ListCellRenderer for the "Active" and "Completed" trip lists.
     * Uses the Trip's HTML-formatted toString() method.
     */
    class TripListRenderer extends JLabel implements ListCellRenderer<Trip> {
        public TripListRenderer() {
            setOpaque(true);
            setBorder(new EmptyBorder(5, 10, 5, 10));
            setFont(FONT_LABEL);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Trip> list, Trip trip, int index,
                boolean isSelected, boolean cellHasFocus) {
            setText(trip.toString()); // Relies on Trip.toString() returning HTML

            if (isSelected) {
                setBackground(currentTheme.listSelectionBackground);
                setForeground(currentTheme.listSelectionForeground);
            } else {
                // Alternating row colors
                setBackground(index % 2 == 0 ? currentTheme.panelBackground : currentTheme.background);
                setForeground(currentTheme.foreground);
            }
            return this;
        }
    }

    /**
     * Main method. Entry point of the application.
     * Creates a splash screen, loads data, and then launches the main GUI.
     * * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {

        final int MIN_SPLASH_TIME = 2500; // Minimum 2.5 seconds
        long startTime = System.currentTimeMillis();

        // Create and show splash screen
        JWindow splash = createSplashScreen();
        if (splash != null) {
            splash.setVisible(true);
        }

        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load data in the background
        TodaTrackSystem loadedSystem = loadData();

        // Enforce minimum splash time
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        if (duration < MIN_SPLASH_TIME) {
            try {
                Thread.sleep(MIN_SPLASH_TIME - duration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Close splash and open main app
        if (splash != null) {
            splash.setVisible(false);
            splash.dispose();
        }

        // Launch the main app on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            TodaTrackApp app = new TodaTrackApp(loadedSystem);
            app.setVisible(true);
        });
    }

    /**
     * Creates the splash screen window.
     * * @return A JWindow for the splash screen, or null if 'logo.png' is missing.
     */
    private static JWindow createSplashScreen() {
        try {
            JWindow window = new JWindow();
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(new LineBorder(new Color(0xAE2C2F), 3)); // Storyboard Red border

            Image originalImage = ImageIO.read(new File("logo.png"));
            Image scaledSplashImage = originalImage.getScaledInstance(128, 128, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaledSplashImage));

            logoLabel.setBorder(new EmptyBorder(20, 20, 10, 20));
            panel.add(logoLabel, BorderLayout.CENTER);

            JLabel loadingLabel = new JLabel("Loading data, please wait...", SwingConstants.CENTER);
            loadingLabel.setFont(FONT_LABEL);
            loadingLabel.setBorder(new EmptyBorder(10, 20, 20, 20));
            panel.add(loadingLabel, BorderLayout.SOUTH);

            window.add(panel);
            window.pack();
            window.setLocationRelativeTo(null);
            return window;
        } catch (IOException e) {
            System.err.println("logo.png not found. Skipping splash screen.");
            return null;
        }
    }

    /**
     * Static inner class to hold color definitions for themes.
     */
    static class ColorSet {
        Color background, panelBackground, foreground, headerBackground, headerForeground;
        Color accentGreen, accentRed, accentBlue, accentGray, accentForeground;
        Color listSelectionBackground, listSelectionForeground, borderColor;
    }

    /**
     * Static inner class to define the Light and Dark theme color sets.
     */
    static class Theme {
        public static final ColorSet LIGHT = new ColorSet();
        public static final ColorSet DARK = new ColorSet();

        static {
            // LIGHT THEME
            LIGHT.background = Color.WHITE;
            LIGHT.panelBackground = new Color(0xF8F9FA); // Off-white
            LIGHT.foreground = Color.BLACK;
            LIGHT.headerBackground = new Color(0xAE2C2F); // Storyboard Red
            LIGHT.headerForeground = Color.WHITE;
            LIGHT.accentGreen = new Color(40, 167, 69);
            LIGHT.accentRed = new Color(220, 53, 69);
            LIGHT.accentBlue = new Color(0, 123, 255);
            LIGHT.accentGray = new Color(108, 117, 125);
            LIGHT.accentForeground = Color.WHITE; // Text on buttons
            LIGHT.listSelectionBackground = new Color(0, 123, 255);
            LIGHT.listSelectionForeground = Color.WHITE;
            LIGHT.borderColor = new Color(0xDDDDDD);

            // DARK THEME
            DARK.background = new Color(0x212529); // Dark Gray
            DARK.panelBackground = new Color(0x343A40); // Darker Gray
            DARK.foreground = Color.LIGHT_GRAY;
            DARK.headerBackground = new Color(0xAE2C2F); // Storyboard Red (constant)
            DARK.headerForeground = Color.WHITE; // Brighter text for dark header
            DARK.accentGreen = new Color(40, 167, 69);
            DARK.accentRed = new Color(220, 53, 69);
            DARK.accentBlue = new Color(0, 123, 255);
            DARK.accentGray = new Color(108, 117, 125);
            DARK.accentForeground = Color.WHITE; // White text on buttons
            DARK.listSelectionBackground = new Color(0, 123, 255);
            DARK.listSelectionForeground = Color.WHITE;
            DARK.borderColor = new Color(0x495057); // Gray border
        }
    }
}