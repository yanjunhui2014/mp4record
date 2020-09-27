package com.milo.librecord.impl;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;

import java.io.IOException;

/**
 * 标题：
 * 功能：
 * 备注：
 * <p>
 * Created by Milo  2020/3/24
 * E-Mail : 303767416@qq.com
 */
public interface RecorderProvider {

    /**
     * @param recordPath  视频存储路径
     * @param maxDuration 设置最大时间限制，单位秒, 0不限制
     * @param maxFileSize 设置最大文件限制，单位， 0不限制
     * @throws Exception
     */
    void initRecorder(@NonNull String recordPath, int maxDuration, int maxFileSize) throws Exception;

    CameraProvider getCameraProvider();

    void startRecorder() throws IllegalStateException, IOException, CameraAccessException;

    void startRecorder(CameraCaptureSession.StateCallback stateCallback) throws IllegalStateException, IOException, CameraAccessException;

    void stopRecorder();

    void pauseRecorder();

    void resumeRecorder();

    void reset();

    void releaseRecorder();

    MediaRecorder getMediaRecorder();

}
