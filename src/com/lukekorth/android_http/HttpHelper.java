
package com.lukekorth.android_http;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.google.gson.Gson;
import com.integralblue.httpresponsecache.HttpResponseCache;

import org.apache.http.NameValuePair;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

public class HttpHelper {

    public static final int NO_CACHE = 0;
    public static final int VALIDATE_CACHE = 1;

    private static final String TAG = "android-http";
    private static final String UAS = "android-http";
    private boolean DEBUG_HTTP;

    private Context mContext;
    private static Gson gson = null;
    private int connectTimeout = 10 * 1000; // 10 seconds in milliseconds
    private int readTimeout = 60 * 1000; // 60 seconds in milliseconds

    public HttpHelper(Context context) {
        Init(context, (long) 10); // 10 MiB
    }

    public HttpHelper(Context context, long size) {
        Init(context, size);
    }

    private void Init(Context context, long size) {
        mContext = context;

        DEBUG_HTTP = IsDebug();

        try {
            HttpResponseCache.install(new File(context.getCacheDir(), "http"), size * 1024 * 1024);
        } catch (IOException e) {
            if (DEBUG_HTTP)
                Log.w(TAG, "IOException occured while getting cache dir " + e);
        }
    }

    public void flush() {
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }

    public static Gson getGson() {
        if (gson == null)
            gson = new Gson();

        return gson;
    }

    public void setConnectTimeout(int seconds) {
        connectTimeout = seconds * 1000;
    }

    public void setReadTimeout(int seconds) {
        readTimeout = seconds * 1000;
    }

    public String get(String url) {
        return get(url, -1);
    }

    public String get(String url, int cache) {
        HttpURLConnection urlConnection = null;
        String response = null;

        if (DEBUG_HTTP) {
            Log.d(TAG, "url: " + url);
        }

        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();

            urlConnection.setConnectTimeout(connectTimeout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setRequestProperty("User-Agent", UAS);

            if (cache == NO_CACHE) {
                urlConnection.addRequestProperty("Cache-Control", "no-cache");
            } else if (cache == VALIDATE_CACHE) {
                urlConnection.addRequestProperty("Cache-Control", "max-age=0");
            }

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            response = readStream(in);

            if (DEBUG_HTTP) {
                Log.d(TAG, "response code: " + urlConnection.getResponseCode());
                Log.d(TAG, "response payload: " + response);
            }
        } catch (MalformedURLException e) {
            if (DEBUG_HTTP)
                Log.w(TAG, "MalformedURLException occured while parsing url " + e);
        } catch (IOException e) {
            if (DEBUG_HTTP) {
                response = readStream(urlConnection.getErrorStream());
                Log.d(TAG, "error response: " + response);

                Log.w(TAG,
                        "IOException occured while trying to open connection or getting input stream. "
                                + e);
            }
        } finally {
            urlConnection.disconnect();
        }

        return response;
    }

    public String get(String url, List<NameValuePair> nameValuePairs) {
        return get(url, nameValuePairs, -1);
    }

    public String get(String url, List<NameValuePair> nameValuePairs, int cache) {
        url = url + "?" + getParameters(nameValuePairs);

        if (DEBUG_HTTP) {
            Log.d(TAG, "query string: " + nameValuePairs.toString());
        }

        return get(url, cache);
    }

    @SuppressWarnings("rawtypes")
    public <T> T get(String url, Class type) {
        return get(url, type, -1);
    }

    @SuppressWarnings({
            "unchecked", "rawtypes"
    })
    public <T> T get(String url, Class type, int cache) {
        if (gson == null) {
            gson = new Gson();
        }

        return (T) gson.fromJson(get(url, cache), type);
    }

    @SuppressWarnings("rawtypes")
    public <T> T get(String url, List<NameValuePair> nameValuePairs, Class type) {
        return get(url, nameValuePairs, type, -1);
    }

    @SuppressWarnings({
            "unchecked", "rawtypes"
    })
    public <T> T get(String url, List<NameValuePair> nameValuePairs, Class type, int cache) {
        if (gson == null) {
            gson = new Gson();
        }

        return (T) gson.fromJson(get(url, nameValuePairs, cache), type);
    }

