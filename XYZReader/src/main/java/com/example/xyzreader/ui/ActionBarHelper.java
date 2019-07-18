package com.example.xyzreader.ui;

import android.support.v7.app.ActionBar;

import com.example.xyzreader.R;

public class ActionBarHelper {

    public static void initActionBar(ActionBar actionBar) {
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setLogo(R.drawable.logo);
        actionBar.setDisplayUseLogoEnabled(true);
    }
}
