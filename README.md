# 同花顺论坛监控 - Android 原生版

基于 Android 无障碍服务的同花顺论坛监控工具，可直接在 OPPO R17 等安卓设备上运行，无需小主机。

---

## ✨ 核心优势

| 特性 | Python ADB 版本 | Android 原生版本 |
|------|----------------|-----------------|
| 运行环境 | 需要小主机/云服务器 | 直接在手机上运行 |
| 稳定性 | 🟡 中（ADB可能断线） | 🟢 高（无障碍服务稳定） |
| 响应速度 | 🟡 慢（截图+OCR） | 🟢 快（直接读界面） |
| 权限要求 | USB 调试 | 无障碍权限 |
| 硬件要求 | 小主机 + USB线 | 仅需手机本身 |

---

## 📦 功能特性

- ✅ 实时监控指定用户的帖子
- ✅ 自动点击"最近访问"标签
- ✅ 定时检测（可配置间隔）
- ✅ Server酱微信通知
- ✅ QQ邮件通知
- ✅ 交易时段限制（9:30-19:00）
- ✅ 后台运行（前台服务保活）

---

## 🛠️ 编译步骤

### 前置要求

1. **安装 Android Studio**
   - 下载：https://developer.android.com/studio
   - 安装最新版本

2. **安装 JDK**
   - Android Studio 内置 JDK，或手动安装 JDK 11+

### 编译 APK

```bash
# 1. 进入项目目录
cd forum-monitor/android-app

# 2. 使用 Gradle 编译
./gradlew assembleDebug

# 3. APK 生成位置
# android-app/app/build/outputs/apk/debug/app-debug.apk
```

或在 Android Studio 中：
1. 打开项目 `File` → `Open` → 选择 `android-app` 目录
2. 等待 Gradle 同步完成
3. `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`

---

## 📱 安装与配置

### 1. 安装 APK

将 `app-debug.apk` 安装到 OPPO R17：

```bash
# 通过 ADB 安装（如果有 USB 连接）
adb install app-debug.apk

# 或直接复制到手机，用文件管理器安装
```

### 2. 开启无障碍服务

1. 打开「论坛监控」App
2. 点击 **「开启无障碍服务」** 按钮
3. 系统跳转到设置页面
4. 找到 **「论坛监控」**，开启开关
5. 返回 App，看到「服务已启动」提示

### 3. 配置监控参数

| 配置项 | 说明 | 示例 |
|--------|------|------|
| 监控用户 | 多个用户用逗号分隔 | `君子先修心, 君子先修心2` |
| 检查间隔 | 检测频率（秒，建议≥60） | `300` (5分钟) |
| Server酱Key | 微信通知（可选） | `SCT123456` |
| QQ邮箱账号 | 发件邮箱 | `xxx@qq.com` |
| QQ邮箱授权码 | 授权码（非登录密码） | `abcdefgh` |
| 通知邮箱 | 接收通知的邮箱 | `target@email.com` |

#### 获取 Server酱 Key
1. 访问 https://sct.ftqq.com/
2. 登录并创建应用
3. 复制 SendKey

#### 获取 QQ 邮箱授权码
1. 登录 QQ 邮箱 → 设置 → 账户
2. 开启 POP3/SMTP 服务
3. 生成授权码

### 4. 启动监控

1. 打开同花顺 APP，**进入论坛页面**
2. 点击「论坛监控」App 中的 **「启动监控」** 开关
3. App 会在后台自动运行

---

## ⚙️ 运行原理

### 无障碍服务流程

```
1. 监听同花顺APP界面变化
   ↓
2. 识别"最近访问"标签 → 自动点击
   ↓
3. 查找帖子列表中的目标用户
   ↓
4. 发现新帖子 → 记录时间戳
   ↓
5. 发送通知（Server酱 + 邮件）
```

### 定时检测机制

- 每 N 秒（默认 300 秒）执行一次检测
- 仅在交易时段（9:30-19:00）运行
- 检测到新帖子立即通知，不重复通知

---

## 🔧 调试技巧

### 查看日志

使用 ADB 查看 App 日志：

```bash
adb logcat | grep ForumMonitor
```

### 手动测试

1. 在同花顺论坛页面手动刷新
2. 观察 App 日志是否输出 `发现新帖子`
3. 检查微信/邮件是否收到通知

---

## ⚠️ 注意事项

### OPPO R17 特殊设置

1. **开发者选项**：
   - USB调试 → 开启
   - USB 调试（安全设置）→ 开启
   - 停用权限监控 → 开启

2. **省电策略**：
   - 设置 → 电池 → 关闭「自动优化」
   - 将「论坛监控」App 加入白名单

3. **后台保活**：
   - ColorOS 可能会杀后台，建议开启「不锁定屏幕」（充电时保持亮屏）

### 无障碍权限限制

- 同花顺 APP 更新后可能影响无障碍服务
- 如监控失效，尝试重新开启无障碍服务
- 部分敏感页面无障碍服务无法读取

---

## 🆚 与 Python ADB 版本对比

| 维度 | Python ADB | Android 原生 |
|------|-----------|-------------|
| 硬件依赖 | 小主机/树莓派 + USB 线 | 无 |
| 部署复杂度 | 🔴 高 | 🟢 低 |
| 维护成本 | 🟡 中（ADB保活代码复杂） | 🟢 低 |
| 性能 | 🟡 慢（OCR耗资源） | 🟢 快（直接读文本） |
| 适用场景 | 电脑/服务器环境 | 手机直接运行 |

---

## 📋 代码结构

```
android-app/
├── app/
│   └── src/main/
│       ├── java/com/hexin/forummonitor/
│       │   ├── MainActivity.java          # 主界面
│       │   ├── ForumMonitorService.java   # 无障碍服务（核心）
│       │   └── Notifier.java             # 通知模块
│       ├── res/
│       │   ├── layout/
│       │   │   └── activity_main.xml      # 主界面布局
│       │   ├── values/
│       │   │   ├── colors.xml
│       │   │   ├── strings.xml
│       │   │   └── themes.xml
│       │   └── xml/
│       │       └── accessibility_service_config.xml
│       └── AndroidManifest.xml
├── build.gradle
└── README.md
```

---

## 🚀 下一步优化方向

1. **日志优化**：增加本地日志记录，方便排查问题
2. **多账号支持**：支持多个 Server酱 Key 和邮箱
3. **自动截图**：发现新帖子时自动截图保存
4. **数据统计**：统计每日检测次数和发现帖子数
5. **深色模式**：适配系统深色模式

---

## 📞 问题反馈

如遇到问题，请提供：
1. OPPO R17 系统版本
2. 同花顺 APP 版本号
3. ADB 日志（`adb logcat | grep ForumMonitor`）
