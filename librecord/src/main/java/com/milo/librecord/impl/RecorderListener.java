package com.milo.librecord.impl;

/**
 * 标题：录制监听
 * 功能：
 * 备注：
 * <p>
 * Created by Milo  2020/3/24
 * E-Mail : 303767416@qq.com
 */
public interface RecorderListener {

    void onStart();

    void onStop();

    void onPause();

    void onResume();

    void onReset();

}
