package com.plug.inization.base;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.plug.inization.HooKProxyActivity;
import com.plug.inization.HookTestActivity;
import com.plug.inization.PermissionActivity;
import com.plug.standar.Constant;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

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
    private final static String ADD_ASSET_PATH = "addAssetPath"; // 添加资源的方法名称 AssetManager 356行
    public static ArrayList<String> classArrayList = new ArrayList<>();

    static {
        classArrayList.add(HookTestActivity.class.getName());
        classArrayList.add("com.plug.plug_package.TestActivity2");
    }

    private Resources plugResources;
    private AssetManager assetManager;

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

//        try {
//            // 将宿主和插件包的ClassLoader融合在一块
//            hookFusionClassLoader();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.d(">>>", "hookFusionClassLoader 失败 e:" + e.toString());
//        }

        try {
            // LoadApk式
            if (Build.VERSION.SDK_INT >= TARGET_SDK) {
                custom26ClassLoader();
            } else {
                customClassLoader();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(">>>", "customClassLoader 失败 e:" + e.toString());
        }
    }

    /**
     * 适配26以上 获取LoadApk
     * todo 没有适配成功
     */
    private void custom26ClassLoader() throws Exception {
        // 获取 ActivityThread
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        // 实例化 ActivityThread
        Object activityThread = activityThreadClass.getDeclaredMethod("currentActivityThread").invoke(null);
        // mPackages 添加 自定义的LoadedApk   获取成员变量 final ArrayMap<String, WeakReference<LoadedApk>> mPackages = new ArrayMap<String, WeakReference<LoadedApk>>();
        Field mPackagesField = activityThreadClass.getDeclaredField("mPackages");
        mPackagesField.setAccessible(true);
        // 拿到mPackages对象
        Object mPackages = mPackagesField.get(activityThread);
        Map mPackagesMap = (Map) mPackages;

        // 获取 CompatibilityInfo 返回一个默认的CompatibilityInfo
        // /** default compatibility info object for compatible applications */
        //    public static final CompatibilityInfo DEFAULT_COMPATIBILITY_INFO = new CompatibilityInfo() {
        //    };
        Class<?> mCompatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
        Field defaultCompatibilityInfoField = mCompatibilityInfoClass.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
        defaultCompatibilityInfoField.setAccessible(true);
        Object mCompatibilityInfo = defaultCompatibilityInfoField.get(null);

        // ApplicationInfo  获取
        ApplicationInfo applicationInfo = getApplicationInfoAction26();

        // 返回一个LoadApk 没有就去创建 r.packageInfo = getPackageInfoNoCheck( r.activityInfo.applicationInfo, r.compatInfo);
        Method getPackageInfoNoCheckMethod = activityThreadClass.getDeclaredMethod("getPackageInfoNoCheck", ApplicationInfo.class, mCompatibilityInfoClass);
        getPackageInfoNoCheckMethod.setAccessible(true);
        Object mLoadApk = getPackageInfoNoCheckMethod.invoke(activityThread, applicationInfo, mCompatibilityInfo);

        // 自定义加载器 加载插件
        // String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent
        File plugCustomFile = getDir("plug_custom", Context.MODE_PRIVATE);
        DexClassLoader dexClassLoader = new DexClassLoader(PluginManager.getInstance(this).getPlugFile().getAbsolutePath(), plugCustomFile.getAbsolutePath(), null, getClassLoader());
        // 替换LoadedApk 中的 private ClassLoader mClassLoader;
        Field mClassLoaderField = mLoadApk.getClass().getDeclaredField("mClassLoader");
        mClassLoaderField.setAccessible(true);
        mClassLoaderField.set(mLoadApk, dexClassLoader);

        // mPackages.put(aInfo.packageName, new WeakReference<LoadedApk>(packageInfo));
        // 最终的目标 mPackages.put(插件的包名，插件的LoadedApk);
        WeakReference weakReference = new WeakReference(mLoadApk);
        // 将自定义的LoadApk put进去
        mPackagesMap.put(applicationInfo.packageName, weakReference);
    }

    /**
     * 获取26以上 ApplicationInfo
     * todo 没有适配成功
     */
    private ApplicationInfo getApplicationInfoAction26() throws Exception {
        //        分析系统源码PackageManagerService
//        public Package parsePackage(File packageFile, int flags) throws PackageParserException {
//            return parsePackage(packageFile, flags, false /* useCaches */);
//        }

        // 插件包安装路径
        File file = PluginManager.getInstance(this).getPlugFile();
        if (!file.exists()) {
            throw new FileNotFoundException("插件包未找到");
        }
        // 反射获取android.content.pm.PackageParser
        Class<?> packageParser = Class.forName("android.content.pm.PackageParser");
        Class<?> mPackageUserStateClass = Class.forName("android.content.pm.PackageUserState");
        Class<?> packageClass = Class.forName("android.content.pm.PackageParser$Package");
        Object packageParserObject = packageParser.newInstance();
        // 返回一个默认的 ApplicationInfo
        //        public static ApplicationInfo generateApplicationInfo(Package p, int flags,
        //        PackageUserState state) {
        //            return generateApplicationInfo(p, flags, state, UserHandle.getCallingUserId());
        //        }
        Method generateApplicationInfoMethod = packageParser.getDeclaredMethod("generateApplicationInfo", packageClass, int.class, mPackageUserStateClass);
        generateApplicationInfoMethod.setAccessible(true);

        // 获取Package ，PackageParser类
        // public Package parsePackage(File packageFile, int flags) throws PackageParserException {
        //        if (packageFile.isDirectory()) {
        //            return parseClusterPackage(packageFile, flags);
        //        } else {
        //            return parseMonolithicPackage(packageFile, flags);
        //        }
        //    }
        Method parsePackageMethod = packageParser.getDeclaredMethod("parsePackage", File.class, int.class);
        parsePackageMethod.setAccessible(true);
        Object mPackage = parsePackageMethod.invoke(packageParserObject, file, PackageManager.GET_ACTIVITIES);
        // 需要参数Package， int， PackageUserState
        ApplicationInfo mApplication = (ApplicationInfo) generateApplicationInfoMethod.invoke(packageParserObject, mPackage, 0, mPackageUserStateClass.newInstance());
        // 获得的 ApplicationInfo 就是插件的 ApplicationInfo
        // 我们这里获取的 ApplicationInfo
        // applicationInfo.publicSourceDir = 插件的路径；
        // applicationInfo.sourceDir = 插件的路径；
        String path = file.getAbsolutePath();
        mApplication.publicSourceDir = path;
        mApplication.sourceDir = path;
        return mApplication;
    }

    /**
     * LoadedApk 自定义
     */
    private void customClassLoader() throws Exception {
        // 获取 ActivityThread
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        // 实例化 ActivityThread
        Object activityThread = activityThreadClass.getDeclaredMethod("currentActivityThread").invoke(null);
        // mPackages 添加 自定义的LoadedApk   获取成员变量 final ArrayMap<String, WeakReference<LoadedApk>> mPackages = new ArrayMap<String, WeakReference<LoadedApk>>();
        Field mPackagesField = activityThreadClass.getDeclaredField("mPackages");
        mPackagesField.setAccessible(true);
        // 拿到mPackages对象
        Object mPackages = mPackagesField.get(activityThread);
        Map mPackagesMap = (Map) mPackages;

        // 获取 CompatibilityInfo 返回一个默认的CompatibilityInfo
        // /** default compatibility info object for compatible applications */
        //    public static final CompatibilityInfo DEFAULT_COMPATIBILITY_INFO = new CompatibilityInfo() {
        //    };
        Class<?> mCompatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
        Field defaultCompatibilityInfoField = mCompatibilityInfoClass.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
        defaultCompatibilityInfoField.setAccessible(true);
        Object mCompatibilityInfo = defaultCompatibilityInfoField.get(null);

        // ApplicationInfo  获取
        ApplicationInfo applicationInfo = getApplicationInfoAction();

        // 返回一个LoadApk 没有就去创建 r.packageInfo = getPackageInfoNoCheck( r.activityInfo.applicationInfo, r.compatInfo);
        Method getPackageInfoNoCheckMethod = activityThreadClass.getDeclaredMethod("getPackageInfoNoCheck", ApplicationInfo.class, mCompatibilityInfoClass);
        getPackageInfoNoCheckMethod.setAccessible(true);
        Object mLoadApk = getPackageInfoNoCheckMethod.invoke(activityThread, applicationInfo, mCompatibilityInfo);

        // 自定义加载器 加载插件
        // String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent
        File plugCustomFile = getDir("plug_custom", Context.MODE_PRIVATE);
        DexClassLoader dexClassLoader = new DexClassLoader(PluginManager.getInstance(this).getPlugFile().getAbsolutePath(), plugCustomFile.getAbsolutePath(), null, getClassLoader());
        // 替换LoadedApk 中的 private ClassLoader mClassLoader;
        Field mClassLoaderField = mLoadApk.getClass().getDeclaredField("mClassLoader");
        mClassLoaderField.setAccessible(true);
        mClassLoaderField.set(mLoadApk, dexClassLoader);

        // mPackages.put(aInfo.packageName, new WeakReference<LoadedApk>(packageInfo));
        // 最终的目标 mPackages.put(插件的包名，插件的LoadedApk);
        WeakReference weakReference = new WeakReference(mLoadApk);
        // 将自定义的LoadApk put进去
        mPackagesMap.put(applicationInfo.packageName, weakReference);
    }

    private ApplicationInfo getApplicationInfoAction() throws Exception {
        //        分析系统源码PackageManagerService
//        public Package parsePackage(File packageFile, int flags) throws PackageParserException {
//            return parsePackage(packageFile, flags, false /* useCaches */);
//        }

        // 插件包安装路径
        File file = PluginManager.getInstance(this).getPlugFile();
        if (!file.exists()) {
            throw new FileNotFoundException("插件包未找到");
        }
        // 反射获取android.content.pm.PackageParser
        Class<?> packageParser = Class.forName("android.content.pm.PackageParser");
        Class<?> mPackageUserStateClass = Class.forName("android.content.pm.PackageUserState");
        Class<?> packageClass = Class.forName("android.content.pm.PackageParser$Package");
        Object packageParserObject = packageParser.newInstance();
        // 返回一个默认的 ApplicationInfo
        //        public static ApplicationInfo generateApplicationInfo(Package p, int flags,
        //        PackageUserState state) {
        //            return generateApplicationInfo(p, flags, state, UserHandle.getCallingUserId());
        //        }
        Method generateApplicationInfoMethod = packageParser.getDeclaredMethod("generateApplicationInfo", packageClass, int.class, mPackageUserStateClass);
        generateApplicationInfoMethod.setAccessible(true);

        // 获取Package ，PackageParser类
        // public Package parsePackage(File packageFile, int flags) throws PackageParserException {
        //        if (packageFile.isDirectory()) {
        //            return parseClusterPackage(packageFile, flags);
        //        } else {
        //            return parseMonolithicPackage(packageFile, flags);
        //        }
        //    }
        Method parsePackageMethod = packageParser.getDeclaredMethod("parsePackage", File.class, int.class);
        parsePackageMethod.setAccessible(true);
        Object mPackage = parsePackageMethod.invoke(packageParserObject, file, PackageManager.GET_ACTIVITIES);
        // 需要参数Package， int， PackageUserState
        ApplicationInfo mApplication = (ApplicationInfo) generateApplicationInfoMethod.invoke(packageParserObject, mPackage, 0, mPackageUserStateClass.newInstance());
        // 获得的 ApplicationInfo 就是插件的 ApplicationInfo
        // 我们这里获取的 ApplicationInfo
        // applicationInfo.publicSourceDir = 插件的路径；
        // applicationInfo.sourceDir = 插件的路径；
        String path = file.getAbsolutePath();
        mApplication.publicSourceDir = path;
        mApplication.sourceDir = path;
        return mApplication;
    }

    /**
     * 将宿主和插件包的ClassLoader融合在一块
     * SDK 21-28 都兼容
     */
    private void hookFusionClassLoader() throws Exception {
        // 第一步：找到宿主 dexElements 得到此对象     PathClassLoader代表是宿主
        // 本质就是PathClassLoader
        PathClassLoader hostPathClassLoader = (PathClassLoader) this.getClassLoader();
        // BaseDexClassLoader.java  找到private final DexPathList pathList; Class c = pathList.findClass(name, suppressedExceptions);
        Class<?> hostBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        // private final DexPathList pathList;
        Field hostBaseDexClassLoaderField = hostBaseDexClassLoaderClass.getDeclaredField("pathList");
        hostBaseDexClassLoaderField.setAccessible(true);
        Object hostPathList = hostBaseDexClassLoaderField.get(hostPathClassLoader);
        // private Element[] dexElements;
        Field hostDexElementsField = hostPathList.getClass().getDeclaredField("dexElements");
        hostDexElementsField.setAccessible(true);
        Object hostDexElements = hostDexElementsField.get(hostPathList);

        // 第二步：找到插件 dexElements 得到此对象，代表插件 DexClassLoader--代表插件
        File plugFile = getDir("plug", Context.MODE_PRIVATE);
        File plugFileAPK = PluginManager.getInstance(this).getPlugFile();
        if (!plugFileAPK.exists()) {
            throw new FileNotFoundException("插件包找不到");
        }
        DexClassLoader plugPathClassLoader = new DexClassLoader(plugFileAPK.getAbsolutePath(), plugFile.getAbsolutePath(), null, hostPathClassLoader);
        // BaseDexClassLoader.java  找到private final DexPathList pathList; Class c = pathList.findClass(name, suppressedExceptions);
        Class<?> plugBaseDexClassLoaderPlugClass = Class.forName("dalvik.system.BaseDexClassLoader");
        // private final DexPathList pathList;
        Field plugBaseDexClassLoaderPlugField = plugBaseDexClassLoaderPlugClass.getDeclaredField("pathList");
        plugBaseDexClassLoaderPlugField.setAccessible(true);
        Object pulgPathList = plugBaseDexClassLoaderPlugField.get(plugPathClassLoader);
        // private Element[] dexElements;
        Field plugDexElementstField = pulgPathList.getClass().getDeclaredField("dexElements");
        plugDexElementstField.setAccessible(true);
        Object plugDexElements = plugDexElementstField.get(pulgPathList);

        // 第三步：创建出 新的 newDexElements []，类型必须是Element，必须是数组对象
        int hostLength = Array.getLength(hostDexElements);
        int plugLength = Array.getLength(plugDexElements);
        int sumLength = hostLength + plugLength;
        // 参数一：int[]  String[] ...  我们需要Element[]
        // 参数二：数组对象的长度
        // 本质就是 Element[] newDexElements
        // 创建数组对象
        Object newDexElements = Array.newInstance(hostDexElements.getClass().getComponentType(), sumLength);

        // 第四步：宿主dexElements + 插件dexElements =----> 融合  新的 newDexElements 赋值
        for (int i = 0; i < sumLength; i++) {
            // 先融合宿主
            if (i < hostLength) {
                // 参数一：新要融合的容器 -- newDexElements
                Array.set(newDexElements, i, Array.get(hostDexElements, i));
            } else {
                // 再融合插件的
                Array.set(newDexElements, i, Array.get(plugDexElements, i - hostLength));
            }
        }

        // 第五步：把新的 newDexElements，设置到宿主中去
        hostDexElementsField.set(hostPathList, newDexElements);

        // 处理加载插件中的布局
        doPluginLayoutLoad();
    }

    /**
     * 处理加载插件中的布局
     * Resources
     */
    private void doPluginLayoutLoad() throws Exception {
        File file = PluginManager.getInstance(this).getPlugFile();
        if (!file.exists()) {
            Log.e(">>>", "Error skinPath not exist...");
        }
        try {
            assetManager = AssetManager.class.newInstance();
            // 由于AssetManager中的addAssetPath和setApkAssets方法都被@hide，目前只能通过反射去执行方法
            Method addAssetPath = assetManager.getClass().getDeclaredMethod(ADD_ASSET_PATH, String.class);
            // 设置私有方法可访问
            addAssetPath.setAccessible(true);
            // addAssetPath
            addAssetPath.invoke(assetManager, file.getAbsolutePath());
            // 创建外部的资源包
            Resources hostResources = getResources();
            // 实例化此方法 final StringBlock[] ensureStringBlocks() , StringBlock[] 会加载资源xml文件
            Method ensureStringBlocksMethod = assetManager.getClass().getDeclaredMethod("ensureStringBlocks");
            ensureStringBlocksMethod.setAccessible(true);
            ensureStringBlocksMethod.invoke(assetManager); // 执行了ensureStringBlocks  string.xml  color.xml   anim.xml 被初始化
            plugResources = new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
        } catch (Exception e) {

        }
    }

    @Override
    public Resources getResources() {
        return plugResources == null ? super.getResources() : plugResources;
    }

    @Override
    public AssetManager getAssets() {
        return assetManager == null ? super.getAssets() : assetManager;
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
                            if (classArrayList.contains(className)) {
                                // 做自己的业务逻辑 换成 可以 通过 AMS检查的 ProxyActivity
                                Intent proxyIntent = new Intent(HookApplication.this, HooKProxyActivity.class);
                                proxyIntent.putExtra("actionIntent", intent);
                                args[2] = proxyIntent;
                            }
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
                            if (classArrayList.contains(className)) {
                                // 做自己的业务逻辑 换成 可以 通过 AMS检查的 ProxyActivity
                                Intent proxyIntent = new Intent(HookApplication.this, HooKProxyActivity.class);
                                proxyIntent.putExtra("actionIntent", intent);
                                args[2] = proxyIntent;
                            }
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
        mCallback.set(handler, new MyCallback26(handler));
    }

    public class MyCallback26 implements Handler.Callback {
        private Handler handler;

        public MyCallback26(Handler handler) {
            this.handler = handler;
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case EXECUTE_TRANSACTION: {
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
                            Class<?> launchActivityClass = Class.forName("android.app.servertransaction.LaunchActivityItem");
                            if (launchActivityClass.isInstance(item)) {
                                // 获取 LaunchActivityItem
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


                                    Field mInfoField = launchActivityClass.getDeclaredField("mInfo");
                                    // TODO: 2020/3/15 LoadApk 需要这样操作，classLoader融合不需要这样做
                                    /***
                                     *  我们在以下代码中，对插件  和 宿主 进行区分
                                     *   LoadApk -->> initializeJavaContextClassLoader(); 插件的没有实例化会报错
                                     *    if (pi == null) {
                                     *             throw new IllegalStateException("Unable to get package info for "
                                     *                     + mPackageName + "; is package not installed?");
                                     *         }
                                     */
                                    mInfoField.setAccessible(true);
                                    ActivityInfo mInfoObject = (ActivityInfo) mInfoField.get(item);
                                    // 什么时候 加载插件的
                                    if (actionIntent.getPackage() == null) { // 证明是插件
                                        mInfoObject.applicationInfo.packageName = actionIntent.getComponent().getPackageName();
                                        // Hook 拦截此 getPackageInfo 做自己的逻辑
                                        hookGetPackageInfo();
                                    } else { // 宿主
                                        mInfoObject.applicationInfo.packageName = actionIntent.getPackage();
                                    }
                                }
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

                            // TODO: 2020/3/15 LoadApk 需要这样操作，classLoader融合不需要这样做
                            /***
                             *  我们在以下代码中，对插件  和 宿主 进行区分
                             *   LoadApk -->> initializeJavaContextClassLoader(); 插件的没有实例化会报错
                             *    if (pi == null) {
                             *             throw new IllegalStateException("Unable to get package info for "
                             *                     + mPackageName + "; is package not installed?");
                             *         }
                             */
                            Field activityInfoField = obj.getClass().getDeclaredField("activityInfo");
                            activityInfoField.setAccessible(true);
                            ActivityInfo activityInfo = (ActivityInfo) activityInfoField.get(obj);
                            // 什么时候 加载插件的  ？
                            if (actionIntent.getPackage() == null) { // 证明是插件
                                activityInfo.applicationInfo.packageName = actionIntent.getComponent().getPackageName();
                                // Hook 拦截此 getPackageInfo 做自己的逻辑
                                hookGetPackageInfo();
                            } else { // 宿主
                                activityInfo.applicationInfo.packageName = actionIntent.getPackage();
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

    // 绕过pms检查
    private void hookGetPackageInfo() throws Exception {
        // sPackageManager 替换  我们自己的动态代理
        Class mActivityThreadClass = Class.forName("android.app.ActivityThread");
        // 获取实例化对象
        Object mActivityThread = mActivityThreadClass.getMethod("currentActivityThread").invoke(null);
        // 获取IPackageManager
        Field sPackageManagerField = mActivityThreadClass.getDeclaredField("sPackageManager");
        sPackageManagerField.setAccessible(true);
        final Object packageManager = sPackageManagerField.get(null);
        // 获取代理IPackageManager
        Class<?> iPackageMangerClass = Class.forName("android.content.pm.IPackageManager");

        /*
        实例化一个PackageInfo
         private void initializeJavaContextClassLoader() {
            IPackageManager pm = ActivityThread.getPackageManager();
            android.content.pm.PackageInfo pi;
            try {
                pi = pm.getPackageInfo(mPackageName, PackageManager.MATCH_DEBUG_TRIAGED_MISSING,
                        UserHandle.myUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
            if (pi == null) {
                throw new IllegalStateException("Unable to get package info for "
                        + mPackageName + "; is package not installed?");
             }
          }
        * */

        Object proxyInstance = Proxy.newProxyInstance(getClassLoader(),
                new Class[]{iPackageMangerClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("getPackageInfo".equals(method.getName())) {
                            // 如何才能绕过 PMS, 欺骗系统
                            // pi != null
                            return new PackageInfo(); // 成功绕过 PMS检测
                        }
                        return method.invoke(packageManager, args);
                    }
                });
        sPackageManagerField.set(mActivityThread, proxyInstance);
    }
}
