package com.plug.plug_package;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.plug.plug_package.base.BaseActivity;


public class PlugMainActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plug_main);
        // this 会报错，因为插件没有安装，也没有组件的环境，所以必须使用宿主环境
        Toast.makeText(mActivity, "我是插件", Toast.LENGTH_SHORT).show();
        findViewByID(R.id.jump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 插件内的类跳转，需要依赖宿主环境
                startActivity(new Intent(mActivity, TestActivity.class));
            }
        });
    }
}
