package os.gui;

import os.Main;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimulatorGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color PAGE = new Color(243, 239, 233);
    private static final Color PANEL = new Color(255, 252, 247);
    private static final Color PANEL_ALT = new Color(247, 244, 238);
    private static final Color GERMANY_BLACK = new Color(26, 26, 28);
    private static final Color GERMANY_RED = new Color(168, 36, 49);
    private static final Color GERMANY_GOLD = new Color(210, 167, 55);
    private static final Color TEXT = new Color(32, 36, 40);
    private static final Color MUTED = new Color(96, 102, 110);
    private static final Color LINE = new Color(211, 205, 194);

    private final JComboBox<String> algorithmBox = new JComboBox<>(new String[]{"HRRN", "RR", "MLFQ"});
    private final JTextField quantumField = new JTextField("2", 6);

    private final JTextField p1StartField = new JTextField("", 12);
    private final JTextField p1EndField = new JTextField("", 12);
    private final JTextField p2FileField = new JTextField("", 12);
    private final JTextField p2DataField = new JTextField("", 12);
    private final JTextField p3FileField = new JTextField("", 12);

    private final JTextArea outputArea = new JTextArea();
    private final JTextArea guideArea = new JTextArea();

    private final JButton runButton = new JButton("Run Simulation");
    private final JButton hrrnDemoButton = new JButton("Load HRRN Demo");
    private final JButton rrDemoButton = new JButton("Load RR Demo");
    private final JButton mlfqDemoButton = new JButton("Load MLFQ Demo");
    private final JButton clearButton = new JButton("Clear Output");
    private final JLabel statusValue = new JLabel("Idle");
    private final JLabel schedulerValue = new JLabel("HRRN");
    private final JLabel outputCountValue = new JLabel("0");
    private final JLabel swapCountValue = new JLabel("0");
    private final JLabel finishCountValue = new JLabel("0");
    private final JLabel currentClockValue = new JLabel("Not started");
    private final JLabel runningProcessValue = new JLabel("None");
    private final JTextArea readyQueueArea = new JTextArea();
    private final JTextArea blockedQueueArea = new JTextArea();
    private final JTextArea swapArea = new JTextArea();
    private final JTextArea memoryFrameArea = new JTextArea();
    private final JTextArea frameTraceArea = new JTextArea();
    private final JButton playPauseButton = new JButton("Play");
    private final JButton nextStepButton = new JButton("Next Step");
    private final JButton previousStepButton = new JButton("Previous Step");
    private final JButton resetViewButton = new JButton("Reset View");
    private final Map<String, JLabel> processStateLabels = new LinkedHashMap<String, JLabel>();
    private final List<ClockFrame> frames = new ArrayList<ClockFrame>();
    private int currentFrameIndex = -1;
    private Timer playbackTimer;

    public SimulatorGUI() {
        super("GUC Operating Systems Simulator");

        installLookAndFeel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1260, 780));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        outputArea.setEditable(false);
        outputArea.setLineWrap(false);
        outputArea.setWrapStyleWord(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setBackground(new Color(250, 248, 243));
        outputArea.setForeground(TEXT);
        outputArea.setMargin(new java.awt.Insets(14, 14, 14, 14));

        guideArea.setEditable(false);
        guideArea.setLineWrap(true);
        guideArea.setWrapStyleWord(true);
        guideArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        guideArea.setBackground(PANEL);
        guideArea.setForeground(TEXT);
        guideArea.setMargin(new java.awt.Insets(18, 18, 18, 18));
        guideArea.setText(buildGuideText());
        configureReadOnlyArea(readyQueueArea, 13, true);
        configureReadOnlyArea(blockedQueueArea, 13, true);
        configureReadOnlyArea(swapArea, 12, true);
        configureReadOnlyArea(memoryFrameArea, 12, false);
        configureReadOnlyArea(frameTraceArea, 12, true);
        currentClockValue.setFont(new Font("Georgia", Font.BOLD, 22));
        currentClockValue.setForeground(TEXT);
        runningProcessValue.setFont(new Font("Georgia", Font.BOLD, 22));
        runningProcessValue.setForeground(GERMANY_RED);
        playbackTimer = new Timer(1200, event -> advanceFrame());
        playbackTimer.setRepeats(true);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(PAGE);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);

        setContentPane(root);
        refreshQuantumFieldState();
        clearFrameViews();
    }

    private void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 16));
        header.setBackground(GERMANY_BLACK);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(11, 11, 11), 1),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JPanel stripe = new JPanel(new GridLayout(1, 3, 0, 0));
        stripe.setMaximumSize(new Dimension(160, 10));
        stripe.add(colorStrip(GERMANY_BLACK));
        stripe.add(colorStrip(GERMANY_RED));
        stripe.add(colorStrip(GERMANY_GOLD));

        JLabel title = new JLabel("German University in Cairo");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Georgia", Font.BOLD, 28));

        JLabel subtitle = new JLabel("Operating Systems Project Simulator");
        subtitle.setForeground(new Color(232, 232, 232));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        JLabel team = new JLabel("Team: Windows XP");
        team.setForeground(new Color(255, 222, 128));
        team.setFont(new Font("Segoe UI", Font.BOLD, 15));

        left.add(stripe);
        left.add(Box.createVerticalStrut(12));
        left.add(title);
        left.add(Box.createVerticalStrut(6));
        left.add(subtitle);
        left.add(Box.createVerticalStrut(6));
        left.add(team);

        JPanel right = new JPanel(new GridLayout(1, 3, 10, 10));
        right.setOpaque(false);
        right.add(headerBadge("Scheduler", "HRRN / RR / MLFQ", GERMANY_RED));
        right.add(headerBadge("Backend", "Ready", GERMANY_GOLD));
        right.add(headerBadge("Style", "GUC Demo", new Color(43, 119, 113)));

        header.add(left, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel colorStrip(Color color) {
        JPanel panel = new JPanel();
        panel.setBackground(color);
        return panel;
    }

    private JPanel headerBadge(String label, String value, Color color) {
        JPanel badge = new JPanel(new BorderLayout(4, 4));
        badge.setBackground(color);
        badge.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel top = new JLabel(label);
        top.setForeground(Color.WHITE);
        top.setFont(new Font("Segoe UI", Font.BOLD, 11));

        JLabel bottom = new JLabel(value);
        bottom.setForeground(Color.WHITE);
        bottom.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        badge.add(top, BorderLayout.NORTH);
        badge.add(bottom, BorderLayout.CENTER);
        return badge;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("Dashboard", buildDashboardTab());
        tabs.addTab("Team Windows XP", buildTeamTab());
        tabs.addTab("Evaluation Guide", buildGuideTab());
        return tabs;
    }

    private JComponent buildDashboardTab() {
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(430, 0));
        left.add(buildSchedulerPanel());
        left.add(Box.createVerticalStrut(14));
        left.add(buildInputsPanel());
        left.add(Box.createVerticalStrut(14));
        left.add(buildActionsPanel());
        left.add(Box.createVerticalStrut(14));
        left.add(buildBrandStoryPanel());

        JPanel right = new JPanel(new BorderLayout(14, 14));
        right.setOpaque(false);
        right.add(buildMetricsPanel(), BorderLayout.NORTH);
        right.add(buildVisualizationTabs(), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        splitPane.setResizeWeight(0.33);
        splitPane.setDividerSize(12);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        return splitPane;
    }

    private JComponent buildGuideTab() {
        JPanel guidePanel = createSectionPanel("Evaluation Guide", "Use this during your viva, demo, or project walkthrough.");
        guidePanel.add(wrapScroll(guideArea), BorderLayout.CENTER);
        return guidePanel;
    }

    private JComponent buildTeamTab() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(PAGE);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel banner = new JPanel(new BorderLayout(18, 18));
        banner.setBackground(new Color(34, 83, 154));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(19, 57, 116), 1),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));

        JPanel bannerLeft = new JPanel();
        bannerLeft.setOpaque(false);
        bannerLeft.setLayout(new BoxLayout(bannerLeft, BoxLayout.Y_AXIS));

        JLabel teamTitle = new JLabel("Windows XP");
        teamTitle.setForeground(Color.WHITE);
        teamTitle.setFont(new Font("Georgia", Font.BOLD, 32));

        JLabel teamSubtitle = new JLabel("Operating Systems Team - German University in Cairo");
        teamSubtitle.setForeground(new Color(229, 239, 255));
        teamSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        JLabel teamLine = new JLabel("Built with backend correctness, presentation polish, and teamwork.");
        teamLine.setForeground(new Color(245, 214, 122));
        teamLine.setFont(new Font("Segoe UI", Font.BOLD, 14));

        bannerLeft.add(teamTitle);
        bannerLeft.add(Box.createVerticalStrut(8));
        bannerLeft.add(teamSubtitle);
        bannerLeft.add(Box.createVerticalStrut(8));
        bannerLeft.add(teamLine);

        JPanel orbit = new JPanel(new GridLayout(1, 3, 8, 8));
        orbit.setOpaque(false);
        orbit.add(headerBadge("University", "GUC", GERMANY_RED));
        orbit.add(headerBadge("Theme", "XP Spirit", GERMANY_GOLD));
        orbit.add(headerBadge("Course", "OS Project", new Color(42, 127, 121)));

        banner.add(bannerLeft, BorderLayout.CENTER);
        banner.add(orbit, BorderLayout.EAST);

        JPanel membersGrid = new JPanel(new GridLayout(2, 3, 14, 14));
        membersGrid.setOpaque(false);
        membersGrid.add(memberCard("01", "Hicham Walid Hicham Haggag", "hicham.haggag@student.guc.edu.eg", "61-34169", "T-8"));
        membersGrid.add(memberCard("02", "Ingie Mazhar Farouk Ali Khalaf", "ingie.khalaf@student.guc.edu.eg", "61-5096", "T-25"));
        membersGrid.add(memberCard("03", "Basmala Allam Anwar", "basmala.aly@student.guc.edu.eg", "61-9927", "T-8"));
        membersGrid.add(memberCard("04", "Kyrillos Ramy Mousa Girgis", "Kyrillos.girgis@student.guc.edu.eg", "61-12947", "T-8"));
        membersGrid.add(memberCard("05", "Farid Fady", "Farid.gerges@student.guc.edu.eg", "61-6014", "T-25"));

        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        content.add(banner, BorderLayout.NORTH);
        content.add(membersGrid, BorderLayout.CENTER);

        return wrapScroll(content);
    }

    private JPanel buildSchedulerPanel() {
        JPanel panel = createSectionPanel("Scheduler Setup", "Choose the policy the simulator should follow during the run.");

        JPanel content = new JPanel(new GridLayout(2, 2, 12, 12));
        content.setOpaque(false);

        algorithmBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        quantumField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        algorithmBox.addActionListener(event -> refreshQuantumFieldState());

        content.add(fieldBlock("Algorithm", algorithmBox));
        content.add(fieldBlock("RR Quantum", styleField(quantumField)));
        content.add(infoCard("Arrivals", "P1 at t=0, P2 at t=1, P3 at t=4"));
        content.add(infoCard("Memory Model", "40 words total, 3 variable slots per process"));

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInputsPanel() {
        JPanel panel = createSectionPanel("Program Inputs", "These inputs feed the three provided OS programs in order.");

        JPanel grid = new JPanel(new GridLayout(5, 2, 10, 10));
        grid.setOpaque(false);

        grid.add(fieldLabel("Program 1 Start"));
        grid.add(styleField(p1StartField));
        grid.add(fieldLabel("Program 1 End"));
        grid.add(styleField(p1EndField));
        grid.add(fieldLabel("Program 2 File Name"));
        grid.add(styleField(p2FileField));
        grid.add(fieldLabel("Program 2 File Data"));
        grid.add(styleField(p2DataField));
        grid.add(fieldLabel("Program 3 File Name"));
        grid.add(styleField(p3FileField));

        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionsPanel() {
        JPanel panel = createSectionPanel("Run Controls", "Preset buttons help you set up a correct evaluation demo quickly.");

        stylePrimaryButton(runButton, GERMANY_RED);
        styleAccentButton(hrrnDemoButton, GERMANY_GOLD);
        styleSecondaryButton(rrDemoButton);
        styleSecondaryButton(mlfqDemoButton);
        styleSecondaryButton(clearButton);

        runButton.addActionListener(event -> runSimulation());
        hrrnDemoButton.addActionListener(event -> loadHrrnPreset());
        rrDemoButton.addActionListener(event -> loadRrPreset());
        mlfqDemoButton.addActionListener(event -> loadMlfqPreset());
        clearButton.addActionListener(event -> {
            stopPlayback();
            outputArea.setText("");
            clearFrameViews();
            updateSummary("Idle", "0", "0", "0");
        });

        JPanel topButtons = new JPanel(new GridLayout(1, 2, 10, 10));
        topButtons.setOpaque(false);
        topButtons.add(runButton);
        topButtons.add(hrrnDemoButton);

        JPanel middleButtons = new JPanel(new GridLayout(1, 2, 10, 10));
        middleButtons.setOpaque(false);
        middleButtons.add(rrDemoButton);
        middleButtons.add(mlfqDemoButton);

        JPanel bottomButtons = new JPanel(new GridLayout(1, 1, 10, 10));
        bottomButtons.setOpaque(false);
        bottomButtons.add(clearButton);

        JPanel actionsWrapper = new JPanel();
        actionsWrapper.setOpaque(false);
        actionsWrapper.setLayout(new BoxLayout(actionsWrapper, BoxLayout.Y_AXIS));
        actionsWrapper.add(topButtons);
        actionsWrapper.add(Box.createVerticalStrut(10));
        actionsWrapper.add(middleButtons);
        actionsWrapper.add(Box.createVerticalStrut(10));
        actionsWrapper.add(bottomButtons);

        panel.add(actionsWrapper, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBrandStoryPanel() {
        JPanel panel = createSectionPanel("Project Identity", "University and team branding for a more presentation-ready product.");

        JTextArea story = new JTextArea();
        story.setEditable(false);
        story.setLineWrap(true);
        story.setWrapStyleWord(true);
        story.setOpaque(false);
        story.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        story.setForeground(MUTED);
        story.setText(
                "This interface uses Germany-inspired colors to reflect GUC: black, red, and gold. " +
                "The dashboard is designed to help Team Windows XP demo both system correctness and user-facing polish."
        );

        panel.add(story, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildMetricsPanel() {
        JPanel metrics = new JPanel(new GridLayout(1, 5, 12, 12));
        metrics.setOpaque(false);
        metrics.add(metricCard("Status", statusValue));
        metrics.add(metricCard("Scheduler", schedulerValue));
        metrics.add(metricCard("Outputs", outputCountValue));
        metrics.add(metricCard("Swaps", swapCountValue));
        metrics.add(metricCard("Finished", finishCountValue));
        return metrics;
    }

    private JPanel buildOutputPanel() {
        JPanel panel = createSectionPanel("Simulation Trace", "This is the full execution log: instructions, memory, queues, mutexes, and swapping.");
        panel.add(wrapScroll(outputArea), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildVisualizationTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.addTab("System View", buildSystemViewPanel());
        tabs.addTab("Simulation Trace", buildOutputPanel());
        return tabs;
    }

    private JComponent buildSystemViewPanel() {
        JPanel panel = createSectionPanel("Clock-By-Clock View", "Visual display of queues, running process, memory, swapping, and state changes.");

        styleSecondaryButton(previousStepButton);
        styleAccentButton(playPauseButton, GERMANY_GOLD);
        styleSecondaryButton(nextStepButton);
        styleSecondaryButton(resetViewButton);

        playPauseButton.addActionListener(event -> togglePlayback());
        nextStepButton.addActionListener(event -> stepForward());
        previousStepButton.addActionListener(event -> stepBackward());
        resetViewButton.addActionListener(event -> resetFrameView());

        JPanel topSummary = new JPanel(new GridLayout(1, 2, 12, 12));
        topSummary.setOpaque(false);
        topSummary.add(metricCard("Current Clock", currentClockValue));
        topSummary.add(metricCard("Running Process", runningProcessValue));

        JPanel queueGrid = new JPanel(new GridLayout(1, 3, 10, 10));
        queueGrid.setOpaque(false);
        queueGrid.add(infoPanel("Ready Queues", readyQueueArea));
        queueGrid.add(infoPanel("Blocked / Resource Queues", blockedQueueArea));
        queueGrid.add(infoPanel("Swap / Disk Activity", swapArea));

        JPanel processGrid = new JPanel(new GridLayout(1, 3, 10, 10));
        processGrid.setOpaque(false);
        processGrid.add(processStateCard("P1"));
        processGrid.add(processStateCard("P2"));
        processGrid.add(processStateCard("P3"));

        JPanel controls = new JPanel(new GridLayout(1, 4, 10, 10));
        controls.setOpaque(false);
        controls.add(previousStepButton);
        controls.add(playPauseButton);
        controls.add(nextStepButton);
        controls.add(resetViewButton);

        JSplitPane lowerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                infoPanel("Memory Contents", memoryFrameArea),
                infoPanel("Current Clock Trace", frameTraceArea)
        );
        lowerSplit.setResizeWeight(0.55);
        lowerSplit.setDividerSize(10);
        lowerSplit.setBorder(null);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(topSummary);
        content.add(Box.createVerticalStrut(10));
        content.add(queueGrid);
        content.add(Box.createVerticalStrut(10));
        content.add(processGrid);
        content.add(Box.createVerticalStrut(10));
        content.add(controls);
        content.add(Box.createVerticalStrut(10));
        content.add(lowerSplit);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane wrapScroll(JComponent component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        scrollPane.setBorder(BorderFactory.createLineBorder(LINE, 1));
        scrollPane.getViewport().setBackground(PANEL);
        return scrollPane;
    }

    private void configureReadOnlyArea(JTextArea area, int fontSize, boolean lineWrap) {
        area.setEditable(false);
        area.setLineWrap(lineWrap);
        area.setWrapStyleWord(lineWrap);
        area.setFont(new Font("Consolas", Font.PLAIN, fontSize));
        area.setBackground(new Color(250, 248, 243));
        area.setForeground(TEXT);
        area.setMargin(new Insets(12, 12, 12, 12));
    }

    private JPanel infoPanel(String title, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT);

        panel.add(label, BorderLayout.NORTH);
        panel.add(wrapScroll(area), BorderLayout.CENTER);
        return panel;
    }

    private JPanel processStateCard(String processName) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(PANEL_ALT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 218, 206), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel title = new JLabel(processName);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TEXT);

        JLabel state = new JLabel("NOT ARRIVED");
        state.setOpaque(true);
        state.setHorizontalAlignment(JLabel.CENTER);
        state.setFont(new Font("Segoe UI", Font.BOLD, 13));
        state.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        applyStateStyle(state, "NOT ARRIVED");
        processStateLabels.put(processName, state);

        card.add(title, BorderLayout.NORTH);
        card.add(state, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSectionPanel(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(MUTED);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);
        header.add(Box.createVerticalStrut(8));
        header.add(new JSeparator());

        panel.add(header, BorderLayout.NORTH);
        return panel;
    }

    private JPanel fieldBlock(String title, JComponent field) {
        JPanel block = new JPanel(new BorderLayout(6, 6));
        block.setOpaque(false);
        block.add(fieldLabel(title), BorderLayout.NORTH);
        block.add(field, BorderLayout.CENTER);
        return block;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT);
        return label;
    }

    private JPanel infoCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(PANEL_ALT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 218, 206), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel top = new JLabel(title);
        top.setFont(new Font("Segoe UI", Font.BOLD, 12));
        top.setForeground(GERMANY_RED);

        JLabel bottom = new JLabel("<html><body style='width:180px'>" + value + "</body></html>");
        bottom.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottom.setForeground(TEXT);

        card.add(top, BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    private JPanel metricCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        valueLabel.setForeground(TEXT);
        valueLabel.setFont(new Font("Georgia", Font.BOLD, 22));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JTextField styleField(JTextField field) {
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(194, 188, 177), 1),
                BorderFactory.createEmptyBorder(7, 8, 7, 8)
        ));
        return field;
    }

    private void stylePrimaryButton(JButton button, Color background) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(11, 15, 11, 15));
    }

    private void styleAccentButton(JButton button, Color background) {
        button.setBackground(background);
        button.setForeground(GERMANY_BLACK);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(11, 15, 11, 15));
    }

    private void styleSecondaryButton(JButton button) {
        button.setBackground(new Color(239, 234, 226));
        button.setForeground(TEXT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(198, 191, 181), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
    }

    private void loadHrrnPreset() {
        algorithmBox.setSelectedItem("HRRN");
        quantumField.setText("2");
        p1StartField.setText("10");
        p1EndField.setText("15");
        p2FileField.setText("myfile.txt");
        p2DataField.setText("hello");
        p3FileField.setText("myfile.txt");
    }

    private void loadRrPreset() {
        algorithmBox.setSelectedItem("RR");
        quantumField.setText("2");
        p1StartField.setText("2");
        p1EndField.setText("9");
        p2FileField.setText("rr-demo.txt");
        p2DataField.setText("round-robin");
        p3FileField.setText("rr-demo.txt");
    }

    private void loadMlfqPreset() {
        algorithmBox.setSelectedItem("MLFQ");
        quantumField.setText("2");
        p1StartField.setText("10");
        p1EndField.setText("15");
        p2FileField.setText("mlfq-demo.txt");
        p2DataField.setText("hello-from-mlfq");
        p3FileField.setText("mlfq-demo.txt");
    }

    private void runSimulation() {
        String algorithm = (String) algorithmBox.getSelectedItem();
        String quantum = quantumField.getText().trim();

        if ("RR".equals(algorithm)) {
            try {
                int parsed = Integer.parseInt(quantum);
                if (parsed <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                JOptionPane.showMessageDialog(this, "RR quantum must be a positive integer.");
                return;
            }
        }

        runButton.setEnabled(false);
        hrrnDemoButton.setEnabled(false);
        rrDemoButton.setEnabled(false);
        mlfqDemoButton.setEnabled(false);
        statusValue.setText("Running");
        schedulerValue.setText(algorithm);
        outputArea.setText("Running simulation...\n");

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                List<String> inputs = new ArrayList<String>();
                inputs.add(p1StartField.getText().trim());
                inputs.add(p1EndField.getText().trim());
                inputs.add(p2FileField.getText().trim());
                inputs.add(p2DataField.getText().trim());
                inputs.add(p3FileField.getText().trim());

                List<String> argsList = new ArrayList<String>();
                if ("RR".equals(algorithm)) {
                    argsList.add("RR");
                    argsList.add("q=" + quantum);
                } else if ("MLFQ".equals(algorithm)) {
                    argsList.add("MLFQ");
                } else {
                    argsList.add("HRRN");
                }

                PrintStream originalOut = System.out;
                PrintStream originalErr = System.err;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                PrintStream capture = new PrintStream(buffer);

                try {
                    System.setOut(capture);
                    System.setErr(capture);
                    Main.runWithScriptedInputs(argsList.toArray(new String[0]), inputs);
                } finally {
                    capture.flush();
                    System.setOut(originalOut);
                    System.setErr(originalErr);
                }

                return buffer.toString();
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    outputArea.setText(result);
                    outputArea.setCaretPosition(0);
                    loadFramesFromOutput(result);
                    updateMetrics(result, algorithm);
                } catch (Exception exception) {
                    outputArea.setText("Failed to run simulation.\n" + exception.getMessage());
                    clearFrameViews();
                    updateSummary("Failed", "0", "0", "0");
                } finally {
                    runButton.setEnabled(true);
                    hrrnDemoButton.setEnabled(true);
                    rrDemoButton.setEnabled(true);
                    mlfqDemoButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void updateMetrics(String output, String algorithm) {
        schedulerValue.setText(algorithm);
        updateSummary(
                "Completed",
                String.valueOf(count(output, "[OUTPUT]")),
                String.valueOf(count(output, "[SWAP]")),
                String.valueOf(count(output, "[SYSTEM] P"))
        );
    }

    private void updateSummary(String status, String outputs, String swaps, String finished) {
        statusValue.setText(status);
        outputCountValue.setText(outputs);
        swapCountValue.setText(swaps);
        finishCountValue.setText(finished);
    }

    private int count(String text, String token) {
        int count = 0;
        int index = 0;
        while (text != null && token != null && !token.isEmpty()) {
            index = text.indexOf(token, index);
            if (index == -1) {
                break;
            }
            count++;
            index += token.length();
        }
        return count;
    }

    private void loadFramesFromOutput(String output) {
        stopPlayback();
        frames.clear();

        if (output == null || output.trim().isEmpty()) {
            clearFrameViews();
            return;
        }

        String[] lines = output.split("\\R");
        List<String> currentLines = null;
        int currentClock = -1;
        Set<String> finishedProcesses = new LinkedHashSet<String>();

        for (String line : lines) {
            if (line.startsWith(">>> CLOCK:")) {
                if (currentLines != null) {
                    frames.add(parseFrame(currentClock, currentLines, finishedProcesses));
                }
                currentClock = extractClock(line);
                currentLines = new ArrayList<String>();
            }

            if (currentLines != null) {
                currentLines.add(line);
            }
        }

        if (currentLines != null) {
            frames.add(parseFrame(currentClock, currentLines, finishedProcesses));
        }

        if (frames.isEmpty()) {
            clearFrameViews();
            return;
        }

        currentFrameIndex = 0;
        showFrame(0);
    }

    private ClockFrame parseFrame(int clock, List<String> lines, Set<String> finishedProcesses) {
        ClockFrame frame = new ClockFrame();
        frame.clock = clock;
        frame.fullTrace = joinLines(lines);

        List<String> queueLines = new ArrayList<String>();
        List<String> blockedLines = new ArrayList<String>();
        List<String> swapLines = new ArrayList<String>();
        List<String> memoryLines = new ArrayList<String>();
        boolean inMemorySection = false;

        for (String line : lines) {
            if (line.startsWith("[SYSTEM] P")) {
                String processName = extractProcessName(line);
                if (processName != null) {
                    finishedProcesses.add(processName);
                }
            }

            if (line.startsWith("READY Queue:") || line.startsWith("RQ")) {
                queueLines.add(line);
            }
            if (line.startsWith("BLOCKED Queue:") || line.startsWith("FILE Blocked:")
                    || line.startsWith("USER INPUT Blocked:") || line.startsWith("USER OUTPUT Blocked:")) {
                blockedLines.add(line);
            }
            if (line.startsWith("RUNNING:")) {
                frame.runningProcess = line.substring("RUNNING:".length()).trim();
            }
            if (line.startsWith("[SWAP]") || line.startsWith("[DISK]")) {
                swapLines.add(line);
            }
            if (line.startsWith("------ MEMORY ------")) {
                inMemorySection = true;
                continue;
            }
            if (inMemorySection) {
                if (line.trim().isEmpty()) {
                    inMemorySection = false;
                } else {
                    memoryLines.add(line);
                }
            }
        }

        frame.readyQueues = queueLines.isEmpty() ? "No queue snapshot yet." : joinLines(queueLines);
        frame.blockedQueues = blockedLines.isEmpty() ? "No blocked processes." : joinLines(blockedLines);
        frame.swapActivity = swapLines.isEmpty() ? "No swap or disk activity in this clock." : joinLines(swapLines);
        frame.memorySnapshot = memoryLines.isEmpty() ? "No memory snapshot captured yet." : joinLines(memoryLines);
        frame.processStates = deriveProcessStates(frame, finishedProcesses);
        return frame;
    }

    private Map<String, String> deriveProcessStates(ClockFrame frame, Set<String> finishedProcesses) {
        Map<String, String> states = new LinkedHashMap<String, String>();
        List<String> readyProcesses = extractProcesses(frame.readyQueues);
        List<String> blockedProcesses = extractProcesses(frame.blockedQueues);

        for (String processName : Arrays.asList("P1", "P2", "P3")) {
            String state = "NOT ARRIVED";
            if (finishedProcesses.contains(processName)) {
                state = "FINISHED";
            } else if (processName.equals(frame.runningProcess)) {
                state = "RUNNING";
            } else if (blockedProcesses.contains(processName)) {
                state = "BLOCKED";
            } else if (readyProcesses.contains(processName)) {
                state = "READY";
            }
            states.put(processName, state);
        }

        return states;
    }

    private List<String> extractProcesses(String text) {
        List<String> processes = new ArrayList<String>();
        if (text == null) {
            return processes;
        }

        for (String token : text.split("[^A-Za-z0-9]+")) {
            if (token.matches("P[123]") && !processes.contains(token)) {
                processes.add(token);
            }
        }
        return processes;
    }

    private int extractClock(String line) {
        String digits = line.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(digits);
    }

    private String extractProcessName(String line) {
        for (String token : line.split("[^A-Za-z0-9]+")) {
            if (token.matches("P[123]")) {
                return token;
            }
        }
        return null;
    }

    private String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private void showFrame(int index) {
        if (index < 0 || index >= frames.size()) {
            return;
        }

        currentFrameIndex = index;
        ClockFrame frame = frames.get(index);
        currentClockValue.setText(frame.clock >= 0 ? String.valueOf(frame.clock) : "N/A");
        runningProcessValue.setText(frame.runningProcess == null || frame.runningProcess.isEmpty() ? "None" : frame.runningProcess);
        readyQueueArea.setText(frame.readyQueues);
        blockedQueueArea.setText(frame.blockedQueues);
        swapArea.setText(frame.swapActivity);
        memoryFrameArea.setText(frame.memorySnapshot);
        frameTraceArea.setText(frame.fullTrace);
        frameTraceArea.setCaretPosition(0);

        for (Map.Entry<String, JLabel> entry : processStateLabels.entrySet()) {
            String state = frame.processStates.containsKey(entry.getKey()) ? frame.processStates.get(entry.getKey()) : "NOT ARRIVED";
            entry.getValue().setText(state);
            applyStateStyle(entry.getValue(), state);
        }

        previousStepButton.setEnabled(index > 0);
        nextStepButton.setEnabled(index < frames.size() - 1);
        resetViewButton.setEnabled(!frames.isEmpty());
    }

    private void clearFrameViews() {
        frames.clear();
        currentFrameIndex = -1;
        currentClockValue.setText("Not started");
        runningProcessValue.setText("None");
        readyQueueArea.setText("Run a simulation to see the ready queues.");
        blockedQueueArea.setText("Run a simulation to see blocked queues and resource ownership.");
        swapArea.setText("Swap and disk activity will appear here.");
        memoryFrameArea.setText("Memory contents for the selected clock will appear here.");
        frameTraceArea.setText("Use Run Simulation, then play or step through clocks.");
        for (Map.Entry<String, JLabel> entry : processStateLabels.entrySet()) {
            entry.getValue().setText("NOT ARRIVED");
            applyStateStyle(entry.getValue(), "NOT ARRIVED");
        }
        previousStepButton.setEnabled(false);
        nextStepButton.setEnabled(false);
        resetViewButton.setEnabled(false);
        playPauseButton.setText("Play");
    }

    private void applyStateStyle(JLabel label, String state) {
        if ("RUNNING".equals(state)) {
            label.setBackground(new Color(204, 241, 210));
            label.setForeground(new Color(23, 92, 40));
        } else if ("READY".equals(state)) {
            label.setBackground(new Color(247, 235, 189));
            label.setForeground(new Color(107, 76, 10));
        } else if ("BLOCKED".equals(state)) {
            label.setBackground(new Color(245, 208, 208));
            label.setForeground(new Color(131, 33, 33));
        } else if ("FINISHED".equals(state)) {
            label.setBackground(new Color(210, 223, 245));
            label.setForeground(new Color(30, 63, 117));
        } else {
            label.setBackground(new Color(232, 228, 220));
            label.setForeground(MUTED);
        }
    }

    private void togglePlayback() {
        if (frames.isEmpty()) {
            return;
        }
        if (playbackTimer.isRunning()) {
            stopPlayback();
        } else {
            playbackTimer.start();
            playPauseButton.setText("Pause");
        }
    }

    private void stopPlayback() {
        if (playbackTimer != null && playbackTimer.isRunning()) {
            playbackTimer.stop();
        }
        playPauseButton.setText("Play");
    }

    private void advanceFrame() {
        if (currentFrameIndex < frames.size() - 1) {
            showFrame(currentFrameIndex + 1);
        } else {
            stopPlayback();
        }
    }

    private void stepForward() {
        stopPlayback();
        if (currentFrameIndex < frames.size() - 1) {
            showFrame(currentFrameIndex + 1);
        }
    }

    private void stepBackward() {
        stopPlayback();
        if (currentFrameIndex > 0) {
            showFrame(currentFrameIndex - 1);
        }
    }

    private void resetFrameView() {
        stopPlayback();
        if (!frames.isEmpty()) {
            showFrame(0);
        }
    }

    private String buildGuideText() {
        return
                "GUC OPERATING SYSTEMS PROJECT - EVALUATION GUIDE\n\n" +
                "Team Name: Windows XP\n" +
                "University: German University in Cairo\n\n" +
                "What This Simulator Shows\n" +
                "1. Program arrival and process creation.\n" +
                "2. Memory allocation inside a fixed 40-word memory.\n" +
                "3. Process Control Block storage and updates.\n" +
                "4. Mutual exclusion over userInput, userOutput, and file resources.\n" +
                "5. Scheduling using HRRN, Round Robin, or MLFQ.\n" +
                "6. Swapping processes to disk when memory is full.\n" +
                "7. Human-readable tracing of queues, running process, instruction, and memory state.\n" +
                "8. Visual display of ready queues, blocked queues, running process, process states, memory, and swap activity.\n" +
                "9. Clock-by-clock playback with play, pause, previous-step, next-step, and reset controls.\n\n" +
                "Recommended Demo Flow\n" +
                "- First click 'Load HRRN Demo' for the cleanest baseline walkthrough.\n" +
                "- Keep Scheduler on HRRN for the cleanest first explanation.\n" +
                "- Click 'Run Simulation'.\n" +
                "- Show how P1 arrives first, then P2, then P3.\n" +
                "- Point out that P2 is swapped out when memory becomes full.\n" +
                "- Explain that Program 2 writes to myfile.txt and Program 3 reads the same file.\n" +
                "- Show that Program 3 finally prints hello.\n\n" +
                "How To Use The Buttons\n" +
                "- Run Simulation: executes the full OS simulation using the current settings and current input fields.\n" +
                "- Load HRRN Demo: fills the fields with a clean end-to-end scenario using HRRN.\n" +
                "- Load RR Demo: switches to Round Robin with quantum 2 and fills values that make context switching easy to see.\n" +
                "- Load MLFQ Demo: switches to MLFQ and fills a ready-made scenario that shows queue demotion, preemption, swapping, and final file output.\n" +
                "- Play: automatically advances through recorded clock frames after a run.\n" +
                "- Pause: stops automatic playback on the current clock frame.\n" +
                "- Previous Step / Next Step: move one clock at a time through execution.\n" +
                "- Reset View: returns the visual clock view to the first captured frame.\n" +
                "- Clear Output: clears the log window and resets the summary cards visually.\n" +
                "- Team Windows XP tab: shows the team members in a more presentation-friendly style.\n\n" +
                "Scheduler Inputs\n" +
                "- Algorithm: choose HRRN, RR, or MLFQ.\n" +
                "- RR Quantum: used only when Round Robin is selected.\n" +
                "- MLFQ: starts every process in RQ0, uses 4 queues, uses quantums 1, 2, 4, and 8, and demotes a process only when it fully consumes its time slice.\n\n" +
                "Program Inputs\n" +
                "- Program 1 Start / End: values used by printFromTo.\n" +
                "- Program 2 File Name / File Data: Program 2 writes this data to this file.\n" +
                "- Program 3 File Name: Program 3 reads the file and prints its contents.\n";
    }

    private void refreshQuantumFieldState() {
        String algorithm = (String) algorithmBox.getSelectedItem();
        boolean rrSelected = "RR".equals(algorithm);
        quantumField.setEnabled(rrSelected);
        quantumField.setBackground(rrSelected ? Color.WHITE : new Color(237, 233, 226));
        quantumField.setToolTipText(rrSelected
                ? "Round Robin quantum in instructions."
                : "MLFQ uses fixed quantums 1, 2, 4, 8. HRRN does not use a quantum.");
    }

    private JPanel memberCard(String number, String name, String email, String id, String tutorial) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel badge = new JLabel(number);
        badge.setOpaque(true);
        badge.setBackground(new Color(233, 241, 255));
        badge.setForeground(new Color(28, 74, 137));
        badge.setFont(new Font("Segoe UI", Font.BOLD, 16));
        badge.setHorizontalAlignment(JLabel.CENTER);
        badge.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT);
        nameLabel.setFont(new Font("Georgia", Font.BOLD, 18));

        JEditorPane detailsPane = new JEditorPane();
        detailsPane.setContentType("text/html");
        detailsPane.setEditable(false);
        detailsPane.setOpaque(false);
        detailsPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        detailsPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailsPane.setMargin(new Insets(0, 0, 0, 0));
        detailsPane.setText(
                "<html><body style='font-family:Segoe UI; font-size:12px; color:#60666E; width:230px'>" +
                        "<b>Email:</b> <a href='mailto:" + email + "'>" + email + "</a><br/>" +
                        "<b>ID:</b> " + id + "<br/>" +
                        "<b>Tutorial:</b> " + tutorial +
                        "</body></html>"
        );
        detailsPane.addHyperlinkListener(event -> {
            if (event.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                openLink(event.getURL() != null ? event.getURL().toString() : "mailto:" + email);
            }
        });

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);
        top.add(badge, BorderLayout.WEST);
        top.add(nameLabel, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(detailsPane, BorderLayout.CENTER);
        return card;
    }

    private void openLink(String uriText) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(uriText));
            }
        } catch (Exception ignored) {
        }
    }

    private static class ClockFrame {
        int clock;
        String readyQueues;
        String blockedQueues;
        String runningProcess;
        String swapActivity;
        String memorySnapshot;
        String fullTrace;
        Map<String, String> processStates;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SimulatorGUI().setVisible(true);
            }
        });
    }
}
