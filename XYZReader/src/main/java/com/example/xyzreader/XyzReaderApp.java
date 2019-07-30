package com.example.xyzreader;

import android.app.Application;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

public class XyzReaderApp extends Application {
    private static XyzReaderApp app;
    private volatile OkHttpClient okHttpClient;
    private static final int CACHE_SIZE_BYTES = 512 * 1024 * 1024; // 512 Mb
    private static final String HTTP_CACHE = "http-cache";

    private RequestQueue requestQueue;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public static XyzReaderApp getInstance() {
        return app;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this);
        }
        return requestQueue;
    }

    @NonNull
    public OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            File cacheDir = new File(getApplicationContext().getCacheDir(), HTTP_CACHE);
            Cache cache = new Cache(cacheDir, CACHE_SIZE_BYTES);
            builder.cache(cache);
            builder.addInterceptor(new HttpLoggingInterceptor());
            okHttpClient = builder.build();
        }
        return okHttpClient;
    }
}
