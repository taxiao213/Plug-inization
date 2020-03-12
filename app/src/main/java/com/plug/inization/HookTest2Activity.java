package com.plug.inization;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * 不在AndroidManifest注册  使用hook技术 跳转此类
 * todo 不能继承AppCompatActivity
 * Created by A35 on 2020/3/12
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class HookTest2Activity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "Test2Activity", Toast.LENGTH_SHORT).show();
    }
}
