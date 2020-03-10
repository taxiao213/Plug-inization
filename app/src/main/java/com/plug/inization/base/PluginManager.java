package com.plug.inization.base;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.nfc.Tag;
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

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
        return new File(Environment.getExternalStorageDirectory() + File.separator + "plug.apk");
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
}
