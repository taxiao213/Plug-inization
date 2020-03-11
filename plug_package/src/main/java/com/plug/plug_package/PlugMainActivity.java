package com.plug.plug_package;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.plug.plug_package.base.BaseActivity;
import com.plug.standar.Constant;

import java.awt.font.TextAttribute;


public class PlugMainActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plug_main);
        // this 会报错，因为插件没有安装，也没有组件的环境，所以必须使用宿主环境
        Toast.makeText(mActivity, "我是插件", Toast.LENGTH_SHORT).show();

        // 跳转内部activity
        findViewByID(R.id.jump_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 插件内的类跳转，需要依赖宿主环境
                startActivity(new Intent(mActivity, TestActivity.class));
            }
        });

        // 跳转内部service
        findViewByID(R.id.jump_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 插件内的类跳转，需要依赖宿主环境
                startService(new Intent(mActivity, TestService.class));
            }
        });

        // 注册Broadcast
        findViewByID(R.id.jump_broadcast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TestBroadcast testBroadcast = new TestBroadcast();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Constant.BROADCAST_ACTION);
                registerReceiver(testBroadcast, intentFilter);
            }
        });

        // 发送Broadcast
        findViewByID(R.id.send_broadcast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 插件内的类跳转，需要依赖宿主环境
                Intent intent = new Intent();
                intent.setAction(Constant.BROADCAST_ACTION);
                sendBroadcast(intent);
            }
        });
    }
}
