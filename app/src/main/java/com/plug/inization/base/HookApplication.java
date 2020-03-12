package com.plug.inization.base;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.plug.inization.HooKProxyActivity;
import com.plug.inization.HookTestActivity;
import com.plug.inization.PermissionActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用hook技术 动态代理 跳转没有注册的类
 * Created by A35 on 2020/3/12
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class HookApplication extends Application {
    public final int TARGET_SDK = 26;
    public static final int LAUNCH_ACTIVITY = 100;
    public static final int EXECUTE_TRANSACTION = 159;
    public static ArrayList<String> classArrayList = new ArrayList<>();

    static {
        classArrayList.add(HookTestActivity.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (Build.VERSION.SDK_INT >= TARGET_SDK) {
                hookAmsTarget26Action();
            } else {
                hookAmsAction();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(">>>", "hookAmsAction 失败 e:" + e.toString());
        }

        try {
            if (Build.VERSION.SDK_INT >= TARGET_SDK) {
                hookLuanchTarget26Activity();
            } else {
                hookLuanchActivity();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(">>>", "hookLuanchActivity 失败 e:" + e.toString());
        }
    }

    private void hookAmsAction() throws Exception {
        // 在启动之前 Hook ActivityManager.getService().startActivity()
       /* int result = ActivityManagerNative.getDefault()
    .startActivity(whoThread, who.getBasePackageName(), intent,
            intent.resolveTypeIfNeeded(who.getContentResolver()),
            token, target != null ? target.mEmbeddedID : null,
            requestCode, 0, null, options);
    IActivityManager  == ActivityManagerNative.getDefault()
          IActivityManager.startActivity()
          IActivityManager 类
          public int startActivity(IApplicationThread caller, String callingPackage, Intent intent,String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags,  ProfilerInfo profilerInfo, Bundle options)

   Hook  mInstance

   public abstract class Singleton<T> {
    private T mInstance;

    protected abstract T create();

        public final T get() {
            synchronized (this) {
                if (mInstance == null) {
                    mInstance = create();
                }
                return mInstance;
            }
        }
    }
    */

        // 需要代理的接口
        Class<?> activityManagerClass = Class.forName("android.app.IActivityManager");
        Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
        // 反射获取 IActivityManager
        Method activityManagerDefault = activityManagerNativeClass.getDeclaredMethod("getDefault");
        final Object IActivityManager = activityManagerDefault.invoke(null);
        // 反射获取 @1 Singleton
        Field gDefaultField = activityManagerNativeClass.getDeclaredField("gDefault");
        gDefaultField.setAccessible(true); // 授权
        Object gDefault = gDefaultField.get(null);

        // IActivityManager @2
        Object proxyInstance = Proxy.newProxyInstance(getClassLoader(),// 类加载器
                new Class[]{activityManagerClass},// 需要代理的接口
                new InvocationHandler() {   // 接口回调
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Log.d(">>> ", "Proxy");
                        if (TextUtils.equals("startActivity", method.getName())) {
                            Intent intent = (Intent) args[2];
                            String className = intent.getComponent().getClassName();
//                            if (classArrayList.contains(className)) {
                            // 做自己的业务逻辑 换成 可以 通过 AMS检查的 ProxyActivity
                            Intent proxyIntent = new Intent(HookApplication.this, HooKProxyActivity.class);
                            proxyIntent.putExtra("actionIntent", intent);
                            args[2] = proxyIntent;
//                            }
                        }
                        return method.invoke(IActivityManager, args);
                    }
                });
        // 反射获取Singleton 改变成员变量mInstance
        Class<?> singletonClass = Class.forName("android.util.Singleton");
        Field mInstance = singletonClass.getDeclaredField("mInstance");
        mInstance.setAccessible(true);
        // 获取要反射的对象@1 ，变量值 @2
        mInstance.set(gDefault, proxyInstance);
    }


    /**
     * Hook适配 Android9.0
     * 26以上是另一种写法
     * 要在执行 AMS之前，替换可用的 Activity，替换在AndroidManifest里面配置的Activity
     */
    private void hookAmsTarget26Action() throws Exception {
        // 在启动之前 Hook ActivityManager.getService().startActivity()
       /*
       ActivityManager.getService() 返回 IActivityManager类
        int result = ActivityManager.getService()
                .startActivity(whoThread, who.getBasePackageName(), intent,
                        intent.resolveTypeIfNeeded(who.getContentResolver()),
                        token, target != null ? target.mEmbeddedID : null,
                        requestCode, 0, null, options);

          public int startActivity(IApplicationThread caller, String callingPackage,
          Intent intent,String resolvedType, IBinder resultTo, String resultWho, int requestCode,
          int flags,  ProfilerInfo profilerInfo, Bundle options)

       public abstract class Singleton<T> {
        private T mInstance;

        protected abstract T create();

            public final T get() {
                synchronized (this) {
                    if (mInstance == null) {
                        mInstance = create();
                    }
                    return mInstance;
                }
            }
        }
    */
        Class<?> iactivityManagerClass = Class.forName("android.app.IActivityManager");
        // 获取IActivityManager类
        Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
        Method getService = activityManagerClass.getDeclaredMethod("getService");
        final Object getServiceManager = getService.invoke(null);
        Field singletonField = activityManagerClass.getDeclaredField("IActivityManagerSingleton");
        singletonField.setAccessible(true);
        // 对象@1
        Object singletonObject = singletonField.get(null);
        // IActivityManager 替换成 动态代理  @2
        Object proxyInstance = Proxy.newProxyInstance(getClassLoader(),
                new Class[]{iactivityManagerClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Log.d(">>>", " Proxy hookAmsTarget26Action ");
                        if (TextUtils.equals("startActivity", method.getName())) {
                            Intent intent = (Intent) args[2];
                            String className = intent.getComponent().getClassName();
//                            if (classArrayList.contains(className)) {
                            // 做自己的业务逻辑 换成 可以 通过 AMS检查的 ProxyActivity
                            Intent proxyIntent = new Intent(HookApplication.this, HooKProxyActivity.class);
                            proxyIntent.putExtra("actionIntent", intent);
                            args[2] = proxyIntent;
//                            }
                        }
                        return method.invoke(getServiceManager, args);
                    }
                });
        // 反射获取Singleton 改变成员变量mInstance
        Class<?> singletonClass = Class.forName("android.util.Singleton");
        Field mInstance = singletonClass.getDeclaredField("mInstance");
        mInstance.setAccessible(true);
        // 获取要反射的对象@1 ，变量值 @2
        mInstance.set(singletonObject, proxyInstance);
    }

    /**
     * Hook适配 Android9.0
     * 26以上是另一种写法
     * AMS检查过后，要把这个HooKProxyActivity 换回来HookTestActivity
     */
    private void hookLuanchTarget26Activity() throws Exception {
        /*
          启动Activity
          case EXECUTE_TRANSACTION:
                    final ClientTransaction transaction = (ClientTransaction) msg.obj;
                    mTransactionExecutor.execute(transaction);
                    if (isSystem()) {
                        // Client transactions inside system process are recycled on the client side
                        // instead of ClientLifecycleManager to avoid being cleared before this
                        // message is handled.
                        transaction.recycle();
                    }

                    break;

           final H mH = new H();

            public static ActivityThread currentActivityThread() {
                return sCurrentActivityThread;
            }
        * */

        // 获取 ActivityThread
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        // 实例化 ActivityThread
        Object activityThread = activityThreadClass.getDeclaredMethod("currentActivityThread").invoke(null);
        // 获取成员变量 Handler
        Field mH = activityThreadClass.getDeclaredField("mH");
        mH.setAccessible(true);
        Handler handler = (Handler) mH.get(activityThread);
        // 获取 Handler.Callback 重写Callback
        Field mCallback = Handler.class.getDeclaredField("mCallback");
        mCallback.setAccessible(true);
        mCallback.set(handler,new MyCallback26(handler));
    }

    public class MyCallback26 implements Handler.Callback {
        private Handler handler;

        public MyCallback26(Handler handler) {
            this.handler = handler;
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case EXECUTE_TRANSACTION:{
                    Object obj = msg.obj;
                    Log.d(">>>", " LAUNCH_ACTIVITY ");
                   /*
                      final ClientTransaction transaction = (ClientTransaction) msg.obj;
                    mTransactionExecutor.execute(transaction);
                    if (isSystem()) {
                        // Client transactions inside system process are recycled on the client side
                        // instead of ClientLifecycleManager to avoid being cleared before this
                        // message is handled.
                        transaction.recycle();
                    }
                    1.获取 ClientTransaction 类 成员变量 mActivityCallbacks
                    private List<ClientTransactionItem> mActivityCallbacks;

                    2.从成员变量mActivityCallbacks获取Intent 父类找不到去子类找 LaunchActivityItem 继承 ClientTransactionItem
                    public class LaunchActivityItem extends ClientTransactionItem {
                        private Intent mIntent;
                    }
                    3.从LaunchActivityItem 获取Intent,改变值
                    }*/
                    try {
                        // 获取成员变量 List<ClientTransactionItem>
                        Field transactionField = obj.getClass().getDeclaredField("mActivityCallbacks");
                        transactionField.setAccessible(true);
                        List list = (List) transactionField.get(obj);
                        if (list != null && list.size() > 0) {
                            Object item = list.get(0);
                            // 获取 LaunchActivityItem
                            Class<?> launchActivityClass = Class.forName("android.app.servertransaction.LaunchActivityItem");
                            Field mIntentField = launchActivityClass.getDeclaredField("mIntent");
                            mIntentField.setAccessible(true);
                            Intent intentValue = (Intent) mIntentField.get(item);
                            // 获取 intent 对象，才能取出携带过来的 actionIntent
                            Intent actionIntent = intentValue.getParcelableExtra("actionIntent");
                            if (actionIntent != null) {
                                if (classArrayList.contains(actionIntent.getComponent().getClassName())) {
                                    mIntentField.set(item, actionIntent);// 把 HooKProxyActivity 换成  HookTestActivity
                                }
                                // TODO: 2020/3/13 测试用
//                                else {
//                                    // 没有权限 跳转权限类
//                                    mIntentField.set(item, new Intent(HookApplication.this, PermissionActivity.class));// 把 HooKProxyActivity 换成  HookTestActivity
//                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            // 让系统去处理原有的方法
            // return false  系统就会往下执行
            // return true 系统不会往下执行
            handler.handleMessage(msg);
            return true;
        }
    }

    /**
     * 26以上是另一种写法
     * AMS检查过后，要把这个HooKProxyActivity 换回来HookTestActivity
     */
    private void hookLuanchActivity() throws Exception {
        // AMS检查过后，要把这个HooKProxyActivity 换回来 --> HookTestActivity
        // ActivityThread 获取变量  H mH = new H()

          /* handler对象怎么来
            1.寻找H，先寻找ActivityThread

            执行此方法 public static ActivityThread currentActivityThread()

            通过ActivityThread 找到 H

            ActivityThread 类
            public static ActivityThread currentActivityThread() {
                return sCurrentActivityThread;
            }
            public void dispatchMessage(Message msg) {
                if (msg.callback != null) {
                    handleCallback(msg);
                } else {
                    if (mCallback != null) {
                    // 默认不走
                        if (mCallback.handleMessage(msg)) {
                            return;
                        }
                    }
                    handleMessage(msg);
                }
        }*/
        // 获取ActivityThread
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        // 获得ActivityThrea对象
        Object mActivityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field mInstance = activityThreadClass.getDeclaredField("mH");
        mInstance.setAccessible(true);
        // 获取Handler 对象
        Handler mH = (Handler) mInstance.get(mActivityThread);
        // 获取Handler.Callback 对象
        Field mCallbackFiled = Handler.class.getDeclaredField("mCallback");
        mCallbackFiled.setAccessible(true);
        // 替换 增加我们自己的实现代码
        mCallbackFiled.set(mH, new MyCallback(mH));
    }

    public class MyCallback implements Handler.Callback {
        private Handler handler;

        public MyCallback(Handler handler) {
            this.handler = handler;
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case LAUNCH_ACTIVITY: {
                    Object obj = msg.obj;
                    Log.d(">>>", " LAUNCH_ACTIVITY ");
                   /* obj中获取Intent
                    ActivityClientRecord r = (ActivityClientRecord) msg.obj;
                    static final class ActivityClientRecord {
                        IBinder token;
                        int ident;
                        Intent intent;
                    }*/
                    try {
                        // 获取成员变量intent
                        Field intent = obj.getClass().getDeclaredField("intent");
                        intent.setAccessible(true);
                        Intent intentValue = (Intent) intent.get(obj);
                        // 获取 intent 对象，才能取出携带过来的 actionIntent
                        Intent actionIntent = intentValue.getParcelableExtra("actionIntent");
                        if (actionIntent != null) {
                            if (classArrayList.contains(actionIntent.getComponent().getClassName())) {
                                intent.set(obj, actionIntent);// 把 HooKProxyActivity 换成  HookTestActivity
                            }
                            // TODO: 2020/3/13 测试用
//                            else {
//                                // 没有权限 跳转权限类
//                                intent.set(obj, new Intent(HookApplication.this, PermissionActivity.class));// 把 HooKProxyActivity 换成  HookTestActivity
//                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            // 让系统去处理原有的方法
            // return false  系统就会往下执行
            // return true 系统不会往下执行
            handler.handleMessage(msg);
            return true;
        }
    }


}
