# 编译 APK 指南

## 方法1：使用 Android Studio（推荐新手）

### 步骤

1. **安装 Android Studio**
   - 下载：https://developer.android.com/studio
   - 安装并启动

2. **打开项目**
   - `File` → `Open`
   - 选择 `forum-monitor/android-app` 目录

3. **等待 Gradle 同步**
   - 首次打开会自动下载依赖，需要几分钟
   - 等待左下角的进度条完成

4. **编译 APK**
   - `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 等待编译完成

5. **获取 APK**
   - 编译完成后会弹出通知
   - 点击 `locate` 查找 APK 位置
   - 路径：`app/build/outputs/apk/debug/app-debug.apk`

---

## 方法2：使用命令行（推荐开发者）

### 前置要求

- 安装 JDK 11+
- 安装 Android SDK

### 步骤

```bash
# 1. 进入项目目录
cd /Users/donghua/WorkBuddy/20260325183220/forum-monitor/android-app

# 2. 给 gradlew 添加执行权限
chmod +x gradlew

# 3. 清理旧文件
./gradlew clean

# 4. 编译 Debug 版本
./gradlew assembleDebug

# 5. 编译 Release 版本（如果需要）
./gradlew assembleRelease
```

### APK 位置

- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release.apk`

---

## 常见问题

### Gradle 同步失败

```bash
# 手动清理缓存
./gradlew clean --no-daemon

# 重新同步
./gradlew build --refresh-dependencies
```

### JDK 版本问题

```bash
# 检查 JDK 版本
java -version

# 如果版本不对，设置 JAVA_HOME
export JAVA_HOME=/path/to/jdk11
```

### Gradle 下载慢

配置国内镜像：

在 `build.gradle` 中添加：

```gradle
repositories {
    maven { url 'https://maven.aliyun.com/repository/google' }
    maven { url 'https://maven.aliyun.com/repository/public' }
    google()
    mavenCentral()
}
```

---

## 签名（Release 版本）

如果需要签名 Release APK：

1. 创建密钥库：
```bash
keytool -genkey -v -keystore forum-monitor.keystore -alias forum-monitor -keyalg RSA -keysize 2048 -validity 10000
```

2. 在 `app/build.gradle` 中配置签名：

```gradle
android {
    signingConfigs {
        release {
            storeFile file("forum-monitor.keystore")
            storePassword "your-password"
            keyAlias "forum-monitor"
            keyPassword "your-password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            // ...
        }
    }
}
```
