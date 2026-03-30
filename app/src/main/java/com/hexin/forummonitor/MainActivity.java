package com.hexin.forummonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

/**
 * 主界面：配置和管理监控服务
 */
public class MainActivity extends Activity {

    private EditText etUsers;
    private EditText etInterval;
    private EditText etServerChanKey;
    private EditText etEmailAccount;
    private EditText etEmailAuth;
    private EditText etNotifyEmail;
    private Switch swStart;
    private Switch swStop;
    private Button btnSave;
    private Button btnOpenAccessibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadSettings();
    }

    private void initViews() {
        etUsers = findViewById(R.id.etUsers);
        etInterval = findViewById(R.id.etInterval);
        etServerChanKey = findViewById(R.id.etServerChanKey);
        etEmailAccount = findViewById(R.id.etEmailAccount);
        etEmailAuth = findViewById(R.id.etEmailAuth);
        etNotifyEmail = findViewById(R.id.etNotifyEmail);
        swStart = findViewById(R.id.swStart);
        swStop = findViewById(R.id.swStop);
        btnSave = findViewById(R.id.btnSave);
        btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility);

        // 保存配置
        btnSave.setOnClickListener(v -> saveSettings());

        // 打开无障碍设置
        btnOpenAccessibility.setOnClickListener(v -> openAccessibilitySettings());

        // 开始监控
        swStart.setOnClickListener(v -> {
            if (swStart.isChecked()) {
                startMonitoring();
            }
        });

        // 停止监控
        swStop.setOnClickListener(v -> {
            if (swStop.isChecked()) {
                stopMonitoring();
            }
        });
    }

    /**
     * 保存配置
     */
    private void saveSettings() {
        String users = etUsers.getText().toString().trim();
        String interval = etInterval.getText().toString().trim();
        String serverKey = etServerChanKey.getText().toString().trim();
        String emailAccount = etEmailAccount.getText().toString().trim();
        String emailAuth = etEmailAuth.getText().toString().trim();
        String notifyEmail = etNotifyEmail.getText().toString().trim();

        if (TextUtils.isEmpty(users)) {
            Toast.makeText(this, "请输入监控用户", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int intervalSec = Integer.parseInt(interval);
            if (intervalSec < 60) {
                Toast.makeText(this, "检查间隔不能小于60秒", Toast.LENGTH_SHORT).show();
                return;
            }

            // 保存到SharedPreferences
            getSharedPreferences("config", Context.MODE_PRIVATE)
                .edit()
                .putString("users", users)
                .putInt("interval", intervalSec)
                .putString("serverChanKey", serverKey)
                .putString("emailAccount", emailAccount)
                .putString("emailAuth", emailAuth)
                .putString("notifyEmail", notifyEmail)
                .apply();

            Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "检查间隔必须是数字", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 加载配置
     */
    private void loadSettings() {
        getSharedPreferences("config", Context.MODE_PRIVATE).apply();

        String users = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("users", "君子先修心");
        int interval = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getInt("interval", 300);
        String serverKey = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("serverChanKey", "");
        String emailAccount = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("emailAccount", "");
        String emailAuth = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("emailAuth", "");
        String notifyEmail = getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("notifyEmail", "");

        etUsers.setText(users);
        etInterval.setText(String.valueOf(interval));
        etServerChanKey.setText(serverKey);
        etEmailAccount.setText(emailAccount);
        etEmailAuth.setText(emailAuth);
        etNotifyEmail.setText(notifyEmail);
    }

    /**
     * 打开无障碍设置
     */
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "请开启'论坛监控'无障碍服务", Toast.LENGTH_LONG).show();
    }

    /**
     * 开始监控
     */
    private void startMonitoring() {
        Toast.makeText(this, "监控已启动", Toast.LENGTH_SHORT).show();
        // 服务会自动开始检测
    }

    /**
     * 停止监控
     */
    private void stopMonitoring() {
        Toast.makeText(this, "监控已停止", Toast.LENGTH_SHORT).show();
        swStart.setChecked(false);
    }

    /**
     * 检查无障碍服务是否开启
     */
    private boolean isAccessibilityServiceEnabled() {
        String serviceName = getPackageName() + "/.ForumMonitorService";
        String enabledServices = Settings.Secure.getString(
            getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        return enabledServices != null && enabledServices.contains(serviceName);
    }
}
