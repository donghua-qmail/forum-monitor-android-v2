# 使用 GitHub Actions 自动编译 APK

## 🎯 快速开始

### 方法1：GitHub Actions（推荐，无需本地环境）

#### 步骤1：推送到 GitHub

```bash
# 1. 在 GitHub 上创建新仓库
# 访问 https://github.com/new
# 仓库名：forum-monitor-android

# 2. 初始化 Git 仓库
cd /Users/donghua/WorkBuddy/20260325183220/forum-monitor/android-app

# 3. 初始化并推送到 GitHub
git init
git add .
git commit -m "Initial commit: Android forum monitor"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/forum-monitor-android.git
git push -u origin main
```

#### 步骤2：触发编译

1. 访问 GitHub 仓库页面
2. 点击 `Actions` 标签
3. 选择 `Build APK` workflow
4. 点击 `Run workflow`
5. 输入版本号（如：1.0.0）
6. 点击 `Run workflow` 绿色按钮

#### 步骤3：下载 APK

编译完成后（约 5-10 分钟）：

1. 进入 `Actions` → `Build APK` → 最新的运行记录
2. 滚动到底部，找到 `Artifacts`
3. 下载：
   - `forum-monitor-debug-1.0.0.zip`（Debug 版本）
   - `forum-monitor-release-1.0.0.zip`（Release 版本）
4. 解压得到 `app-debug.apk`

---

## 方法2：使用 Docker（本地有 Docker）

```bash
# 1. 构建编译环境
cd forum-monitor/android-app
docker run --rm -v "$PWD":/app -w /app openjdk:17-slim bash -c "
    apt-get update &&
    apt-get install -y wget unzip &&
    wget https://services.gradle.org/distributions/gradle-8.0-bin.zip &&
    unzip gradle-8.0-bin.zip &&
    export PATH=$PATH:/app/gradle-8.0/bin &&
    chmod +x gradlew &&
    ./gradlew assembleDebug
"

# 2. APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 方法3：安装 Android Studio（长期开发）

### 1. 下载安装

- Mac：https://dl.google.com/dl/android/studio/install/2024.2.2.12/mac/android-studio-2024.2.2.12-mac.dmg
- Windows：https://dl.google.com/dl/android/studio/install/2024.2.2.12/windows/android-studio-2024.2.2.12-windows.exe

### 2. 安装 JDK

```bash
# macOS
brew install openjdk@17

# 设置环境变量
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
source ~/.zshrc
```

### 3. 编译

```bash
cd forum-monitor/android-app
./gradlew assembleDebug
```

---

## 📱 安装 APK 到 OPPO R17

### 方法1：USB 安装

```bash
# 1. 连接手机到电脑，开启 USB 调试
adb devices

# 2. 安装 APK
adb install app-debug.apk

# 3. 如果已安装，覆盖安装
adb install -r app-debug.apk
```

### 方法2：直接安装

1. 将 `app-debug.apk` 复制到手机
2. 用文件管理器打开
3. 点击安装（可能需要允许「未知来源」）

---

## 🔍 Debug 和 Release 版本区别

| 版本 | 签名 | 大小 | 用途 |
|------|------|------|------|
| Debug | Debug 签名 | 稍大 | 测试、开发 |
| Release | 未签名 | 稍小 | 需要自行签名 |

### 签名 Release APK（可选）

```bash
# 1. 生成密钥库
keytool -genkey -v -keystore forum-monitor.keystore -alias forum-monitor -keyalg RSA -keysize 2048 -validity 10000

# 2. 使用密钥库签名
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA256 \
  -keystore forum-monitor.keystore \
  -storepass your-password \
  -keypass your-password \
  app-release-unsigned.apk \
  forum-monitor

# 3. 使用 zipalign 优化
zipalign -v 4 app-release-unsigned.apk forum-monitor-signed.apk
```

---

## ⚠️ 常见问题

### 1. GitHub Actions 编译失败

**原因**：Gradle 下载依赖失败

**解决**：在 `.github/workflows/build-apk.yml` 中添加国内镜像

```yaml
- name: 配置国内镜像
  run: |
    mkdir -p ~/.gradle
    echo 'repositories { maven { url "https://maven.aliyun.com/repository/public" } }' > ~/.gradle/init.gradle
```

### 2. 安装时提示"解析包错误"

**原因**：APK 下载不完整

**解决**：重新下载，确保文件完整

### 3. APK 无法安装到 OPPO R17

**原因**：ColorOS 安全设置阻止

**解决**：
- 设置 → 安全 → 允许安装未知来源应用 → 启用
- 开发者选项 → USB 安装 → 开启

---

## 📞 下一步

编译完成后，参考 `README.md` 中的使用说明配置和运行监控服务。
