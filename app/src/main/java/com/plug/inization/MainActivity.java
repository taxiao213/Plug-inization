package com.plug.inization;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.plug.inization.base.PluginManager;
import com.plug.inization.base.ProxyActivity;
import com.plug.inization.base.ProxyService;
import com.plug.standar.Constant;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MainActivity extends AppCompatActivity {

    private Button hook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View load = findViewById(R.id.load);
        View plug = findViewById(R.id.plug);
        View loadService = findViewById(R.id.load_service);
        View registerReceiver = findViewById(R.id.register_receiver);
        View sendReceiver = findViewById(R.id.send_receiver);
        hook = findViewById(R.id.hook);
        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPlug();
            }
        });

        plug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlugActivity();
            }
        });

        loadService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadService();
            }
        });

        registerReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerReceiver();
            }
        });

        sendReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendReceiver();
            }
        });

        // hook 动态代理
        hook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, ((Button)v).getText(), Toast.LENGTH_SHORT).show();
            }
        });

        try {
            hook();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPlug() {
        PluginManager.getInstance(this).loadPlugin();
    }

    // 跳转插件Activity
    public void startPlugActivity() {
        // 获取插件APK的所有Activity
        String packagePath = PluginManager.getInstance(this).getPlugFile().getAbsolutePath();
        PackageManager packageManager = getPackageManager();
        PackageInfo info = packageManager.getPackageArchiveInfo(packagePath, PackageManager.GET_ACTIVITIES);
        // 获取AndroidManifest.xml 按顺序
        ActivityInfo activity = info.activities[0];
        // 占位 代理Activity
        Intent intent = new Intent(this, ProxyActivity.class);
        intent.putExtra(Constant.CLASS_NAME, activity.name);
        startActivity(intent);
    }

    // 加载Service
    private void loadService() {
        startService(new Intent(this, ProxyService.class));
    }

    // 注册静态服务
    private void registerReceiver() {
        PluginManager.getInstance(this).parserApk();
    }

    // 发送广播
    private void sendReceiver() {
        Intent intent = new Intent();
        intent.setAction(Constant.BROADCAST_ACTION);
        sendBroadcast(intent);
    }

    /**
     * 动态代理 改变 view 提示语
     */
    private void hook() throws Exception {
        // 通过反射去获取
        Class<?> viewClass = Class.forName("android.view.View");
        // 获取ListenerInfo对象 1.先找到ListenerInfo
        Method declaredMethod = viewClass.getDeclaredMethod("getListenerInfo");
        declaredMethod.setAccessible(true);
        Object invoke = declaredMethod.invoke(hook);
        // 2.再通过ListenerInfo.mOnClickListener赋值 替换
        // 获取内部静态类 用$符号
        Class<?> listenerInfoClass = Class.forName("android.view.View$ListenerInfo");
        Field mOnClickListener = listenerInfoClass.getField("mOnClickListener");
        final Object listener = mOnClickListener.get(invoke);

        // 2参数 需要监听的class接口，监听什么接口，就返回什么接口
        // 3参数 监听接口方法里面的回调
        Object proxyInstance = Proxy.newProxyInstance(getClassLoader(), new Class[]{View.OnClickListener.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Button button = new Button(MainActivity.this);
                button.setText("hook haha");
                return method.invoke(listener, button);
            }
        });
        // 替换getListenerInfo().mOnClickListener
        // 1.先找到ListenerInfo
        // 2.再通过ListenerInfo.mOnClickListener赋值 替换
        /*
        * public void setOnClickListener(@Nullable OnClickListener l) {
             if (!isClickable()) {
             setClickable(true);
             }
             getListenerInfo().mOnClickListener = l;
           }

           ListenerInfo getListenerInfo() {
                if (mListenerInfo != null) {
                    return mListenerInfo;
                }
                mListenerInfo = new ListenerInfo();
                return mListenerInfo;
             }

          static class ListenerInfo {
            public View.OnClickListener mOnClickListener;
          }
        * */
        // 把系统的 mOnClickListener  换成 我们自己写的 动态代理
        mOnClickListener.set(invoke, proxyInstance);
    }
}
