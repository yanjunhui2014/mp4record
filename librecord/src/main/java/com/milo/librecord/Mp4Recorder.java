package com.milo.librecord;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.text.TextUtils;
import android.view.Surface;
import android.view.TextureView;

import com.milo.librecord.feature.ApiLowCameraProvider;
import com.milo.librecord.impl.CameraProvider;
import com.milo.librecord.impl.RecorderListener;
import com.milo.librecord.impl.RecorderProvider;
import com.milo.librecord.utils.LibRecordLog;

import java.io.File;
import java.io.IOException;

/**
 * 标题：mp4录制器
 * 功能：
 * 备注：
 * <p>
 * Created by Milo  2020/3/24
 * E-Mail : 303767416@qq.com
 */
public class Mp4Recorder implements RecorderProvider {
    private static final String TAG = Mp4Recorder.class.getSimpleName();

    private String mVideoPath;
    private int    mMaxDuration;
    private int    mMaxFileSize;

    private MediaRecorder    mMediaRecorder;
    private CameraProvider   mCameraProvider;
    private RecorderListener mRecorderListener;

    private boolean mIsLandScape;
    public  boolean mIsFontCamera = false; //是否前置摄像机

    public int mVideoEncodingBitRate;//指定bitRate
    public int mVideoFrameBitRate;//指定帧bitRate

    public Mp4Recorder(Activity activity, SurfaceTexture displayView, RecorderListener recorderListener) throws Exception {
        this(activity, displayView, recorderListener, false, false);
    }

    public Mp4Recorder(Activity activity, SurfaceTexture displayView, RecorderListener recorderListener, boolean isFontCamera) throws Exception {
        this(activity, displayView, recorderListener, false, isFontCamera);
    }

    /**
     * @param activity
     * @param displayView
     * @param recorderListener
     * @param isFontCamera     - 是否前置摄像机
     * @throws IOException
     */
    public Mp4Recorder(Activity activity, SurfaceTexture displayView, RecorderListener recorderListener, boolean isFontCamera, boolean lowDefinitionFirst) throws Exception {
        this.mIsFontCamera = isFontCamera;
        mCameraProvider = new CameraProviderFactroy(activity).getCameraProvider(isFontCamera ? 1 : 0, lowDefinitionFirst);
        if (mCameraProvider == null) {
            throw new IllegalArgumentException("当前相机不支持拍摄");
        }

        mCameraProvider.init(displayView);
        mRecorderListener = recorderListener;
    }

    /**
     * @param activity
     * @param displayView
     * @param recorderListener
     * @param isFontCamera     - 是否前置摄像机
     * @param specialWidth     - 指定宽度
     * @throws IOException
     */
    public Mp4Recorder(Activity activity, SurfaceTexture displayView, RecorderListener recorderListener, boolean isFontCamera, int specialWidth) throws Exception {
        this.mIsFontCamera = isFontCamera;
        mCameraProvider = new CameraProviderFactroy(activity).getCameraProvider(isFontCamera ? 1 : 0, specialWidth, false);
        if (mCameraProvider == null) {
            throw new IllegalArgumentException("当前相机不支持拍摄");
        }

        mCameraProvider.init(displayView);
        mRecorderListener = recorderListener;
    }

    /**
     * @param activity
     * @param displayView
     * @param recorderListener
     * @param isFontCamera     - 是否前置摄像机
     * @param specialWidth     - 指定宽度
     * @param isLandScape      - 是否横屏
     * @throws IOException
     */
    public Mp4Recorder(Activity activity, SurfaceTexture displayView, RecorderListener recorderListener, boolean isFontCamera, int specialWidth, boolean isLandScape) throws Exception {
        this.mIsFontCamera = isFontCamera;
        this.mIsLandScape = isLandScape;
        mCameraProvider = new CameraProviderFactroy(activity).getCameraProvider(isFontCamera ? 1 : 0, specialWidth, isLandScape);
        if (mCameraProvider == null) {
            throw new IllegalArgumentException("当前相机不支持拍摄");
        }

        mCameraProvider.init(displayView);
        mRecorderListener = recorderListener;
    }

    /**
     * @param activity
     * @param textureView
     * @param recorderListener
     * @param isFontCamera     - 是否前置摄像机
     * @param specialWidth     - 指定宽度
     * @param isLandScape      - 是否横屏
     * @throws IOException
     */
    public Mp4Recorder(final Activity activity, final TextureView textureView, RecorderListener recorderListener, boolean isFontCamera, int specialWidth, boolean isLandScape) throws Exception {
        this.mIsFontCamera = isFontCamera;
        this.mIsLandScape = isLandScape;
        mCameraProvider = new CameraProviderFactroy(activity).getCameraProvider(isFontCamera ? 1 : 0, specialWidth, isLandScape);
        if (mCameraProvider == null) {
            throw new IllegalArgumentException("当前相机不支持拍摄");
        }

        mCameraProvider.init(textureView.getSurfaceTexture());
        textureView.post(new Runnable() {
            @Override
            public void run() {
                configureTransform(activity, textureView, textureView.getMeasuredWidth(), textureView.getMeasuredHeight());
            }
        });
        mRecorderListener = recorderListener;
    }

