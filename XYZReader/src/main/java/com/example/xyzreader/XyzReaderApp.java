package com.example.xyzreader;

import android.app.Application;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

public class XyzReaderApp extends Application {
    private static XyzReaderApp app;
    private volatile Picasso instance;
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
    public Picasso getPicasso() {
        if (instance == null) {
            Picasso.Builder picassoBuilder =
                    new Picasso.Builder(getApplicationContext()).listener(new Picasso.Listener() {
                        @Override
                        public void onImageLoadFailed(Picasso picasso, Uri uri,
                                                      Exception exception) {
                            Timber.e(exception, "Error loading an image: %s", uri);
                        }
                    });
            OkHttpClient client = getOkHttpClient();
            picassoBuilder.downloader(new OkHttp3Downloader(client));
            instance = picassoBuilder.build();
        }
        return instance;
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
