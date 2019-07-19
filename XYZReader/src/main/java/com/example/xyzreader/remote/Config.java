package com.example.xyzreader.remote;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {
    public static final URL BASE_URL;
    private static String TAG = Config.class.toString();

    public static final String READER_JSON = "https://go.udacity.com/xyz-reader-json";
    public static final String READER_JSON_2 = "https://raw.githubusercontent.com/SuperAwesomeness/XYZReader/master/data.json";

    static {
        URL url = null;
        try {
            url = new URL(READER_JSON_2);
        } catch (MalformedURLException ignored) {
            // TODO: throw a real error
            Log.e(TAG, "Please check your internet connection.");
        }

        BASE_URL = url;
    }
}