    @Override
    public void initRecorder(String videoPath, int maxDuration, int maxFileSize) throws Exception {
        this.mVideoPath = videoPath;
        this.mMaxDuration = maxDuration;
        this.mMaxFileSize = maxFileSize;

        if (TextUtils.isEmpty(videoPath)) {
            throw new NullPointerException("传入的videoPath不可为空");
        }
        if (maxDuration < 0) {
            throw new IllegalArgumentException("maxDuration不可以小于0");
        }
        if (maxFileSize < 0) {
            throw new IllegalArgumentException("maxFileSize不可以小于0");
        }

        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }

        if (mIsLandScape) {
            mMediaRecorder.setOrientationHint(0);
        } else {
            mMediaRecorder.setOrientationHint(mIsFontCamera ? 270 : 90);
        }

        mCameraProvider.startPreView(mMediaRecorder);

        if (mCameraProvider instanceof ApiLowCameraProvider) {
            mMediaRecorder.setCamera(((Camera) mCameraProvider.getCamera()));
        }

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        if (CameraProviderFactroy.camera2Enable()) {
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        } else {
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        }

        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioChannels(1);//单声道支持率最高
        mMediaRecorder.setAudioEncodingBitRate(22050);//采样率, 44.1HZ

        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        File file = new File(videoPath);
        if (file.exists()) {
            file.delete();
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("文件路径不可以是一个文件夹:" + file.getAbsolutePath());
        } else {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                throw new IllegalAccessException("文件夹:" + file.getParentFile() + "创建失败");
            }
            if (!file.createNewFile()) {
                throw new IllegalAccessException("文件:" + file.getAbsolutePath() + "创建失败");
            }
        }
        mMediaRecorder.setOutputFile(file.getAbsolutePath());

        if (mVideoFrameBitRate != 0) {
            mMediaRecorder.setVideoFrameRate(mVideoFrameBitRate);
        } else {
            final int frameRate = mCameraProvider.getFrameRate(30);
            LibRecordLog.d(TAG, "初始化时设置的帧率为:" + frameRate + "fps");
            if (frameRate != CameraProvider.NO_FRAME_RATE) {
                mMediaRecorder.setVideoFrameRate(frameRate);
            }
        }

        CamcorderProfile profile = mCameraProvider.getRecommendProfile();
        LibRecordLog.d(TAG, "初始化时设置的分辨率为:" + profile.videoFrameWidth + "*" + profile.videoFrameHeight + ", 码率为:" + profile.videoBitRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        if (mVideoEncodingBitRate != 0) {
            mMediaRecorder.setVideoEncodingBitRate(mVideoEncodingBitRate);
        } else {
            mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        }
    }

    @Override
    public CameraProvider getCameraProvider() {
        return mCameraProvider;
    }

    @Override
    @SuppressWarnings("NewApi")
    public void startRecorder() throws IllegalStateException, IOException, CameraAccessException {
        startRecorder(null);
    }

    @Override
    @SuppressWarnings("NewApi")
    public void startRecorder(CameraCaptureSession.StateCallback stateCallback) throws IllegalStateException, IOException, CameraAccessException {
        if (mMediaRecorder != null) {
            mCameraProvider.startRecord(mMediaRecorder);

            if (mRecorderListener != null) {
                mRecorderListener.onStart();
            }
        }
    }

    @Override
    public void stopRecorder() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
                ((Camera) mCameraProvider.getCamera()).stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mRecorderListener != null) {
                mRecorderListener.onStop();
            }
        }
    }

    @Override
    public void pauseRecorder() {
        try {
            if (mMediaRecorder != null) {
                if (Build.VERSION.SDK_INT >= 24) {
                    mMediaRecorder.pause();
                    if (mRecorderListener != null) {
                        mRecorderListener.onPause();
                    }
                } else {
                    mMediaRecorder.stop();
                    if (mRecorderListener != null) {
                        mRecorderListener.onStop();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resumeRecorder() {
        if (mMediaRecorder != null) {
            if (Build.VERSION.SDK_INT >= 24) {
                mMediaRecorder.resume();
                if (mRecorderListener != null) {
                    mRecorderListener.onResume();
                }
            } else {
                //api小于24什么也不做
            }
        }
    }

    @Override
    public void reset() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.reset();
                initRecorder(mVideoPath, mMaxDuration, mMaxFileSize);

                if (mRecorderListener != null) {
                    mRecorderListener.onReset();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void releaseRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        if (mCameraProvider != null) {
            mCameraProvider.release();
        }
    }

    @Override
    public MediaRecorder getMediaRecorder() {
        return mMediaRecorder;
    }

    /**
     * 适配画面预览方向
     *
     * @param activity
     * @param textureView
     * @param viewWidth
     * @param viewHeight
     */
    private void configureTransform(Activity activity, TextureView textureView, int viewWidth, int viewHeight) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, viewHeight, viewWidth);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / viewHeight,
                    (float) viewWidth / viewWidth);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

}
