import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * NotifyServerGui - Ứng dụng Java hoàn chỉnh gồm:
 * 1. Server GUI với Swing để tạo và broadcast thông báo
 * 2. HTTP Server tích hợp với SSE Gateway
 * 3. Web Client với giao diện Facebook-like newsfeed
 * 
 * Made by: NguyenVanChung
 * Student ID: 28219105598
 * 
 * Cách chạy:
 * javac NotifyServerGui.java
 * java NotifyServerGui
 * Sau đó mở http://localhost:8080
 */
public class NotifyServerGui {
    
    // ===== CONSTANTS =====
    private static final int PORT = 8080;
    private static final String SERVER_NAME = "NotifyServer/1.0";
    
    // ===== GUI COMPONENTS =====
    private JFrame frame;
    private JTextField titleField;
    private JTextField messageField;
    private JTextArea bodyArea;
    private JButton broadcastButton;
    private JTextArea logArea;
    
    // ===== SERVER COMPONENTS =====
    private HttpServer httpServer;
    private final Set<HttpExchange> sseClients = ConcurrentHashMap.newKeySet();
    private long messageIdCounter = 1;
    
    // ===== EXTRA FIELD COMPONENTS =====
    private JComboBox<String> priorityCombo;
    private JTextField iconField;
    private JComboBox<String> categoryCombo;
    private JTextField customExtraField;
    
    /**
     * Constructor - Khởi tạo GUI và HTTP Server
     */
    public NotifyServerGui() {
        initializeGUI();
        startHttpServer();
    }
    
    // ===== GUI INITIALIZATION =====
    
