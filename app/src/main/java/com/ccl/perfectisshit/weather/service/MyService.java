package com.ccl.perfectisshit.weather.service;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ccl.perfectisshit.weather.MainActivity;

/**
 * Created by ccl on 2017/5/25.
 */

public class MyService extends Service {
    private static final String TAG = "ScreenReceiver";
    private final static int GRAY_SERVICE_ID = 1001;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final IntentFilter filter = new IntentFilter();
        // 屏幕灭屏广播
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        // 屏幕亮屏广播
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // 屏幕解锁广播
        filter.addAction(Intent.ACTION_USER_PRESENT);
        // 当长按电源键弹出“关机”对话或者锁屏时系统会发出这个广播
        // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者“关机”对话框之上，
        // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(new ScreenReceiver(),filter);
        if (Build.VERSION.SDK_INT < 18) {
            startForeground(GRAY_SERVICE_ID, new Notification());//API < 18 ，此方法能有效隐藏Notification上的图标
        } else {
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(GRAY_SERVICE_ID, new Notification());
        }
//        final IntentFilter filter = new IntentFilter();
//        // 屏幕灭屏广播
//        filter.addAction(Intent.ACTION_SCREEN_OFF);
//        // 屏幕亮屏广播
//        filter.addAction(Intent.ACTION_SCREEN_ON);
//        // 屏幕解锁广播
//        filter.addAction(Intent.ACTION_USER_PRESENT);
//        // 当长按电源键弹出“关机”对话或者锁屏时系统会发出这个广播
//        // example：有时候会用到系统对话框，权限可能很高，会覆盖在锁屏界面或者“关机”对话框之上，
//        // 所以监听这个广播，当收到时就隐藏自己的对话，如点击pad右下角部分弹出的对话框
//        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
//        registerReceiver(new ScreenReceiver(), filter);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 给 API >= 18 的平台上用的灰色保活手段
     */
    public static class GrayInnerService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d(TAG, "service");
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

    }

    public class ScreenReceiver extends BroadcastReceiver {

        private static final String TAG = "ScreenReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                Log.d(TAG, "screen on");
                /*if (MainActivity.isPlaying) {
                    Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
                    activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(activityIntent);
                }*/
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                Log.d(TAG, "screen off");

            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                Log.d(TAG, "screen unlock");


            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                Log.i(TAG, " receive Intent.ACTION_CLOSE_SYSTEM_DIALOGS");
            }
        }
    }
}
