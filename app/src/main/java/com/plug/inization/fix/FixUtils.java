package com.plug.inization.fix;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;


import com.plug.inization.FixBugActivity;
import com.plug.inization.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * 热修复工具类
 * Created by A35 on 2020/3/17
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class FixUtils {
    private static final String TAG = FixUtils.class.getSimpleName();
    private static FixUtils fixUtils;
    private String FIX_DEX_NAME = "fix_bug.dex";
    private String FIX_FILE_NAME = "fix_bug_dex";
    private File fixDexFile;

    public static FixUtils getInstance() {
        if (fixUtils == null) {
            synchronized (FixUtils.class) {
                if (fixUtils == null) {
                    fixUtils = new FixUtils();
                }
            }
        }
        return fixUtils;
    }

    private FixUtils() {

    }

    /**
     * 将修复后的Dex插桩在最前
     */
    public void fixDex(Context context) {
        File fixDexFile = getFixDexFile(context);
        if (fixDexFile != null && fixDexFile.exists()) {
            try {
                // BaseDexClassLoader类 -> pathList 反射得到 DexPathList，DexPathList类 -> dexElements 反射得到 Element[]，将修复后的 Element[] 置换
                // 获取 BaseDexClassLoader
                // private final DexPathList pathList;
                ClassLoader classLoader = context.getClassLoader();
                Class<?> baseDexClass = Class.forName("dalvik.system.BaseDexClassLoader");
                Field pathListField = baseDexClass.getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object pathListObject = pathListField.get(classLoader);

                // DexPathList类
                // private Element[] dexElements;
                Class<?> pathListClass = pathListObject.getClass();
                Field dexElementsField = pathListClass.getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);

                // 获取修复前的dex 和 修复后的dex 文件
                // private static Element[] makePathElements(List<File> files, List<IOException> suppressedExceptions, ClassLoader loader) {
                //      return makeElements(files, null, suppressedExceptions, true, loader);
                // }

                // private static Element[] makeDexElements(List<File> files, File optimizedDirectory,
                //        List<IOException> suppressedExceptions,
                //        ClassLoader loader) {
                //    return makeElements(files, optimizedDirectory, suppressedExceptions, false, loader);
                // }

                // 1.修复前的dex
                Object[] dexElementsObject = (Object[]) dexElementsField.get(pathListObject);

                // 2.修复后的dex
                List<File> files = new ArrayList<>();
                List<IOException> suppressedExceptions = new ArrayList<>();
                files.add(fixDexFile);
                Class<?> classLoad = Class.forName("java.lang.ClassLoader");
                Method makeDexElementsMethod = pathListClass.getDeclaredMethod("makeDexElements", List.class, File.class, List.class, classLoad);
                makeDexElementsMethod.setAccessible(true);
                File optimizedDirectory = context.getDir(FIX_FILE_NAME, Context.MODE_PRIVATE);
                Object dexFixElementsObject = makeDexElementsMethod.invoke(pathListObject, files, optimizedDirectory, suppressedExceptions, classLoader);

                int dexElements = Array.getLength(dexElementsObject);
                int dexFixElements = Array.getLength(dexFixElementsObject);
                int total = dexElements + dexFixElements;
                Object newElementsObject = Array.newInstance(dexElementsObject.getClass().getComponentType(), total);

                // 第一种写法
//                for (int i = 0; i < total; i++) {
//                    if (i < dexFixElements) {
//                        Array.set(newElementsObject, i, Array.get(dexFixElementsObject, i));
//                    } else {
//                        Array.set(newElementsObject, i, Array.get(dexElementsObject, i - dexFixElements));
//                    }
//                }

                // 第二种写法
                System.arraycopy(dexFixElementsObject, 0, newElementsObject, 0, dexFixElements);
                System.arraycopy(dexElementsObject, 0, newElementsObject, dexFixElements, dexElements);

                // 将修复后的dex 置换
                dexElementsField.set(pathListObject, newElementsObject);
                Log.d(TAG, ">>>修复成功");
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, ">>>修复失败" + e.getMessage());
                Toast.makeText(context, "修复失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, ">>>文件不存在");
        }
    }

    private File getFixDexFile(Context context) {
        fixDexFile = new File(context.getFilesDir(), FIX_DEX_NAME);
        return fixDexFile;
    }

    /**
     * 将 fix_bug.dex 复制到文件
     *
     * @return boolean  true 成功 false 失败
     */
    public boolean copyDexFile(Context context) {
        InputStream fixDexIs = null;
        FileOutputStream fixDexOs = null;
        boolean copySuccess = false;
        try {
            fixDexIs = context.getAssets().open(FIX_DEX_NAME);
            fixDexFile = new File(context.getFilesDir(), FIX_DEX_NAME);
            if (!fixDexFile.exists()) {
                fixDexFile.createNewFile();
            }
            fixDexOs = new FileOutputStream(fixDexFile);
            int available = fixDexIs.available();
            byte[] bytes = new byte[1024];
            int read;
            int total = 0;
            while ((read = fixDexIs.read(bytes)) != -1) {
                fixDexOs.write(bytes, 0, read);
                total += read;
            }
            if (total == available) {
                copySuccess = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fixDexIs != null) {
                    fixDexIs.close();
                }
                if (fixDexOs != null) {
                    fixDexOs.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (copySuccess) {
            Toast.makeText(context, "文件读取成功,请重新运行程序", Toast.LENGTH_SHORT).show();
            exit(context);
        } else {
            Toast.makeText(context, "文件读取失败", Toast.LENGTH_SHORT).show();
        }
        return copySuccess;
    }

    /**
     * bug还原
     */
    public void deleteFixFile(Context context) {
        File fixDexFile = new File(context.getFilesDir(), FIX_DEX_NAME);
        if (fixDexFile.exists()) {
            boolean delete = fixDexFile.delete();
            if (delete) {
                Toast.makeText(context, "bug还原成功", Toast.LENGTH_SHORT).show();
                exit(context);
                return;
            }
        }
        Toast.makeText(context, "bug还原失败", Toast.LENGTH_SHORT).show();
    }

    /**
     * 重启界面
     */
    private void exit(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
        System.exit(0);
    }
}
