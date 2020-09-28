package com.milo.record;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.milo.librecord.utils.LibRecordLog;
import com.milo.record.ui.LandScapeRecordActivity;
import com.milo.record.ui.RecordActivity;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private Button mBtnNormal;
    private Button mBtnLandScape;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if (Build.VERSION.SDK_INT >= 23)
            requestPermissions(new String[]{Manifest.permission_group.STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 100);

        File file = new File(Environment.getExternalStorageDirectory() + "/1test/b.mp4");
        LibRecordLog.d(TAG, "file.getAbsolutePath() == " + file.getAbsolutePath());
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBtnNormal) {
            startActivity(RecordActivity.createIntent(this));
        } else if (v == mBtnLandScape) {
            startActivity(LandScapeRecordActivity.createIntent(this));
        }
    }

    private void initView() {
        mBtnNormal = findViewById(R.id.mBtnNormal);
        mBtnNormal.setOnClickListener(this);

        mBtnLandScape = findViewById(R.id.mBtnLandScape);
        mBtnLandScape.setOnClickListener(this);
    }

}
