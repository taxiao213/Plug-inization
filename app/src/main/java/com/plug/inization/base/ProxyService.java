package com.plug.inization.base;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.plug.standar.Constant;
import com.plug.standar.ServiceInterface;

/**
 * Created by A35 on 2020/3/11
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class ProxyService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(">>>", " ProxyService --- onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String className = intent.getStringExtra(Constant.CLASS_NAME);
        Log.d(">>>", " ProxyService --- onStartCommand " + className);
        try {
            Class<?> aClass = PluginManager.getInstance(this).getClassLoader().loadClass(className);
            Object instance = aClass.newInstance();
            ServiceInterface serviceInterface = (ServiceInterface) instance;
            // 注入环境
            serviceInterface.insertServiceContext(this);
            serviceInterface.onStartCommand(intent, flags, startId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
