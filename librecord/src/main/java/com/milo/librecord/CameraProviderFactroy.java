package com.milo.librecord;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.IntRange;

import com.milo.librecord.feature.Api21CameraProvider;
import com.milo.librecord.feature.ApiLowCameraProvider;
import com.milo.librecord.impl.CameraProvider;

/**
 * 标题：相机工厂
 * 功能：
 * 备注：
 * <p>
 * Created by Milo  2020/3/24
 * E-Mail : 303767416@qq.com
 */
public class CameraProviderFactroy {

    private Activity mActivity;

    public CameraProviderFactroy(Activity activity) {
        this.mActivity = activity;
    }

    public CameraProvider getCameraProvider(@IntRange(from = 0, to = 1) int cameraId) {
        if (camera2Enable()) {
            return new Api21CameraProvider(mActivity, String.valueOf(cameraId));
        }

        return new ApiLowCameraProvider(cameraId);
    }

    public CameraProvider getCameraProvider(@IntRange(from = 0, to = 1) int cameraId, boolean lowDefinitionFirst) {
        if (camera2Enable()) {
            return new Api21CameraProvider(mActivity, String.valueOf(cameraId));
        }

        return new ApiLowCameraProvider(cameraId, lowDefinitionFirst);
    }

    public CameraProvider getCameraProvider(@IntRange(from = 0, to = 1) int cameraId, int specialWidth, boolean isLandScape) {
        if (camera2Enable()) {
            return new Api21CameraProvider(mActivity, String.valueOf(cameraId), specialWidth, isLandScape);
        }

        return new ApiLowCameraProvider(cameraId, specialWidth, isLandScape);
    }

    public static boolean camera2Enable() {
        return Build.VERSION.SDK_INT >= 21;
    }

}
