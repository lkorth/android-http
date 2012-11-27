package com.lukekorth.android_http;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.google.gson.Gson;
import com.integralblue.httpresponsecache.HttpResponseCache;

import java.io.File;
import java.io.IOException;

public class HttpHelper {

    private static final String TAG = "android-http";

    private static Gson gson = null;
    private Context mContext;
    private boolean DEBUG_HTTP;

    public HttpHelper(Context context) {
        Init(context, (long) 10 * 1024 * 1024); // 10 MiB
    }

    public HttpHelper(Context context, long size) {
        Init(context, size);
    }

    private void Init(Context context, long size) {
        mContext = context;

        DEBUG_HTTP = IsDebug();

        try {
            HttpResponseCache.install(new File(context.getCacheDir(), "http"), size);
        } catch (IOException e) {
            if (DEBUG_HTTP)
                Log.w(TAG, "IOException when getting cache dir");
        }
    }

    public static Gson getGson() {
        if (gson == null)
            gson = new Gson();

        return gson;
    }


    // http://izvornikod.com/Blog/tabid/82/EntryId/13/How-to-check-if-your-android-application-is-running-in-debug-or-release-mode.aspx
    private boolean IsDebug() {
        boolean debuggable = false;

        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo appinfo = pm.getApplicationInfo(mContext.getPackageName(), 0);
            debuggable = (0 != (appinfo.flags &= ApplicationInfo.FLAG_DEBUGGABLE));
        } catch (NameNotFoundException e) {
            /* debuggable variable will remain false */
        }

        return debuggable;
    }

}
