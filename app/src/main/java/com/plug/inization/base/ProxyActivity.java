package com.plug.inization.base;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.plug.standar.ActivityInterface;
import com.plug.standar.Constant;

import java.lang.reflect.Constructor;

/**
 * 代理的Activity 代理/占位 插件里面的Activity
 * Created by A35 on 2020/3/10
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class ProxyActivity extends AppCompatActivity {

    @Override
    public Resources getResources() {
        return PluginManager.getInstance(this).getResources();
    }

    @Override
    public ClassLoader getClassLoader() {
        return PluginManager.getInstance(this).getClassLoader();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(">>>", " ProxyActivity --- onCreate");
        // 真正的加载 插件里面的 Activity
        String className = getIntent().getStringExtra(Constant.CLASS_NAME);
        try {
            Class mPluginActivityClass = getClassLoader().loadClass(className);
            // 实例化 插件包里面的 Activity
//            Constructor constructor = mPluginActivityClass.getConstructor(new Class[]{});
//            Object mPluginActivity = constructor.newInstance(new Object[]{});
//            ActivityInterface activityInterface = (ActivityInterface) mPluginActivity;

            ActivityInterface activityInterface = (ActivityInterface) mPluginActivityClass.newInstance();
            // 注入环境
            activityInterface.insertAppContext(this);
            Bundle bundle = new Bundle();
            bundle.putString("appName", "我是宿主传递过来的信息");
            // 执行插件里面的onCreate方法
            activityInterface.onCreate(bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startActivity(Intent intent) {
        Log.d(">>>", " ProxyActivity --- startActivity");
        // 插件com.plug.plug_package.PlugMainActivity 跳转com.plug.plug_package.TestActivity 到这
        String className = intent.getStringExtra(Constant.CLASS_NAME);// 获取类名
        Intent intentProxy = new Intent(this, ProxyActivity.class);
        intentProxy.putExtra(Constant.CLASS_NAME, className);
        // 要给TestActivity 进栈 调用父类的startActivity
        super.startActivity(intentProxy);
    }

    @Override
    public ComponentName startService(Intent service) {
        String className = service.getStringExtra(Constant.CLASS_NAME);// 获取类名
        Intent intentProxy = new Intent(this, ProxyService.class);
        intentProxy.putExtra(Constant.CLASS_NAME, className);
        // 要给TestService 进栈 调用父类的startService
        Log.d(">>>", " ProxyActivity --- startService " + className);
        return super.startService(intentProxy);
    }
}
