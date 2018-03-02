package com.ccl.perfectisshit.weather.util;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by ccl on 2017/11/6.
 */

public class HttpUtil {


    public static void sendOkHttpRequest(String url, Callback callBack){
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(callBack);
    }
}
