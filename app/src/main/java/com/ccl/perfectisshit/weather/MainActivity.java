package com.ccl.perfectisshit.weather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.ccl.perfectisshit.weather.listener.PlayerEventListener;
import com.ccl.perfectisshit.weather.service.MyService;
import com.ccl.perfectisshit.weather.util.HttpUtil;
import com.ccl.perfectisshit.weather.wakeup.SimpleWakeupListener;
import com.ccl.perfectisshit.weather.wakeup.WakeupEventAdapter;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements EventListener {

    public static boolean isPlaying = false;

    private static String DESC_TEXT = "精简版唤醒，带有SDK唤醒运行的最少代码，仅仅展示如何调用，\n" +
            "也可以用来反馈测试SDK输入参数及输出回调。\n" +
            "本示例需要自行根据文档填写参数，可以使用之前唤醒示例中的日志中的参数。\n" +
            "需要完整版请参见之前的唤醒示例。\n\n" +
            "唤醒词是纯离线功能，需要获取正式授权文件（与离线命令词的正式授权文件是同一个）。 第一次联网使用唤醒词功能自动获取正式授权文件。之后可以断网测试\n" +
            "请说“小度你好”或者 “百度一下”\n\n";

    private EventManager wakeup;

    public boolean logTime = true;

    private SimpleExoPlayer mPlayer;
    private PlayerView mPlayerView;

    private PowerManager.WakeLock mWakeLock;
    private View mRoot;

    /**
     * 测试参数填在这里
     */
    private void start() {
        Map<String, Object> params = new TreeMap<String, Object>();

        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");
        // "assets:///WakeUp.bin" 表示WakeUp.bin文件定义在assets目录下

        String json; // 这里可以替换成你需要测试的json
        json = new JSONObject(params).toString();
        wakeup.send(SpeechConstant.WAKEUP_START, json, null, 0, 0);
        printLog("输入参数：" + json);
    }

    private void stop() {
        wakeup.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0); //
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setContentView(R.layout.common_mini);

        saveVideo();
        mRoot = findViewById(R.id.root);
        Intent intent = new Intent(this, MyService.class);
        startService(intent);
        wakeup = EventManagerFactory.create(this, "wp");
//        wakeup.registerListener(this);
        wakeup.registerListener(new WakeupEventAdapter(new SimpleWakeupListener(new MyHandler(this)))); //  EventListener 中 onEvent方法
        start();
        initPlayer();
    }

    private void saveVideo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream open = getResources().getAssets().open("earth.flv");
                    File file = new File("sdcard/Download/earth.flv");
                    if (!file.exists() || !file.isFile()) {
                        file.createNewFile();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    int len;
                    byte[] bytes = new byte[1024];
                    while ((len = open.read(bytes)) != -1) {
                        fileOutputStream.write(bytes, 0, len);
                    }
                    open.close();
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "save succeed", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initPlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

// 2. Create the player
        mPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        mPlayerView = findViewById(R.id.player_view);
        mPlayerView.setPlayer(mPlayer);
        mPlayer.setPlayWhenReady(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isPlaying) {
            simulateClickHome();
        }
    }

    private void simulateClickHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
        mRoot.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wakeup.send(SpeechConstant.WAKEUP_STOP, "{}", null, 0, 0);
        releaseScreenWakeLock();
        releasePlayer();
    }


    //   EventListener  回调方法
    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        /*if(name.equalsIgnoreCase(SpeechConstant.CALLBACK_EVENT_WAKEUP_STARTED)){
            new MyHandler(this).obtainMessage(SimpleWakeupListener.MESSAGE_SUCCEED, "word").sendToTarget();
        }*/
        String logTxt = "name: " + name;
        if (params != null && !params.isEmpty()) {
            logTxt += " ;params :" + params;
        } else if (data != null) {
            logTxt += " ;data length=" + data.length;
        }
        printLog(logTxt);
    }

    private void printLog(String text) {
        if (logTime) {
            text += "  ;time=" + System.currentTimeMillis();
        }
        text += "\n";
        Log.i(getClass().getName(), text);
//        txtLog.append(text + "\n");
    }


    @SuppressLint("HandlerLeak")
    private class MyHandler extends Handler {
        private SoftReference<Context> mContextSoftReference;

        MyHandler(Context context) {
            mContextSoftReference = new SoftReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mContextSoftReference == null || mContextSoftReference.get() == null) {
                return;
            }
            switch (msg.what) {
                case SimpleWakeupListener.MESSAGE_SUCCEED:
                    Toast.makeText(MainActivity.this, ((String) msg.obj), Toast.LENGTH_SHORT).show();
                        /*Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);*/
                    if (isPlaying) {
                        return;
                    }
                    isPlaying = true;
                    Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
                    activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(activityIntent);
                    requestWeather("CN101020600");
                    break;
            }
        }
    }

    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=cf44850238a540f5b5bc0a1e2bc9ca75";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseText = response.body().string();
                try {
                    JSONObject responseObj = new JSONObject(responseText);
                    JSONArray jsonArray = responseObj.optJSONArray("HeWeather");
                    String code = "";
                    String txt = "";
                    if (jsonArray.length() > 0) {
                        JSONObject jsonObject = jsonArray.optJSONObject(0);
                        JSONObject now = jsonObject.optJSONObject("now");
                        JSONObject cond = now.optJSONObject("cond");
                        code = cond.optString("code");
                        txt = cond.optString("txt");
                    }
                    final String weatherCode = code;
                    final String weatherTxt = txt;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, weatherCode + "-----" + weatherTxt, Toast.LENGTH_SHORT).show();
