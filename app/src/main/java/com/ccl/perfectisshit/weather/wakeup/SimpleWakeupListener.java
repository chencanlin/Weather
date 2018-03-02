package com.ccl.perfectisshit.weather.wakeup;

import android.os.Handler;
import android.util.Log;


/**
 * Created by fujiayi on 2017/6/21.
 */

public class SimpleWakeupListener implements IWakeupListener {

    public static final int MESSAGE_SUCCEED = 0;

    private final Handler mHandler;

    public SimpleWakeupListener(Handler handler){
        mHandler = handler;
    }

    private static final String TAG = "SimpleWakeupListener";

    @Override
    public void onSuccess(String word, WakeUpResult result) {
        Log.i(TAG, "唤醒成功，唤醒词：" + word);
        mHandler.obtainMessage(MESSAGE_SUCCEED, word).sendToTarget();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "唤醒词识别结束：");
    }

    @Override
    public void onError(int errorCode, String errorMessge, WakeUpResult result) {
        Log.i(TAG, "唤醒错误：" + errorCode + ";错误消息：" + errorMessge + "; 原始返回" + result.getOrigalJson());
    }

    @Override
    public void onASrAudio(byte[] data, int offset, int length) {
        Log.i(TAG, "audio data： " + data.length);
    }

}
