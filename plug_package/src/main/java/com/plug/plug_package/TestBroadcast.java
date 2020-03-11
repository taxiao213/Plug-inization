package com.plug.plug_package;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.plug.plug_package.base.BaseBroadcast;

/**
 * Created by A35 on 2020/3/11
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class TestBroadcast extends BaseBroadcast {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(">>>", " TestBroadcast --- onReceive");
        super.onReceive(context, intent);
    }
}
