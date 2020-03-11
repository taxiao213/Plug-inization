package com.plug.plug_package.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.plug.standar.ActivityInterface;
import com.plug.standar.Constant;

import java.util.Objects;

/**
 * 插件BaseActivity
 * Created by A35 on 2020/3/10
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class BaseActivity extends AppCompatActivity implements ActivityInterface {

    protected Activity mActivity;// 宿主的环境

    /**
     * 设置布局
     *
     * @param layoutId
     */
    public void setContentView(int layoutId) {
        if (mActivity != null) {
            mActivity.setContentView(layoutId);
        }
    }

    /**
     * 找到对应的View
     *
     * @param viewId
     * @return
     */
    public View findViewByID(int viewId) {
        if (mActivity != null) {
            return mActivity.findViewById(viewId);
        }
        return null;
    }

    // 注入宿主 Activity
    @Override
    public void insertAppContext(Activity appActivity) {
        this.mActivity = appActivity;
    }

    /**
     * 插件内Activity跳转
     *
     * @param intent
     */
    @Override
    public void startActivity(Intent intent) {
        Intent intentNew = new Intent();
        intentNew.putExtra(Constant.CLASS_NAME, Objects.requireNonNull(intent.getComponent()).getClassName());
        // 调用 com.plug.inization.base.ProxyActivity 宿主的startActivity()
        mActivity.startActivity(intentNew);
    }

    // 宿主的Service开启startService
    @Override
    public ComponentName startService(Intent service) {
        // 传入新生成的Intent
        Intent intentNew = new Intent();
        intentNew.putExtra(Constant.CLASS_NAME, Objects.requireNonNull(service.getComponent()).getClassName());
        // 调用 com.plug.inization.base.ProxyActivity 宿主的startService()
        return mActivity.startService(intentNew);
    }

    /**
     * 发送广播
     * @param intent
     */
    public void sendBroadcast(Intent intent) {
        mActivity.sendBroadcast(intent);
    }

    // 注册服务
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return mActivity.registerReceiver(receiver, filter);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onCreate(Bundle savedInstanceState) {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onResume() {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onStart() {

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onDestroy() {

    }
}
