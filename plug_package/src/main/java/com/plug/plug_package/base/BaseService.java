package com.plug.plug_package.base;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.plug.standar.ServiceInterface;

/**
 * 插件BaseService
 * Created by A35 on 2020/3/11
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class BaseService extends Service implements ServiceInterface {

    public Service mService;

    // 注入宿主 Service
    @Override
    public void insertServiceContext(Service service) {
        this.mService = service;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(">>>", " BaseService --- onCreate");
    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 0;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