    public String getCached(String url) {
        HttpURLConnection urlConnection = null;
        String response = null;

        if (DEBUG_HTTP) {
            Log.d(TAG, "url: " + url);
        }

        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.addRequestProperty("Cache-Control", "only-if-cached");

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            response = readStream(in);
        } catch (FileNotFoundException e) {
            if (DEBUG_HTTP)
                Log.w(TAG, "The requested resource was not cached");

            response = null;
        } catch (IOException e) {
            if (DEBUG_HTTP)
                Log.w(TAG,
                        "IOException occured while trying to open connection or getting input stream. "
                                + e);
        } finally {
            urlConnection.disconnect();
        }

        return response;
    }

    public String getCached(String url, List<NameValuePair> nameValuePairs) {
        url = url + "?" + getParameters(nameValuePairs);

        if (DEBUG_HTTP) {
            Log.d(TAG, "Attempting to load directly from cache, return null if not cached");
            Log.d(TAG, "query string: " + nameValuePairs.toString());
        }

        return getCached(url);
    }

    @SuppressWarnings({
            "rawtypes", "unchecked"
    })
    public <T> T getCached(String url, List<NameValuePair> nameValuePairs, Class type) {
        if (gson == null) {
            gson = new Gson();
        }

        return (T) gson.fromJson(getCached(url, nameValuePairs), type);
    }

    public String post(String url, List<NameValuePair> nameValuePairs) {
        HttpURLConnection urlConnection = null;
        String response = null;
        String urlParameters = getParameters(nameValuePairs);

        if (DEBUG_HTTP) {
            Log.d(TAG, "url: " + url);
            Log.d(TAG, "query string: " + nameValuePairs.toString());
        }

        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();

            urlConnection.setConnectTimeout(connectTimeout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setRequestProperty("User-Agent", UAS);

            urlConnection.addRequestProperty("Cache-Control", "no-cache");

            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("Content-Length",
                    "" + Integer.toString(urlParameters.getBytes().length));

            DataOutputStream os = new DataOutputStream(urlConnection.getOutputStream());
            os.writeBytes(urlParameters);
            os.flush();
            os.close();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            response = readStream(in);

            if (DEBUG_HTTP) {
                Log.d(TAG, "response code: " + urlConnection.getResponseCode());
                Log.d(TAG, "response payload: " + response);
            }
        } catch (MalformedURLException e) {
            if (DEBUG_HTTP)
                Log.w(TAG, "MalformedURLException occured while parsing url " + e);
        } catch (IOException e) {
            if (DEBUG_HTTP) {
                response = readStream(urlConnection.getErrorStream());
                Log.d(TAG, "error response: " + response);

                Log.w(TAG,
                        "IOException occured while trying to open connection or getting input stream. "
                                + e);
            }
        } finally {
            urlConnection.disconnect();
        }

        return response;
    }

    @SuppressWarnings({
            "unchecked", "rawtypes"
    })
    public <T> T post(String url, List<NameValuePair> nameValuePairs, Class type) {
        if (gson == null) {
            gson = new Gson();
        }

        return (T) gson.fromJson(post(url, nameValuePairs), type);
    }

    private String getParameters(List<NameValuePair> nameValuePairs) {
        String parameters = "";

        for (NameValuePair pair : nameValuePairs) {
            try {
                parameters += pair.getName() + "=" + URLEncoder.encode(pair.getValue(), "UTF-8")
                        + "&";
            } catch (UnsupportedEncodingException e) {
                if (DEBUG_HTTP)
                    Log.w(TAG, "UnsupportedEncodingException while url encoding parameters " + e);
            }
        }

        return parameters.substring(0, parameters.length() - 1);
    }

    private String readStream(InputStream in) {
        InputStreamReader is = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(is);
        StringBuilder sb = new StringBuilder();

        try {
            String read = br.readLine();

            while (read != null) {
                sb.append(read);
                read = br.readLine();
            }
        } catch (IOException e) {
            if (DEBUG_HTTP)
                Log.w(TAG, "IOException occured while reading response from server " + e);
        }

        try {
            in.close();
            is.close();
            br.close();
        } catch (IOException e) {
            if (DEBUG_HTTP)
                Log.w(TAG, "IOException occured while closing streams " + e);
        }

        return sb.toString();
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
            Log.w(TAG, "NameNotFoundException, DEBUG_HTTP will remain false  " + e);
        }

        return debuggable;
    }

}
