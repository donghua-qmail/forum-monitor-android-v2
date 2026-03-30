package com.hexin.forummonitor;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 同花顺论坛无障碍监控服务
 * 功能：
 * 1. 监控同花顺 APP 界面变化
 * 2. 查找目标用户的帖子
 * 3. 自动点击"最近访问"标签
 * 4. 定时检测并发送通知
 */
public class ForumMonitorService extends AccessibilityService {

    private static final String TAG = "ForumMonitorService";

    // 监控配置
    private String[] monitoredUsers = {"君子先修心", "君子先修心2"};
    private int checkInterval = 300; // 5分钟（秒）
    private int tradingStartHour = 9;
    private int tradingStartMinute = 30;
    private int tradingEndHour = 19;
    private int tradingEndMinute = 0;

    // Server酱配置
    private String serverChanKey = ""; // 需要用户配置
    private String serverChanUrl = "https://sctapi.ftqq.com/";

    // QQ邮箱配置
    private String qqEmailAccount = ""; // 需要用户配置
    private String qqEmailAuthCode = ""; // 需要用户配置
    private String notifyEmail = ""; // 通知邮箱

    // 状态
    private Handler handler;
    private Runnable checkRunnable;
    private String lastPostTime = "";
    private boolean isMonitoring = false;
    private Notifier notifier;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "监控服务已启动");
        handler = new Handler(Looper.getMainLooper());

        // 初始化通知器
        notifier = new Notifier();

        // 从SharedPreferences读取配置
        loadConfig();

        // 启动定时检测
        startPeriodicCheck();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 只处理同花顺APP的事件
        if (!event.getPackageName().equals("com.hexin.plat.android")) {
            return;
        }

        // 获取根节点
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Log.d(TAG, "窗口状态改变: " + event.getClassName());
                handleWindowChange(rootNode);
                break;

            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                handleContentChange(rootNode);
                break;
        }

        rootNode.recycle();
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "服务被中断");
    }

    /**
     * 处理窗口变化
     */
    private void handleWindowChange(AccessibilityNodeInfo rootNode) {
        // 检查是否在同花顺论坛界面
        if (isForumPage(rootNode)) {
            Log.i(TAG, "进入论坛页面");
            // 自动点击"最近访问"标签
            clickRecentTab(rootNode);
        }
    }

    /**
     * 处理内容变化
     */
    private void handleContentChange(AccessibilityNodeInfo rootNode) {
        // 查找帖子列表
        List<AccessibilityNodeInfo> postNodes = findPostList(rootNode);
        if (postNodes != null && !postNodes.isEmpty()) {
            // 查找目标用户的帖子
            for (String user : monitoredUsers) {
                if (findUserPost(rootNode, user)) {
                    Log.i(TAG, "发现目标用户: " + user);
                    sendNotification(user);
                    break;
                }
            }
        }
    }

    /**
     * 判断是否是论坛页面
     */
    private boolean isForumPage(AccessibilityNodeInfo rootNode) {
        // 查找"论坛"、"帖子"等关键词
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText("论坛");
        return nodes != null && !nodes.isEmpty();
    }

    /**
     * 查找帖子列表
     */
    private List<AccessibilityNodeInfo> findPostList(AccessibilityNodeInfo rootNode) {
        // 尝试通过资源ID查找列表控件
        List<AccessibilityNodeInfo> listNodes = rootNode.findAccessibilityNodeInfosByViewId(
            "com.hexin.plat.android:id/recyclerView"
        );
        if (listNodes != null && !listNodes.isEmpty()) {
            return listNodes;
        }

        // 如果找不到，查找包含"帖子"文本的节点
        return rootNode.findAccessibilityNodeInfosByText("帖子");
    }

    /**
     * 查找特定用户的帖子
     */
    private boolean findUserPost(AccessibilityNodeInfo rootNode, String userName) {
        // 查找包含用户名的节点
        List<AccessibilityNodeInfo> userNodes = rootNode.findAccessibilityNodeInfosByText(userName);

        if (userNodes != null && !userNodes.isEmpty()) {
            for (AccessibilityNodeInfo userNode : userNodes) {
                // 检查是否是帖子标题区域（父节点包含帖子相关内容）
                AccessibilityNodeInfo parent = userNode.getParent();
                if (parent != null && containsPostContent(parent)) {
                    // 提取帖子时间
                    String postTime = extractPostTime(parent);
                    if (!postTime.equals(lastPostTime)) {
                        lastPostTime = postTime;
                        Log.i(TAG, "发现新帖子 - 用户: " + userName + ", 时间: " + postTime);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 判断节点是否包含帖子内容
     */
    private boolean containsPostContent(AccessibilityNodeInfo node) {
        CharSequence text = node.getText();
        if (text != null && text.length() > 10) { // 假设帖子标题长度>10
            return true;
        }

        // 检查子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean result = containsPostContent(child);
                child.recycle();
                if (result) return true;
            }
        }

        return false;
    }

    /**
     * 提取帖子时间
     */
    private String extractPostTime(AccessibilityNodeInfo node) {
        CharSequence text = node.getText();
        if (text != null) {
            // 匹配时间格式（如: 10分钟前, 刚刚, 2024-03-30 10:30）
            Pattern pattern = Pattern.compile("(\\d+分钟前|刚刚|\\d+:\\d+)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    /**
     * 点击"最近访问"标签
     */
    private void clickRecentTab(AccessibilityNodeInfo rootNode) {
        // 查找"最近访问"文本
        List<AccessibilityNodeInfo> tabNodes = rootNode.findAccessibilityNodeInfosByText("最近访问");

        if (tabNodes != null && !tabNodes.isEmpty()) {
            AccessibilityNodeInfo tabNode = tabNodes.get(0);
            // 执行点击
            boolean clicked = tabNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.i(TAG, clicked ? "成功点击'最近访问'标签" : "点击'最近访问'标签失败");
            tabNode.recycle();
        } else {
            Log.d(TAG, "未找到'最近访问'标签，可能已经在该页面");
        }
    }

    /**
     * 启动定时检测
     */
    private void startPeriodicCheck() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTradingTime() && isMonitoring) {
                    performCheck();
                }
                handler.postDelayed(this, checkInterval * 1000);
            }
        };
        handler.postDelayed(checkRunnable, checkInterval * 1000);
    }

    /**
     * 判断是否是交易时间
     */
    private boolean isTradingTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        int currentMinutes = hour * 60 + minute;
        int startMinutes = tradingStartHour * 60 + tradingStartMinute;
        int endMinutes = tradingEndHour * 60 + tradingEndMinute;

        return currentMinutes >= startMinutes && currentMinutes <= endMinutes;
    }

    /**
     * 执行检测
     */
    private void performCheck() {
        Log.d(TAG, "执行定时检测");

        // 打开同花顺APP
        openTonghuashun();

        // 等待界面加载（给2秒）
        handler.postDelayed(() -> {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // 查找目标用户
                for (String user : monitoredUsers) {
                    if (findUserPost(rootNode, user)) {
                        Log.i(TAG, "检测到新帖子: " + user);
                        sendNotification(user);
                        rootNode.recycle();
                        return;
                    }
                }
                rootNode.recycle();
            }
        }, 2000);
    }

    /**
     * 打开同花顺APP
     */
    private void openTonghuashun() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.hexin.plat.android");
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.d(TAG, "打开同花顺APP");
        } else {
            Log.w(TAG, "未找到同花顺APP");
        }
    }

    /**
     * 发送通知
     */
    private void sendNotification(String userName) {
        // 提取帖子时间
        String postTime = lastPostTime.isEmpty() ? "刚刚" : lastPostTime;

        // 使用Notifier发送通知
        notifier.sendMonitorAlert(
            serverChanKey,
            qqEmailAccount,
            qqEmailAuthCode,
            notifyEmail,
            userName,
            postTime
        );
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        try {
            String usersStr = getSharedPreferences("config", MODE_PRIVATE)
                .getString("users", "君子先修心");
            if (usersStr != null && !usersStr.isEmpty()) {
                monitoredUsers = usersStr.split(",");
                // 去除前后空格
                for (int i = 0; i < monitoredUsers.length; i++) {
                    monitoredUsers[i] = monitoredUsers[i].trim();
                }
            }

            checkInterval = getSharedPreferences("config", MODE_PRIVATE)
                .getInt("interval", 300);

            serverChanKey = getSharedPreferences("config", MODE_PRIVATE)
                .getString("serverChanKey", "");

            qqEmailAccount = getSharedPreferences("config", MODE_PRIVATE)
                .getString("emailAccount", "");

            qqEmailAuthCode = getSharedPreferences("config", MODE_PRIVATE)
                .getString("emailAuth", "");

            notifyEmail = getSharedPreferences("config", MODE_PRIVATE)
                .getString("notifyEmail", "");

            Log.i(TAG, "配置加载完成: 用户数=" + monitoredUsers.length + ", 间隔=" + checkInterval + "秒");

        } catch (Exception e) {
            Log.e(TAG, "加载配置失败", e);
        }
    }

    /**
     * 更新配置
     */
    public void updateConfig(String[] users, int interval, String serverKey,
                            String emailAccount, String emailAuth, String notifyEmail) {
        this.monitoredUsers = users;
        this.checkInterval = interval;
        this.serverChanKey = serverKey;
        this.qqEmailAccount = emailAccount;
        this.qqEmailAuthCode = emailAuth;
        this.notifyEmail = notifyEmail;

        Log.i(TAG, "配置已更新");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        Log.i(TAG, "监控服务已停止");
    }
}
