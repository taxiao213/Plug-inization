package com.plug.inization;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.plug.inization.base.PluginManager;
import com.plug.inization.base.ProxyActivity;
import com.plug.inization.base.ProxyService;
import com.plug.standar.Constant;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View load = findViewById(R.id.load);
        View plug = findViewById(R.id.plug);
        View loadService = findViewById(R.id.load_service);
        View registerReceiver = findViewById(R.id.register_receiver);
        View sendReceiver = findViewById(R.id.send_receiver);
        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPlug();
            }
        });

        plug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlugActivity();
            }
        });

        loadService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadService();
            }
        });

        registerReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerReceiver();
            }
        });
        sendReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendReceiver();
            }
        });
    }

    public void loadPlug() {
        PluginManager.getInstance(this).loadPlugin();
    }

    // 跳转插件Activity
    public void startPlugActivity() {
        // 获取插件APK的所有Activity
        String packagePath = PluginManager.getInstance(this).getPlugFile().getAbsolutePath();
        PackageManager packageManager = getPackageManager();
        PackageInfo info = packageManager.getPackageArchiveInfo(packagePath, PackageManager.GET_ACTIVITIES);
        // 获取AndroidManifest.xml 按顺序
        ActivityInfo activity = info.activities[0];
        // 占位 代理Activity
        Intent intent = new Intent(this, ProxyActivity.class);
        intent.putExtra(Constant.CLASS_NAME, activity.name);
        startActivity(intent);
    }

    // 加载Service
    private void loadService() {
        startService(new Intent(this, ProxyService.class));
    }

    // 注册静态服务
    private void registerReceiver() {
        PluginManager.getInstance(this).parserApk();
    }

    // 发送广播
    private void sendReceiver() {
        Intent intent = new Intent();
        intent.setAction(Constant.BROADCAST_ACTION);
        sendBroadcast(intent);
    }
}
