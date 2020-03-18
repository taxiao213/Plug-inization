package com.plug.inization;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.plug.inization.fix.FixModel;
import com.plug.inization.fix.FixUtils;

/**
 * Created by A35 on 2020/3/12
 * Email:yin13753884368@163.com
 * CSDN:http://blog.csdn.net/yin13753884368/article
 * Github:https://github.com/taxiao213
 */
public class FixBugActivity extends Activity {

    private TextView tvFixInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FixUtils.getInstance().fixDex(FixBugActivity.this);
        setContentView(R.layout.activity_fix_bug);
        tvFixInfo = (TextView) findViewById(R.id.tv_fix_info);
        Button tvFixBut = (Button) findViewById(R.id.tv_fix_but);
        Button tvFixBug = (Button) findViewById(R.id.tv_fix_bug);
        Button tvFixRestore = (Button) findViewById(R.id.tv_fix_restore);

        tvFixInfo.setText(new FixModel().getName());

        tvFixBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 显示修复信息
                tvFixInfo.setText(new FixModel().getName());
            }
        });

        tvFixBug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 修复Bug
                FixUtils.getInstance().copyDexFile(FixBugActivity.this);
            }
        });

        tvFixRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 还原Bug
                FixUtils.getInstance().deleteFixFile(FixBugActivity.this);
            }
        });
    }
}
