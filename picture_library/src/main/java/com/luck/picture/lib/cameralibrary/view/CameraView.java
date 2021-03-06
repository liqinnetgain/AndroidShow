package com.luck.picture.lib.cameralibrary.view;

import android.graphics.Bitmap;

import java.util.List;

/**
 * =====================================
 * 作    者: 陈嘉桐
 * 版    本：1.1.4
 * 创建日期：2017/9/8
 * 描    述：
 * =====================================
 */
public interface CameraView {
    void resetState(int type);

    void confirmState(int type);

    void showPicture(Bitmap bitmap, boolean isVertical);

    void playVideo(String firstFramePath, String url, List<String> framePaths);

    void stopVideo();

    void setTip(String tip);

    void startPreviewCallback();

    boolean handlerFoucs(float x, float y);
}
