package com.plug.standar;

import android.app.Activity;
import android.os.Bundle;

/**
 * 标准
 * Created by A35 on 2020/3/10
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public interface ActivityInterface {
    /**
     * 把宿主(app)的环境  给  插件
     * @param appActivity
     */
    void insertAppContext(Activity appActivity);

    void onCreate(Bundle savedInstanceState);

    void onResume();

    void onStart();

    void onDestroy();

}
