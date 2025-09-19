# 📡 Java Notification System Projects

Dự án Java hoàn chỉnh với GUI và Web Client cho hệ thống thông báo real-time.

## 📋 Tổng quan

Repository này chứa hai dự án độc lập:

1. **NotifyServerGui.java** - Hệ thống thông báo với giao diện Facebook-like

---

## 🚀 Dự án 1: NotifyServerGui

### ✨ Tính năng
- **GUI Swing** với Material Design tiếng Việt
- **Web Interface** giống Facebook newsfeed
- **Server Sent Events (SSE)** cho real-time messaging
- **JSON message format**
- **Port**: 8080

### 📁 Cấu trúc
```
📡 NotifyServerGui.java
├── GUI Components (Swing)
├── HTTP Server (com.sun.net.httpserver)
├── SSE Gateway (/events)
└── Web Client HTML/CSS/JS
```

### 🎯 Cách chạy

#### Bước 1: Compile
```bash
javac NotifyServerGui.java
```

#### Bước 2: Chạy ứng dụng
```bash
java NotifyServerGui
```

#### Bước 3: Truy cập Web
Mở trình duyệt và truy cập: **http://localhost:8080**

### 🎨 Sử dụng GUI
1. **Tiêu đề**: Nhập tiêu đề thông báo
2. **Tin nhắn**: Nhập tin nhắn ngắn
3. **Nội dung**: Nhập nội dung chi tiết
4. **Thuộc tính mở rộng**: 
   - Chọn độ ưu tiên (thấp/trung bình/cao/khẩn cấp)
   - Nhập icon emoji (🎉, 📢, ⚠️, 🚀)
   - Chọn danh mục (tin tức/thông báo/cảnh báo/sự kiện/hệ thống)
   - Tùy chỉnh thêm: `key=value;key2=value2`
5. **Nhấn "🚀 Phát Sóng"** để gửi thông báo

### 🌐 Web Features
- **Facebook-like newsfeed** với card layout
- **Real-time notifications** qua SSE
- **Toast notifications** và browser notifications
- **Sound alerts** khi có tin nhắn mới
- **Unread counter** với page title update
- **Auto-reconnection** khi mất kết nối

---

## ⚙️ Yêu cầu hệ thống

### 📋 Prerequisites
- **Java JDK 11+** (OpenJDK hoặc Oracle JDK)
- **Web Browser** (Chrome, Firefox, Safari, Edge)
- **Port availability**: 8080 và 9090

### 🔧 Kiểm tra Java
```bash
java -version
javac -version
```

---


## 🛠️ Troubleshooting

### ❌ Lỗi thường gặp

#### 1. Port already in use
```bash
# Kiểm tra port đang sử dụng
netstat -an | findstr :8080
netstat -an | findstr :9090

# Hoặc kill process đang dùng port
taskkill /f /pid <PID>
```

#### 2. Java compilation error
```bash
# Đảm bảo đang ở đúng thư mục
pwd
ls -la

# Compile với verbose
javac -verbose NotifyServerGui.java
```

#### 3. Web không load
- Kiểm tra firewall/antivirus
- Thử truy cập `127.0.0.1` thay vì `localhost`
- Kiểm tra console browser (F12) để xem lỗi

#### 4. SSE/EventSource không hoạt động
- Đảm bảo browser hỗ trợ EventSource
- Kiểm tra network tab trong developer tools
- Thử refresh trang web

---

## 📝 Tác giả

### NotifyServerGui
- **Tác giả**: NguyenVanChung
- **Student ID**: 28219106698

### MessageBroadcaster
- **Tác giả**: NguyenTanCanh  
- **Student ID**: 28219044749

---

## 🎉 Demo

#### Terminal 1:
```bash
javac NotifyServerGui.java
java NotifyServerGui
```

#### Browser:
- **Dự án 1**: http://localhost:8080 (Facebook-like)

### 📱 Test real-time messaging:
1. Mở cả hai web interfaces
2. Gửi tin nhắn từ GUI của dự án 1
4. Xem tin nhắn xuất hiện real-time trên web

---

## 📄 License

Dự án học tập - sử dụng tự do cho mục đích giáo dục.

---

## 🔗 Links

- **Repository**: Thư mục hiện tại
- **Java Documentation**: https://docs.oracle.com/en/java/
- **Material Design**: https://material.io/design

---

*📅 Last updated: September 2025*
