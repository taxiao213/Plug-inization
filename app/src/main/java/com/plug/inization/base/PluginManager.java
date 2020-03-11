package com.plug.inization.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.nfc.Tag;
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;

import com.plug.standar.BroadcastInterface;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AlgorithmConstraints;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;

/**
 * Created by A35 on 2020/3/10
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class PluginManager {
    private static final String TAG = PluginManager.class.getSimpleName();
    private static PluginManager pluginManager;
    private Context context;
    private final static String ADD_ASSET_PATH = "addAssetPath"; // 添加资源的方法名称 AssetManager 356行
    private DexClassLoader dexClassLoader;
    private Resources resourcesNew;

    public static PluginManager getInstance(Context context) {
        if (pluginManager == null) {
            synchronized (PluginManager.class) {
                if (pluginManager == null) {
                    pluginManager = new PluginManager(context);
                }
            }
        }
        return pluginManager;
    }

    public PluginManager(Context context) {
        this.context = context;
    }

    /**
     * 加载插件
     */
    public void loadPlugin() {
        File file = getPlugFile();
        if (!file.exists()) {
            Log.d(TAG, "插件包不存在");
            return;
        }
        String path = file.getAbsolutePath();

        // dexClassLoader需要一个缓存目录   /data/data/当前应用的包名/pDir
        File fileDir = context.getDir("pDir", Context.MODE_PRIVATE);
        // Activity class
        dexClassLoader = new DexClassLoader(path, fileDir.getAbsolutePath(), null, context.getClassLoader());
        // 加载插件里面的layout
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method method = assetManager.getClass().getMethod(ADD_ASSET_PATH, String.class);
            method.invoke(assetManager, path);// 插件包的路径   pluginPaht
            Resources resources = context.getResources();// 宿主资源
            // 加载插件里面的资源的 Resources
            resourcesNew = new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取插件包
     *
     * @return
     */
    public File getPlugFile() {
        return new File(Environment.getExternalStorageDirectory() + File.separator + "plug_package-debug.apk");
    }

    /**
     * 获取plug ClassLoader
     *
     * @return
     */
    public ClassLoader getClassLoader() {
        return dexClassLoader;
    }

    /**
     * 获取plug Resources
     *
     * @return
     */
    public Resources getResources() {
        return resourcesNew;
    }

    /**
     * 注册静态服务
     */
    public void parserApk() {
//        分析系统源码PackageManagerService
//        public Package parsePackage(File packageFile, int flags) throws PackageParserException {
//            return parsePackage(packageFile, flags, false /* useCaches */);
//        }
        try {
            // 插件包安装路径
            File file = getPlugFile();
            // 反射获取android.content.pm.PackageParser
            Class<?> packageParser = Class.forName("android.content.pm.PackageParser");
            Object instance = packageParser.newInstance();
            // 反射去获取public Package parsePackage(File packageFile, int flags)
            Method method = packageParser.getMethod("parsePackage", File.class, int.class);
            // 获取返回的值
            Object aPackage = method.invoke(instance, file, PackageManager.GET_ACTIVITIES);
            // public final ArrayList<Activity> receivers = new ArrayList<Activity>(0);
            Field field = aPackage.getClass().getField("receivers");
            // ArrayList<Activity> receivers 获取 AndroidManifest.xml receiver 集合
            ArrayList receiversList = (ArrayList) field.get(aPackage);
            // PackageParser 获取 7057行 -- public static abstract class Component<II extends IntentInfo>
            // 第一种写法 获取静态类 用$符号
//            Class<?> componentClass = Class.forName("android.content.pm.PackageParser$Component");
//            Field intents = componentClass.getField("intents");

            for (Object mActivity : receiversList) {
                // 第二种写法 获取  public final ArrayList<II> intents;
                Field intents = mActivity.getClass().getField("intents");
                // 得到参数 IntentFilter集合
                ArrayList<IntentFilter> intentList = (ArrayList) intents.get(mActivity);
                // 我们还有一个任务，就是要拿到 android:name=".StaticReceiver"
                // activityInfo.name; == android:name=".StaticReceiver"
                // 分析源码 如何 拿到
                // ActivityInfo public static final ActivityInfo generateActivityInfo(ActivityInfo ai, int flags,PackageUserState state, int userId)
                Class<?> packageUserState = Class.forName("android.content.pm.PackageUserState");
                Class mUserHandle = Class.forName("android.os.UserHandle");
                // UserHandle类
                // public static @UserIdInt int getCallingUserId() {
                //     return getUserId(Binder.getCallingUid());
                // }
                // 静态方法可以不用写object
                // int userId = (int) mUserHandle.getMethod("getCallingUserId").invoke(mUserHandle);
                int userId = (int) mUserHandle.getMethod("getCallingUserId").invoke(null);
                // public static final ActivityInfo generateActivityInfo(Activity a, int flags,PackageUserState state, int userId)
                Method generateActivityInfo = packageParser.getDeclaredMethod("generateActivityInfo", mActivity.getClass(), int.class, packageUserState, int.class);
                ActivityInfo info = (ActivityInfo) generateActivityInfo.invoke(instance, mActivity, 0, packageUserState.newInstance(), userId);

                // 获取类名初始化 类名信息在ActivityInfo中，
                Class<?> aClass = getClassLoader().loadClass(info.name);
                BroadcastReceiver broadcastReceiver = (BroadcastReceiver) aClass.newInstance();
                for (IntentFilter intent : intentList) {
                    context.registerReceiver(broadcastReceiver, intent);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
