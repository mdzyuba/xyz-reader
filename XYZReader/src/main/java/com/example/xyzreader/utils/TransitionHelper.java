package com.example.xyzreader.utils;

import android.content.Context;

import com.example.xyzreader.R;

import org.jetbrains.annotations.NotNull;

public class TransitionHelper {

    @NotNull public static String createUniqueTransitionName(Context context, long articleId) {
        StringBuilder sb = new StringBuilder();
        String transitionName = context.getResources().getString(R.string.article_photo);
        sb.append(transitionName).append("-").append(articleId);
        return sb.toString();
    }
}
