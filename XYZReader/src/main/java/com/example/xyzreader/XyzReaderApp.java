package com.example.xyzreader;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import timber.log.Timber;

public class XyzReaderApp extends Application {
    private static XyzReaderApp app;

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
}
