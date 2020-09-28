package com.milo.record.ui;

import android.content.Context;
import android.content.Intent;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.milo.record.R;

/**
 * Title：
 * Describe：
 * Remark：
 * <p>
 * Created by Milo
 * E-Mail : 303767416@qq.com
 * 2020/9/27
 */
public class LandScapeRecordActivity extends RecordActivity {

    public static Intent createIntent(Context context){
        return new Intent(context, LandScapeRecordActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpecialWidth = 320;
        findViewById(R.id.mRaGroup).setVisibility(View.GONE);
        initTextureView();
    }

    @Override
    protected int getContentViewId() {
        return super.getContentViewId();
    }

    @Override
    protected boolean isLandScape() {
        return true;
    }

    @Override
    protected void updateLayoutParams(){
        CamcorderProfile profile = mMp4Recorder.getCameraProvider().getRecommendProfile();
        ViewGroup.LayoutParams params = mTextureView.getLayoutParams();
        params.width = profile.videoFrameWidth;
        params.height = profile.videoFrameHeight;
        mTextureView.setLayoutParams(params);

        mTvRatio.setText(String.format("实际分辨率: %s*%s", profile.videoFrameHeight, profile.videoFrameWidth));
    }

}
