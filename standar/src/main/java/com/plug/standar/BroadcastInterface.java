package com.plug.standar;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Activity标准
 * Created by A35 on 2020/3/10
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public interface BroadcastInterface {
    /**
     * 把宿主(app)的环境  给  插件
     *
     * @param broadcastReceiver
     */
    void insertBroadcastContext(BroadcastReceiver broadcastReceiver);

    void onReceive(Context context, Intent intent);

}
