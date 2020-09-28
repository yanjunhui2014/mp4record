package com.milo.record.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.os.Environment;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.milo.librecord.Mp4Recorder;
import com.milo.librecord.impl.RecorderListener;
import com.milo.librecord.utils.LibRecordLog;
import com.milo.record.R;

import java.io.IOException;

/**
 * Title：
 * Describe：
 * Remark：
 * <p>
 * Created by Milo
 * E-Mail : 303767416@qq.com
 * 2020/9/27
 */
public class RecordActivity extends AppCompatActivity implements View.OnClickListener, RadioButton.OnCheckedChangeListener {
    protected static final String TAG = "RecordActivity";

    protected FrameLayout mFrameLayout;
    protected TextView    mTvState;
    protected RadioButton mRaBtn320;
    protected RadioButton mRaBtn640;
    protected RadioButton mRaBtn1280;
    protected Button      mBtnStart;
    protected Button      mBtnPause;
    protected Button      mBtnResume;
    protected Button      mBtnEnd;
    protected TextView    mTvRatio;

    protected TextureView mTextureView;
    protected Mp4Recorder mMp4Recorder;

    protected int mSpecialWidth = 0;

    public static Intent createIntent(Context context) {
        return new Intent(context, RecordActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        initView();
        Toast.makeText(this, "选择分辨率后初始化相机", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMp4Recorder != null) {
            mMp4Recorder.stopRecorder();
            mMp4Recorder.releaseRecorder();
            mMp4Recorder = null;
        }

        if (mTextureView != null) {
            mTextureView.getSurfaceTexture().release();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBtnStart) {
            try {
                mMp4Recorder.startRecorder();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else if (v == mBtnPause) {
            mMp4Recorder.stopRecorder();
        } else if (v == mBtnResume) {
            mMp4Recorder.resumeRecorder();
        } else if (v == mBtnEnd) {
            mMp4Recorder.stopRecorder();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked){
            mRaBtn320.setEnabled(false);
            mRaBtn640.setEnabled(false);
            mRaBtn1280.setEnabled(false);

            if (buttonView == mRaBtn320) {
                mSpecialWidth = 320;
            } else if (buttonView == mRaBtn640) {
                mSpecialWidth = 640;
            } else if (buttonView == mRaBtn1280) {
                mSpecialWidth = 720;
            }
            initTextureView();
        }
    }
    
    protected int getContentViewId(){
        return R.layout.activity_record;
    }

    protected boolean isLandScape(){
        return false;
    }

    protected void initView() {
        mFrameLayout = findViewById(R.id.mFrameLayout);
        mTvState = findViewById(R.id.mTvState);
        mRaBtn320 = findViewById(R.id.mRaBtn320);
        mRaBtn640 = findViewById(R.id.mRaBtn640);
        mRaBtn1280 = findViewById(R.id.mRaBtn1280);
        mBtnStart = findViewById(R.id.mBtnStart);
        mBtnStart.setOnClickListener(this);
        mBtnPause = findViewById(R.id.mBtnPause);
        mBtnPause.setOnClickListener(this);
        mBtnResume = findViewById(R.id.mBtnResume);
        mBtnResume.setOnClickListener(this);
        mBtnEnd = findViewById(R.id.mBtnEnd);
        mBtnEnd.setOnClickListener(this);
        mTvRatio = findViewById(R.id.mTvRatio);

        mRaBtn320.setOnCheckedChangeListener(this);
        mRaBtn640.setOnCheckedChangeListener(this);
        mRaBtn1280.setOnCheckedChangeListener(this);
    }

    protected void initTextureView() {
        if (mTextureView == null) {
            mTextureView = new TextureView(this);
            mFrameLayout.addView(mTextureView);

            mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    LibRecordLog.d(TAG, "onSurfaceTextureAvailable .. ");
                    initRecorder();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    LibRecordLog.d(TAG, "onSurfaceTextureSizeChanged .. ");
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    LibRecordLog.d(TAG, "onSurfaceTextureDestroyed .. ");
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                }
            });
        }
    }

    protected void initRecorder() {
        if (mMp4Recorder == null) {
            try {
                mMp4Recorder = new Mp4Recorder(this, mTextureView, new RecorderListener() {
                    @Override
                    public void onStart() {
                        mTvState.setText("录制中 ...");
                    }

                    @Override
                    public void onStop() {
                        mTvState.setText("录制已停止 ...");
                    }

                    @Override
                    public void onPause() {
                        mTvState.setText("暂停中 ...");
                    }

                    @Override
                    public void onResume() {
                        mTvState.setText("录制恢复 ...");
                    }

                    @Override
                    public void onReset() {
                        mTvState.setText("重新录制 ...");
                    }
                }, true, mSpecialWidth, isLandScape());
                mMp4Recorder.initRecorder(Environment.getExternalStorageDirectory() + "/1test/b.mp4", 3, 120);

                updateLayoutParams();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void updateLayoutParams(){
        CamcorderProfile profile = mMp4Recorder.getCameraProvider().getRecommendProfile();
        ViewGroup.LayoutParams params = mTextureView.getLayoutParams();
        params.width = profile.videoFrameHeight;
        params.height = profile.videoFrameWidth;
        mTextureView.setLayoutParams(params);

        mTvRatio.setText(String.format("实际分辨率: %s*%s", profile.videoFrameWidth, profile.videoFrameHeight));
    }

}
