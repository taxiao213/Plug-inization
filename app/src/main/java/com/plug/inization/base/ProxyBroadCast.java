package com.plug.inization.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.plug.standar.BroadcastInterface;
import com.plug.standar.Constant;

/**
 * 代理的BroadCast 代理/占位 插件里面的BroadCast
 * Created by A35 on 2020/3/11
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class ProxyBroadCast extends BroadcastReceiver {

    private String mClassName;

    public ProxyBroadCast(String className) {
        this.mClassName = className;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(">>>", " ProxyBroadCast --- onReceive " + mClassName);
        String action = intent.getAction();
        if (TextUtils.equals(action, Constant.BROADCAST_ACTION)) {
            try {
                Class<?> aClass = PluginManager.getInstance(context).getClassLoader().loadClass(mClassName);
                BroadcastInterface instance = (BroadcastInterface) aClass.newInstance();
                instance.insertBroadcastContext(this);
                instance.onReceive(context, intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
