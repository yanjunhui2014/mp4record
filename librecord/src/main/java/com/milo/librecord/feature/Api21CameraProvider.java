package com.milo.librecord.feature;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.milo.librecord.impl.CameraProvider;
import com.milo.librecord.utils.LibRecordLog;

import java.io.IOException;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * 标题：api >= 21 的相机能力提供实现
 * 功能：
 * 备注：用Camera2实现，待扩展
 * <p>
 * Created by Milo  2020/3/25
 * E-Mail : 303767416@qq.com
 */
@SuppressLint({"NewApi", "MissingPermission"})
public class Api21CameraProvider implements CameraProvider<CameraDevice> {
    private static final String TAG = Api21CameraProvider.class.getSimpleName();

    private Context        mContext;
    private CameraManager  mCameraManager;
    private CameraDevice   mCameraDevice;
    private SurfaceTexture mDisplayView;
    private Surface        mPreviewSurface;

    private Disposable    mDisposable;
    private Disposable    mPreviewDisposable;
    private HandlerThread mBackgroundThread;
    private Handler       mBackgroundHandler;

    private CaptureRequest.Builder mCaptureRequestBuilder;

    private CameraCaptureSession mRecordSession;
    private CameraCaptureSession mPreviewSession;
    private String               mCameraId;

    private int     mSpecialWidth = 0;
    private boolean mLandScape    = false;

    private Observable<CameraDevice> mCameraDeviceObservable;

    public Api21CameraProvider(Context context, String cameraId) {
        this(context, cameraId, 0, false);
    }

    public Api21CameraProvider(Context context, String cameraId, int specialWidth) {
        this(context, cameraId, specialWidth, false);
    }

