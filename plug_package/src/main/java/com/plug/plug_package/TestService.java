package com.plug.plug_package;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.plug.plug_package.base.BaseService;

/**
 * 测试类
 * Created by A35 on 2020/3/11
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class TestService extends BaseService {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(">>>", " TestService --- onStartCommand");
        Toast.makeText(mService, "TestService开启", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }
}
