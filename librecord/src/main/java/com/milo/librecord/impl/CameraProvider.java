package com.milo.librecord.impl;

import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

/**
 * 标题：
 * 功能：
 * 备注：
 * <p>
 * Created by Milo  2020/3/24
 * E-Mail : 303767416@qq.com
 */
public interface CameraProvider<T> {

    int NO_FRAME_RATE = -1; //不指定帧率

    void init(SurfaceTexture displayView) throws Exception;

    void release();

    void startPreView(MediaRecorder mediaRecorder);

    void startRecord(MediaRecorder mediaRecorder);

    /**
     * @param minValue 获取帧率,最小值，会自动乘以1000
     * @return {{@link #NO_FRAME_RATE}} 则表示帧率获取失败
     */
    int getFrameRate(int minValue);


    /**
     * 获取推荐分辨率配置
     *
     * @return
     */
    CamcorderProfile getRecommendProfile(); //获取分辨率配置

    /**
     * 获取指定分辨率
     *
     * @param quality
     * @return
     */
    CamcorderProfile getProfile(int quality); //获取分辨率配置

    T getCamera();

}