    public Api21CameraProvider(Context context, String cameraId, int specialWidth, boolean landScape) {
        this.mContext = context;
        this.mCameraId = cameraId;
        this.mSpecialWidth = specialWidth;
        this.mLandScape = landScape;
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        mCameraDeviceObservable = Observable.create(new ObservableOnSubscribe<CameraDevice>() {
            @Override
            public void subscribe(final ObservableEmitter<CameraDevice> emitter) throws Exception {
                if (mCameraDevice != null) {
                    emitter.onNext(mCameraDevice);
                } else {
                    mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            LibRecordLog.d(TAG, "相机成功开启");
                            mCameraDevice = camera;

                            emitter.onNext(camera);
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {
                            LibRecordLog.d(TAG, "相机被关闭");
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            emitter.onError(new Throwable("相机打开失败:" + error));
                        }
                    }, null);
                }
            }
        });
    }

    @Override
    public void init(final SurfaceTexture displayView) throws Exception {
        this.mDisplayView = displayView;
        startBackgroundThread();
    }

    @Override
    public void release() {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
        if (mPreviewDisposable != null && !mPreviewDisposable.isDisposed()) {
            mPreviewDisposable.dispose();
        }

        if (mPreviewSurface != null) {
            mPreviewSurface.release();
        }
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
        if (mRecordSession != null) {
            mRecordSession.close();
            mRecordSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        stopBackgroundThread();
    }

    @Override
    public void startPreView(final MediaRecorder mediaRecorder) {
        LibRecordLog.d(TAG, "开启预览");

        mPreviewDisposable = mCameraDeviceObservable.flatMap(new Function<CameraDevice, ObservableSource<CameraCaptureSession>>() {
            @Override
            public ObservableSource<CameraCaptureSession> apply(final CameraDevice cameraDevice) throws Exception {
                return Observable.create(new ObservableOnSubscribe<CameraCaptureSession>() {
                    @Override
                    public void subscribe(final ObservableEmitter<CameraCaptureSession> emitter) throws Exception {
                        mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                        mPreviewSurface = new Surface(mDisplayView);
                        mDisplayView.setDefaultBufferSize(getRecommendProfile().videoFrameWidth, getRecommendProfile().videoFrameHeight);
                        mCaptureRequestBuilder.addTarget(mPreviewSurface);

                        mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                LibRecordLog.d(TAG, "session配置成功");
                                emitter.onNext(session);
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                LibRecordLog.d(TAG, "session配置成功");
                                emitter.onError(new Throwable("session配置失败"));
                            }
                        }, mBackgroundHandler);
                    }
                });
            }
        }).subscribe(new Consumer<CameraCaptureSession>() {
            @Override
            public void accept(CameraCaptureSession session) throws Exception {
                mPreviewSession = session;
                updatePreview();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                LibRecordLog.d(TAG, "预览图显示失败" + throwable.getMessage());
            }
        });
    }

    @Override
    public void startRecord(final MediaRecorder mediaRecorder) {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }

        if (Build.VERSION.SDK_INT >= 21) {
            try {
                mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

                mediaRecorder.prepare();
                //添加预览的Surface
                mDisplayView.setDefaultBufferSize(getRecommendProfile().videoFrameWidth, getRecommendProfile().videoFrameHeight);
                Surface previewSurface = new Surface(mDisplayView);
                mCaptureRequestBuilder.addTarget(previewSurface);
                //添加MediaRecorder的Surface
                Surface recorderSurface = mediaRecorder.getSurface();
                mCaptureRequestBuilder.addTarget(recorderSurface);

                mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recorderSurface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        LibRecordLog.d(TAG, "CameraCaptureSession .. suc .. ");
                        mRecordSession = session;
                        updatePreview();
                        mediaRecorder.start();
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        LibRecordLog.d(TAG, "CameraCaptureSession .. fail .. ");
                    }
                }, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getFrameRate(int minValue) {
        try {
            Range<Integer>[] range = getCameraCharacteristics().get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            for (int i = 0; i < range.length; i++) {
                if (range[i].contains(minValue)) {
                    return minValue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public CamcorderProfile getRecommendProfile() {
        if (mSpecialWidth != 0) {
            if (CamcorderProfile.hasProfile(Integer.parseInt(mCameraId), CamcorderProfile.QUALITY_480P) && CamcorderProfile.get(CamcorderProfile.QUALITY_480P).videoFrameHeight >= mSpecialWidth) {
                return CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
            }
            if (CamcorderProfile.hasProfile(Integer.parseInt(mCameraId), CamcorderProfile.QUALITY_720P) && CamcorderProfile.get(CamcorderProfile.QUALITY_720P).videoFrameHeight >= mSpecialWidth) {
                return CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
            }
            if (CamcorderProfile.hasProfile(Integer.parseInt(mCameraId), CamcorderProfile.QUALITY_1080P) && CamcorderProfile.get(CamcorderProfile.QUALITY_1080P).videoFrameHeight >= mSpecialWidth) {
                return CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
            }
        }

        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
            return CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        }

        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_CIF)) {
            return CamcorderProfile.get(CamcorderProfile.QUALITY_CIF);
        }

        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QVGA)) {
            return CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA);
        }

        CamcorderProfile profile = null;

        for (int i = 0; i <= 8; i++) {
            if (CamcorderProfile.hasProfile(i)) {
                profile = CamcorderProfile.get(i);
                break;
            }
        }

        return profile;
    }

    @Override
    public CamcorderProfile getProfile(int quality) {
        return CamcorderProfile.get(quality);
    }

    @Override
    public CameraDevice getCamera() {
        return mCameraDevice;
    }

    private CameraCharacteristics getCameraCharacteristics() throws CameraAccessException {
        return mCameraManager.getCameraCharacteristics(mCameraId);
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            if (mPreviewSession != null) {
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                mPreviewSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
            }
            if (mRecordSession != null) {
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                mRecordSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (Build.VERSION.SDK_INT >= 18) {
            if (mBackgroundThread != null) {
                mBackgroundThread.quitSafely();
                mBackgroundThread = null;
            }
        }
    }

}