//                            mLlSwitch.setVisibility(View.GONE);
                            mPlayerView.setVisibility(View.VISIBLE);
                            mRoot.setBackgroundColor(getResources().getColor(android.R.color.black));
                            mWakeLock = wakeUpAndUnlock(MainActivity.this);
                            if (mPlayer == null) {
                                initPlayer();
                            }
                            // Measures bandwidth during playback. Can be null if not required.
                            DefaultBandwidthMeter bandwidthMeters = new DefaultBandwidthMeter();
                            // Produces DataSource instances through which media data is loaded.
                            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(MainActivity.this,
                                    Util.getUserAgent(MainActivity.this, "yourApplicationName"), bandwidthMeters);
                            // This is the MediaSource representing the media to be played.
                            MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                    .createMediaSource(Uri.fromFile(new File("sdcard/Download/earth.flv")));
                            // Prepare the player with the source.
                            mPlayer.prepare(videoSource);
                            mPlayer.addListener(mPlayerEventListener);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    isPlaying = false;
                    simulateClickHome();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public static PowerManager.WakeLock wakeUpAndUnlock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        //获取电源管理器对象
        if (pm != null) {
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
            //获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
            wl.setReferenceCounted(false);
            wl.acquire(0);
            //点亮屏幕

            /*KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            //得到键盘锁管理器对象
            KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unLock");
            //参数是LogCat里用的Tag
            kl.disableKeyguard();*/
            //解锁

        /*
         * 这里写程序的其他代码
         *
         * */

            return wl;
//            kl.reenableKeyguard();
//            //重新启用自动加锁
//            wl.release();
            //释放
        }
        return null;
    }

    private void releaseScreenWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            isPlaying = false;
            mPlayer.stop();
            mPlayer.removeListener(mPlayerEventListener);
            mPlayer.release();
            mPlayer = null;
            mPlayerView.setVisibility(View.GONE);
        }
    }

    private PlayerEventListener mPlayerEventListener = new PlayerEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            super.onPlayerStateChanged(playWhenReady, playbackState);
            if (playbackState == Player.STATE_ENDED) {
                simulateClickHome();
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }
}
