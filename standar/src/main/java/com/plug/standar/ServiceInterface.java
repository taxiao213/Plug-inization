package com.plug.standar;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Service标准
 * Created by A35 on 2020/3/10
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public interface ServiceInterface {
    /**
     * 把宿主(app)的环境  给  插件
     *
     * @param service
     */
    void insertServiceContext(Service service);

    void onCreate(Bundle savedInstanceState);

    int onStartCommand(Intent intent, int flags, int startId);

    IBinder onBind(Intent intent);

    ComponentName startService(Intent service);

}