    /**
     * Khởi tạo giao diện Swing
     */
    private void initializeGUI() {
        frame = new JFrame("Máy Chủ Thông Báo - Gửi Tin Nhắn Đến Web Clients");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(700, 600);
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Input panel
        JPanel inputPanel = createInputPanel();
        
        // Button panel
        JPanel buttonPanel = createButtonPanel();
        
        // Log panel
        JPanel logPanel = createLogPanel();
        
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(logPanel, BorderLayout.SOUTH);
        
        // Credits panel
        JPanel creditsPanel = createCreditsPanel();
        
        frame.add(mainPanel);
        frame.add(creditsPanel, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        log("Giao diện đã khởi tạo thành công");
    }
    
    /**
     * Tạo panel credits
     */
    private JPanel createCreditsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JLabel creditsLabel = new JLabel("Made by NguyenVanChung - Student ID: 28219105598");
        creditsLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 10));
        creditsLabel.setForeground(Color.GRAY);
        
        panel.add(creditsLabel);
        return panel;
    }
    
    /**
     * Tạo panel nhập liệu
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Chi Tiết Thông Báo"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title field
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Tiêu đề:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        titleField = new JTextField(20);
        titleField.setText("Thông báo mới");
        panel.add(titleField, gbc);
        
        // Message field
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Tin nhắn:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        messageField = new JTextField(20);
        messageField.setText("Đây là tin nhắn thử nghiệm");
        panel.add(messageField, gbc);
        
        // Body area
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Nội dung:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        bodyArea = new JTextArea(4, 20);
        bodyArea.setText("Nội dung chi tiết của thông báo sẽ được hiển thị ở đây...");
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        JScrollPane bodyScroll = new JScrollPane(bodyArea);
        panel.add(bodyScroll, gbc);
        
        // Extra fields - User friendly
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0;
        panel.add(new JLabel("Thuộc tính mở rộng:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        
        JPanel extraPanel = createExtraPanel();
        panel.add(extraPanel, gbc);
        
        return panel;
    }
    
    /**
     * Tạo panel thuộc tính mở rộng thân thiện với người dùng
     */
    private JPanel createExtraPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Tùy Chọn Thêm"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Priority
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Độ ưu tiên:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        priorityCombo = new JComboBox<>(new String[]{"thấp", "trung bình", "cao", "khẩn cấp"});
        priorityCombo.setSelectedItem("cao");
        panel.add(priorityCombo, gbc);
        
        // Icon
        gbc.gridx = 2; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Icon:"), gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        iconField = new JTextField("🎉", 5);
        iconField.setToolTipText("Nhập emoji hoặc icon (ví dụ: 🎉, 📢, ⚠️, 🚀)");
        panel.add(iconField, gbc);
        
        // Category
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Danh mục:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        categoryCombo = new JComboBox<>(new String[]{"tin tức", "thông báo", "cảnh báo", "sự kiện", "hệ thống", "khác"});
        categoryCombo.setSelectedItem("tin tức");
        panel.add(categoryCombo, gbc);
        
        // Custom extra
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(new JLabel("Tùy chỉnh thêm:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        customExtraField = new JTextField(15);
        customExtraField.setToolTipText("Định dạng: key=value;key2=value2 (ví dụ: author=admin;location=hanoi)");
        customExtraField.setText("author=NguyenVanChung;location=vietnam");
        panel.add(customExtraField, gbc);
        
        return panel;
    }
    
    /**
     * Tạo panel nút bấm
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        
        broadcastButton = new JButton("Đăng Thông Báo!");
        broadcastButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        broadcastButton.setPreferredSize(new Dimension(300, 40));
        broadcastButton.setBackground(new Color(24, 119, 242));
        broadcastButton.setForeground(Color.BLACK);
        broadcastButton.addActionListener(new BroadcastActionListener());
        
        panel.add(broadcastButton);
        
        return panel;
    }
    
    /**
     * Tạo panel log
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Nhật Ký Máy Chủ"));
        
        logArea = new JTextArea(6, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(logScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ===== HTTP SERVER =====
    
    /**
     * Khởi động HTTP Server
     */
    private void startHttpServer() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
            httpServer.createContext("/", new IndexHandler());
            httpServer.createContext("/events", new SSEHandler());
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
            
            log("Máy chủ HTTP đã khởi động trên cổng " + PORT);
            log("Mở http://localhost:" + PORT + " để xem web client");
        } catch (IOException e) {
            log("Lỗi khởi động máy chủ HTTP: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handler cho trang chính "/"
     */
    private class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = INDEX_HTML;
                
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.getResponseHeaders().set("Server", SERVER_NAME);
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                
                log("Đã phục vụ trang chính cho " + exchange.getRemoteAddress());
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }
    
    /**
     * Handler cho Server Sent Events "/events"
     */
    private class SSEHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                // Setup SSE headers
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.getResponseHeaders().set("Connection", "keep-alive");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Server", SERVER_NAME);
                
                exchange.sendResponseHeaders(200, 0);
                
                // Add client to SSE clients set
                sseClients.add(exchange);
                log("Client SSE mới kết nối: " + exchange.getRemoteAddress() + " (Tổng: " + sseClients.size() + ")");
                
                // Send initial connection message
                sendSSEMessage(exchange, "connected", "Kết nối thành công", "Bạn đã kết nối với máy chủ thông báo", "", new HashMap<>());
                
                // Keep connection alive
                try {
                    // The connection will be kept alive until client disconnects
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(30000); // Check every 30 seconds
                        
                        // Send keepalive ping
                        try {
                            OutputStream os = exchange.getResponseBody();
                            os.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
                            os.flush();
                        } catch (IOException e) {
                            // Client disconnected
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    sseClients.remove(exchange);
                    log("Client SSE đã ngắt kết nối: " + exchange.getRemoteAddress() + " (Tổng: " + sseClients.size() + ")");
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }
    
    // ===== SSE MESSAGING =====
    
    /**
     * Gửi tin nhắn SSE tới một client cụ thể
     */
    private void sendSSEMessage(HttpExchange exchange, String type, String title, String body, String message, Map<String, String> extra) {
        try {
            // Tạo JSON message
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("type", type);
            jsonData.put("title", title);
            jsonData.put("body", body);
            jsonData.put("message", message);
            jsonData.put("extra", extra);
            jsonData.put("id", messageIdCounter++);
            jsonData.put("ts", Instant.now().toEpochMilli());
            
            String jsonString = mapToJson(jsonData);
            
            // Format SSE message
            String sseMessage = "data: " + jsonString + "\n\n";
            
            OutputStream os = exchange.getResponseBody();
            os.write(sseMessage.getBytes(StandardCharsets.UTF_8));
            os.flush();
            
        } catch (IOException e) {
            // Client disconnected, will be removed in finally block
            sseClients.remove(exchange);
        }
    }
    
    /**
     * Broadcast tin nhắn tới tất cả SSE clients
     */
    private void broadcastSSEMessage(String type, String title, String body, String message, Map<String, String> extra) {
        List<HttpExchange> disconnectedClients = new ArrayList<>();
        
        for (HttpExchange client : sseClients) {
            try {
                sendSSEMessage(client, type, title, body, message, extra);
            } catch (Exception e) {
                disconnectedClients.add(client);
            }
        }
        
        // Remove disconnected clients
        for (HttpExchange client : disconnectedClients) {
            sseClients.remove(client);
        }
        
        log("Đã phát sóng tin nhắn tới " + (sseClients.size()) + " clients: " + title);
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Convert Map to JSON string (simple implementation)
     */
    private String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(",");
            first = false;
            
            json.append("\"").append(escapeJson(entry.getKey())).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                json.append(mapToJson(mapValue));
            } else {
                json.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Parse extra field from GUI components
     */
    private Map<String, String> parseExtraField(String customExtra) {
        Map<String, String> result = new HashMap<>();
        
        // Add priority
        String priority = (String) priorityCombo.getSelectedItem();
        if (priority != null) {
            // Convert Vietnamese to English for consistency
            switch (priority) {
                case "thấp": result.put("priority", "low"); break;
                case "trung bình": result.put("priority", "medium"); break;
                case "cao": result.put("priority", "high"); break;
                case "khẩn cấp": result.put("priority", "urgent"); break;
                default: result.put("priority", priority);
            }
        }
        
        // Add icon
        String icon = iconField.getText().trim();
        if (!icon.isEmpty()) {
            result.put("icon", icon);
        }
        
        // Add category
        String category = (String) categoryCombo.getSelectedItem();
        if (category != null) {
            // Convert Vietnamese to English for consistency
            switch (category) {
                case "tin tức": result.put("category", "news"); break;
                case "thông báo": result.put("category", "notification"); break;
                case "cảnh báo": result.put("category", "warning"); break;
                case "sự kiện": result.put("category", "event"); break;
                case "hệ thống": result.put("category", "system"); break;
                case "khác": result.put("category", "other"); break;
                default: result.put("category", category);
            }
        }
        
        // Add custom extra fields
        if (customExtra != null && !customExtra.trim().isEmpty()) {
            String[] pairs = customExtra.split(";");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    result.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Log message to GUI and console
     */
    private void log(String message) {
        String timestamp = DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        
        String logMessage = "[" + timestamp + "] " + message;
        
        SwingUtilities.invokeLater(() -> {
            logArea.append(logMessage + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
        
        System.out.println(logMessage);
    }
    
    // ===== EVENT HANDLERS =====
    
    /**
     * Action listener cho nút Broadcast
     */
    private class BroadcastActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String title = titleField.getText().trim();
            String message = messageField.getText().trim();
            String body = bodyArea.getText().trim();
            String customExtra = customExtraField.getText().trim();
            
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Tiêu đề không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Map<String, String> extra = parseExtraField(customExtra);
            
            // Broadcast to all SSE clients
            broadcastSSEMessage("notification", title, body, message, extra);
            
            log("Bắt đầu phát sóng - Tiêu đề: " + title + ", Clients: " + sseClients.size());
        }
    }
    
    // ===== WEB CLIENT HTML/CSS/JS =====
    
    /**
     * HTML/CSS/JS cho web client - giao diện Facebook-like
     */
    private static final String INDEX_HTML = """
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bảng Tin Thông Báo</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background-color: #f0f2f5;
            color: #1c1e21;
        }
        
        /* Top Bar */
        .top-bar {
            background: #fff;
            border-bottom: 1px solid #dadde1;
            padding: 12px 16px;
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            z-index: 1000;
            display: flex;
            align-items: center;
            justify-content: space-between;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        
        .logo {
            font-size: 24px;
            font-weight: bold;
            color: #1877f2;
        }
        
        .connection-status {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        
        .status-badge {
            padding: 4px 12px;
            border-radius: 16px;
            font-size: 12px;
            font-weight: 600;
            text-transform: uppercase;
        }
        
        .status-connected {
            background: #e7f3ff;
            color: #1877f2;
        }
        
        .status-reconnecting {
            background: #fff3cd;
            color: #856404;
        }
        
        .status-disconnected {
            background: #f8d7da;
            color: #721c24;
        }
        
        .unread-count {
            background: #e41e3f;
            color: white;
            border-radius: 10px;
            padding: 2px 6px;
            font-size: 11px;
            font-weight: bold;
            min-width: 16px;
            text-align: center;
            display: none;
        }
        
        /* Main Container */
        .container {
            max-width: 680px;
            margin: 0 auto;
            padding: 80px 16px 20px;
        }
        
        .feed-header {
            background: #fff;
            border-radius: 8px;
            padding: 16px;
            margin-bottom: 16px;
            text-align: center;
            box-shadow: 0 1px 2px rgba(0,0,0,0.1);
        }
        
        .feed-header h1 {
            font-size: 20px;
            color: #1c1e21;
            margin-bottom: 4px;
        }
        
        .feed-header p {
            color: #65676b;
            font-size: 14px;
        }
        
        /* Notification Cards */
        .notification-card {
            background: #fff;
            border-radius: 8px;
            margin-bottom: 16px;
            box-shadow: 0 1px 2px rgba(0,0,0,0.1);
            overflow: hidden;
            transition: all 0.3s ease;
            opacity: 0;
            transform: translateY(-10px);
            animation: slideIn 0.3s ease forwards;
        }
        
        @keyframes slideIn {
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        
        .notification-card:hover {
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
        }
        
        .card-header {
            padding: 12px 16px;
            border-bottom: 1px solid #e4e6ea;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        
        .card-title {
            font-size: 16px;
            font-weight: 600;
            color: #1c1e21;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        
        .card-time {
            font-size: 12px;
            color: #65676b;
        }
        
        .card-content {
            padding: 16px;
        }
        
        .card-message {
            font-size: 14px;
            color: #1c1e21;
            margin-bottom: 8px;
            font-weight: 500;
        }
        
        .card-body {
            font-size: 14px;
            color: #65676b;
            line-height: 1.4;
            margin-bottom: 12px;
        }
        
        .card-extras {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }
        
        .extra-chip {
            background: #e7f3ff;
            color: #1877f2;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 500;
        }
        
        .extra-chip.priority-high {
            background: #ffebee;
            color: #c62828;
        }
        
        .extra-chip.priority-medium {
            background: #fff3e0;
            color: #ef6c00;
        }
        
        .extra-chip.category-news {
            background: #e8f5e8;
            color: #2e7d32;
        }
        
        /* Empty State */
        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: #65676b;
        }
        
        .empty-state .icon {
            font-size: 48px;
            margin-bottom: 16px;
        }
        
        .empty-state h3 {
            font-size: 18px;
            margin-bottom: 8px;
            color: #1c1e21;
        }
        
        /* Toast Notifications */
        .toast {
            position: fixed;
            top: 80px;
            right: 20px;
            background: #1c1e21;
            color: white;
            padding: 12px 16px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
            z-index: 2000;
            transform: translateX(100%);
            transition: transform 0.3s ease;
        }
        
        .toast.show {
            transform: translateX(0);
        }
        
        /* Responsive */
        @media (max-width: 768px) {
            .container {
                padding: 70px 8px 20px;
            }
            
            .top-bar {
                padding: 8px 12px;
            }
            
            .logo {
                font-size: 20px;
            }
        }
        
        /* Loading Animation */
        .loading-dots {
            display: inline-block;
        }
        
        .loading-dots::after {
            content: '';
            animation: loading 1.5s infinite;
        }
        
        @keyframes loading {
            0%, 20% { content: '.'; }
            40% { content: '..'; }
            60%, 100% { content: '...'; }
        }
    </style>
</head>
<body>
    <!-- Top Bar -->
    <div class="top-bar">
        <div class="logo">📢 Bảng Tin Thông Báo</div>
        <div class="connection-status">
            <div class="unread-count" id="unreadCount">0</div>
            <div class="status-badge status-disconnected" id="statusBadge">Mất kết nối</div>
        </div>
    </div>
    
    <!-- Main Container -->
    <div class="container">
        <div class="feed-header">
            <h1>🔔 Bảng Tin Thông Báo</h1>
            <p>Thông báo real-time từ máy chủ</p>
            <p style="font-size: 12px; margin-top: 5px; color: #65676b;">Made by NguyenVanChung - Student ID: 28219106698</p>
        </div>
        
        <div id="notificationFeed">
            <div class="empty-state">
                <div class="icon">📭</div>
                <h3>Chưa có thông báo nào</h3>
                <p>Đang chờ thông báo mới từ máy chủ<span class="loading-dots"></span></p>
            </div>
        </div>
    </div>
    
    <script>
        // ===== GLOBAL VARIABLES =====
        let eventSource = null;
        let isConnected = false;
        let reconnectAttempts = 0;
        let maxReconnectAttempts = 5;
        let reconnectDelay = 1000;
        let unreadCount = 0;
        let isPageVisible = true;
        
        // DOM Elements
        const statusBadge = document.getElementById('statusBadge');
        const unreadCountEl = document.getElementById('unreadCount');
        const notificationFeed = document.getElementById('notificationFeed');
        
        // ===== INITIALIZATION =====
        document.addEventListener('DOMContentLoaded', function() {
            connectToSSE();
            setupPageVisibilityHandler();
            setupNotificationPermission();
        });
        
        // ===== SSE CONNECTION =====
        function connectToSSE() {
            if (eventSource) {
                eventSource.close();
            }
            
            updateConnectionStatus('reconnecting');
            
            eventSource = new EventSource('/events');
            
            eventSource.onopen = function(event) {
                console.log('SSE connection opened');
                isConnected = true;
                reconnectAttempts = 0;
                updateConnectionStatus('connected');
            };
            
            eventSource.onmessage = function(event) {
                try {
                    const data = JSON.parse(event.data);
                    console.log('Received notification:', data);
                    handleNotification(data);
                } catch (error) {
                    console.error('Error parsing SSE message:', error);
                }
            };
            
            eventSource.onerror = function(event) {
                console.log('SSE connection error');
                isConnected = false;
                updateConnectionStatus('disconnected');
                
                // Auto-reconnect
                if (reconnectAttempts < maxReconnectAttempts) {
                    setTimeout(() => {
                        reconnectAttempts++;
                        console.log(`Reconnect attempt ${reconnectAttempts}/${maxReconnectAttempts}`);
                        connectToSSE();
                    }, reconnectDelay * Math.pow(2, reconnectAttempts));
                }
            };
        }
        
        // ===== CONNECTION STATUS =====
        function updateConnectionStatus(status) {
            statusBadge.className = 'status-badge';
            
            switch(status) {
                case 'connected':
                    statusBadge.classList.add('status-connected');
                    statusBadge.textContent = 'Đã kết nối';
                    break;
                case 'reconnecting':
                    statusBadge.classList.add('status-reconnecting');
                    statusBadge.textContent = 'Đang kết nối lại';
                    break;
                case 'disconnected':
                    statusBadge.classList.add('status-disconnected');
                    statusBadge.textContent = 'Mất kết nối';
                    break;
            }
        }
        
        // ===== NOTIFICATION HANDLING =====
        function handleNotification(data) {
            if (data.type === 'connected') {
                showToast('Đã kết nối với máy chủ thông báo! 🎉');
                return;
            }
            
            if (data.type === 'notification') {
                addNotificationToFeed(data);
                
                // Update unread count if page not visible
                if (!isPageVisible) {
                    unreadCount++;
                    updateUnreadCount();
                }
                
                // Show browser notification
                showBrowserNotification(data);
                
                // Play notification sound
                playNotificationSound();
                
                // Show toast
                showToast(`Thông báo mới: ${data.title}`);
            }
        }
        
        function addNotificationToFeed(data) {
            // Remove empty state if exists
            const emptyState = notificationFeed.querySelector('.empty-state');
            if (emptyState) {
                emptyState.remove();
            }
            
            // Create notification card
            const card = createNotificationCard(data);
            
            // Add to top of feed
            notificationFeed.insertBefore(card, notificationFeed.firstChild);
            
            // Limit to 50 notifications
            const cards = notificationFeed.querySelectorAll('.notification-card');
            if (cards.length > 50) {
                cards[cards.length - 1].remove();
            }
        }
        
        function createNotificationCard(data) {
            const card = document.createElement('div');
            card.className = 'notification-card';
            
            const timestamp = new Date(data.ts).toLocaleString();
            const icon = data.extra && data.extra.icon ? data.extra.icon : '📢';
            
            card.innerHTML = `
                <div class="card-header">
                    <div class="card-title">
                        <span>${icon}</span>
                        ${escapeHtml(data.title)}
                    </div>
                    <div class="card-time">${timestamp}</div>
                </div>
                <div class="card-content">
                    ${data.message ? `<div class="card-message">${escapeHtml(data.message)}</div>` : ''}
                    ${data.body ? `<div class="card-body">${escapeHtml(data.body)}</div>` : ''}
                    ${createExtraChips(data.extra)}
                </div>
            `;
            
            return card;
        }
        
        function createExtraChips(extra) {
            if (!extra || Object.keys(extra).length === 0) {
                return '';
            }
            
            let chipsHtml = '<div class="card-extras">';
            
            for (const [key, value] of Object.entries(extra)) {
                if (key === 'icon') continue; // Skip icon as it's used in title
                
                let chipClass = 'extra-chip';
                if (key === 'priority') {
                    chipClass += ` priority-${value.toLowerCase()}`;
                } else if (key === 'category') {
                    chipClass += ` category-${value.toLowerCase()}`;
                }
                
                chipsHtml += `<span class="${chipClass}">${escapeHtml(key)}: ${escapeHtml(value)}</span>`;
            }
            
            chipsHtml += '</div>';
            return chipsHtml;
        }
        
        // ===== BROWSER NOTIFICATIONS =====
        function setupNotificationPermission() {
            if ('Notification' in window && Notification.permission === 'default') {
                Notification.requestPermission();
            }
        }
        
        function showBrowserNotification(data) {
            if ('Notification' in window && Notification.permission === 'granted' && !isPageVisible) {
                const notification = new Notification(data.title, {
                    body: data.message || data.body,
                    icon: '/favicon.ico',
                    tag: 'notification-' + data.id
                });
                
                notification.onclick = function() {
                    window.focus();
                    notification.close();
                };
                
                // Auto close after 5 seconds
                setTimeout(() => notification.close(), 5000);
            }
        }
        
        // ===== TOAST NOTIFICATIONS =====
        function showToast(message) {
            const toast = document.createElement('div');
            toast.className = 'toast';
            toast.textContent = message;
            
            document.body.appendChild(toast);
            
            // Show toast
            setTimeout(() => toast.classList.add('show'), 100);
            
            // Hide and remove toast
            setTimeout(() => {
                toast.classList.remove('show');
                setTimeout(() => document.body.removeChild(toast), 300);
            }, 3000);
        }
        
        // ===== NOTIFICATION SOUND =====
        function playNotificationSound() {
            // Create a simple beep sound using Web Audio API
            try {
                const audioContext = new (window.AudioContext || window.webkitAudioContext)();
                const oscillator = audioContext.createOscillator();
                const gainNode = audioContext.createGain();
                
                oscillator.connect(gainNode);
                gainNode.connect(audioContext.destination);
                
                oscillator.frequency.value = 800;
                oscillator.type = 'sine';
                
                gainNode.gain.setValueAtTime(0.1, audioContext.currentTime);
                gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.1);
                
                oscillator.start(audioContext.currentTime);
                oscillator.stop(audioContext.currentTime + 0.1);
            } catch (error) {
                console.log('Could not play notification sound:', error);
            }
        }
        
        // ===== UNREAD COUNT =====
        function updateUnreadCount() {
            unreadCountEl.textContent = unreadCount;
            unreadCountEl.style.display = unreadCount > 0 ? 'inline-block' : 'none';
            
            // Update page title
            document.title = unreadCount > 0 ? `(${unreadCount}) Bảng Tin Thông Báo` : 'Bảng Tin Thông Báo';
        }
        
        function resetUnreadCount() {
            unreadCount = 0;
            updateUnreadCount();
        }
        
        // ===== PAGE VISIBILITY =====
        function setupPageVisibilityHandler() {
            document.addEventListener('visibilitychange', function() {
                isPageVisible = !document.hidden;
                if (isPageVisible) {
                    resetUnreadCount();
                }
            });
            
            window.addEventListener('focus', function() {
                isPageVisible = true;
                resetUnreadCount();
            });
            
            window.addEventListener('blur', function() {
                isPageVisible = false;
            });
        }
        
        // ===== UTILITY FUNCTIONS =====
        function escapeHtml(text) {
            const map = {
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#039;'
            };
            return text.replace(/[&<>"']/g, function(m) { return map[m]; });
        }
        
        // ===== ERROR HANDLING =====
        window.addEventListener('error', function(e) {
            console.error('JavaScript error:', e.error);
        });
        
        window.addEventListener('unhandledrejection', function(e) {
            console.error('Unhandled promise rejection:', e.reason);
        });
    </script>
</body>
</html>
""";
    
    // ===== MAIN METHOD =====
    
    /**
     * Entry point của ứng dụng
     */
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel if system look and feel is not available
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                // Keep default look and feel
            }
        }
        
        // Create and run application
        SwingUtilities.invokeLater(() -> {
            new NotifyServerGui();
        });
    }
}
