package com.lukekorth.android_http;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.integralblue.httpresponsecache.HttpResponseCache;

import java.io.File;
import java.io.IOException;

public class HttpHelper {

    private static Gson gson = null;

    public HttpHelper(Context context) {
        new HttpHelper(context, (long) 10 * 1024 * 1024); // 10 MiB
    }

    public HttpHelper(Context context, long size) {
        try {
            HttpResponseCache.install(new File(context.getCacheDir(), "http"), size);
        } catch (IOException e) {
            Log.w("android-http", "IOException when getting cache dir");
        }
    }

    public static Gson getGson() {
        if (gson == null)
            gson = new Gson();

        return gson;
    }

}
