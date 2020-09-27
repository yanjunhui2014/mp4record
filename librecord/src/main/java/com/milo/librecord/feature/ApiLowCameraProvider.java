package com.milo.librecord.feature;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

import androidx.annotation.IntRange;

import com.milo.librecord.impl.CameraProvider;
import com.milo.librecord.utils.LibRecordLog;

import java.io.IOException;
import java.util.List;

/**
 * 标题：API低等级的相机提供者
 * 功能：
 * 备注：
 * <p>
 * Created by Milo  2020/3/24
 * E-Mail : 303767416@qq.com
 */
public final class ApiLowCameraProvider implements CameraProvider<Camera> {
    private static final String TAG = ApiLowCameraProvider.class.getSimpleName();

    public static final int CAMERA_FONT = 1;
    public static final int CAMERA_BACK = 0;

    private Camera         mCamera;
    private SurfaceTexture mDisplayView;
    private int            mFrameRate = NO_FRAME_RATE;//帧率

    protected int         mCameraId;
    private   List<int[]> mPreviewFpsRange = null;//相机fps支持

    private boolean mHasInit; //是否初始化
    private boolean mLowDefinitionFirst;
    private int     mSpecialWidth = 0;
    private boolean mLandScape = false;

    public ApiLowCameraProvider(@IntRange(from = 0, to = 1) int cameraId) {
        this(cameraId, false);
    }

    /**
     * @param cameraId
     * @param lowDefinitionFirst - 是否优先使用低清
     */
    public ApiLowCameraProvider(@IntRange(from = 0, to = 1) int cameraId, boolean lowDefinitionFirst) {
        this.mCameraId = cameraId;
        this.mLowDefinitionFirst = lowDefinitionFirst;
    }

    /**
     * @param cameraId
     * @param specialWidth - 指定宽度,若没有，则选择更高的宽度
     */
    public ApiLowCameraProvider(@IntRange(from = 0, to = 1) int cameraId, int specialWidth, boolean landscape) {
        this.mCameraId = cameraId;
        this.mSpecialWidth = specialWidth;
        this.mLandScape = landscape;
    }

    @Override
    public void init(SurfaceTexture displayView) throws Exception {
        if (mCamera == null) {
            mCamera = Camera.open(mCameraId);
        }
        if (mCamera == null) {
            throw new IllegalStateException("相机初始化失败");
        }
        if (displayView == null) {
            throw new NullPointerException("displayView不可为空");
        }
        if (mHasInit) {
            LibRecordLog.d(TAG, "初始化方法不执行：该相机已经初始化了...");
            return;
        }
        mHasInit = true;

        mCamera.lock();
        Camera.Parameters parameters = mCamera.getParameters();
        //设置聚焦模式
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        //设置预览尺寸
        CamcorderProfile profile = getRecommendProfile();
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);

        if (mCameraId == CAMERA_BACK) {
            parameters.setRecordingHint(true); //缩短Recording启动时间
        }
        //是否支持影像稳定能力，支持则开启
        if (parameters.isVideoStabilizationSupported()) {
            parameters.setVideoStabilization(true);
        }
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(mLandScape ? 0 : 90);
        mCamera.setPreviewTexture(displayView);
        mDisplayView = displayView;

        //获取相机支持的>=20fps的帧率，用于设置给MediaRecorder
        //因为获取的数值是*1000的，所以要除以1000
        mPreviewFpsRange = parameters.getSupportedPreviewFpsRange();

        mCamera.unlock();
    }

    @Override
    public void release() {
        if (mCamera != null) {
            LibRecordLog.d(TAG, "相机资源释放...");
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void startPreView(MediaRecorder mediaRecorder) {
        mCamera.lock();
        mCamera.startPreview();
        mCamera.unlock();
    }

    @Override
    public void startRecord(MediaRecorder mediaRecorder) {
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getFrameRate(int minValue) {
        if (mFrameRate != NO_FRAME_RATE) {
            return mFrameRate;
        }

        for (int[] ints : mPreviewFpsRange) {
            if (ints[0] >= minValue * 1000) {
                mFrameRate = ints[0] / 1000;
                break;
            }
        }

        return mFrameRate;
    }

    @Override
    public CamcorderProfile getRecommendProfile() {
        if (mSpecialWidth != 0) {
            if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_480P) && CamcorderProfile.get(CamcorderProfile.QUALITY_480P).videoFrameHeight >= mSpecialWidth) {
                return CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
            }
            if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_720P) && CamcorderProfile.get(CamcorderProfile.QUALITY_720P).videoFrameHeight >= mSpecialWidth) {
                return CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
            }
            if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_1080P) && CamcorderProfile.get(CamcorderProfile.QUALITY_1080P).videoFrameHeight >= mSpecialWidth) {
                return CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
            }
        }

        if (mLowDefinitionFirst) {
            if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_QCIF)) {
                return CamcorderProfile.get(CamcorderProfile.QUALITY_QCIF);
            }
        }

        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_480P)) {
            return CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        }

        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_CIF)) {
            return CamcorderProfile.get(CamcorderProfile.QUALITY_CIF);
        }

        return CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA);
    }

    @Override
    public CamcorderProfile getProfile(int quality) {
        return CamcorderProfile.get(mCameraId, quality);
    }

    @Override
    public Camera getCamera() {
        return mCamera;
    }

}
