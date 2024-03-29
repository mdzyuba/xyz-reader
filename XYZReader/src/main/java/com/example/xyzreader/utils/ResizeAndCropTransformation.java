package com.example.xyzreader.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.security.MessageDigest;

public class ResizeAndCropTransformation extends BitmapTransformation {
    private static final String ID = ResizeAndCropTransformation.class.getSimpleName();
    private static final byte[] ID_BYTES = ID.getBytes(Charset.forName("UTF-8"));
    private static final float THRESHOLD = 0.1f;
    private final float aspectRatio;

    public ResizeAndCropTransformation(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    @Override
    public Bitmap transform(@NotNull BitmapPool pool, @NotNull Bitmap toTransform, int outWidth, int outHeight) {
        if (toTransform.getWidth() == outWidth && toTransform.getHeight() == outHeight &&
            isRequiredAspectRatio(outWidth, outHeight)) {
            return toTransform;
        }
        float scale = (float) outWidth / (float) toTransform.getWidth();
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        // First, resize the image to fit the width of the image holder.
        Bitmap scaledBitmap = Bitmap.createBitmap(toTransform, 0, 0,
                                                  toTransform.getWidth(),
                                                  toTransform.getHeight(),
                                                  matrix,
                /*filter=*/ true);
        // Then crop out the image to meet the specified aspect ratio.
        int height = outHeight;
        if (aspectRatio > 0) {
            height = (int) (outWidth / aspectRatio);
        }
        int dy = 0;
        if (height < scaledBitmap.getHeight()) {
            dy = (scaledBitmap.getHeight() - height) / 2;
        }
        if ((dy + height) <= scaledBitmap.getHeight()) {
            Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, 0, dy, outWidth, height);
            return croppedBitmap;
        }
        return scaledBitmap;
    }

    private boolean isRequiredAspectRatio(int outWidth, int outHeight) {
        float aspectRatioOut = (float) outWidth / (float) outHeight;
        return Math.abs(aspectRatioOut - aspectRatio) < THRESHOLD;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResizeAndCropTransformation)) return false;
        ResizeAndCropTransformation that = (ResizeAndCropTransformation) o;
        return Float.compare(that.aspectRatio, aspectRatio) == 0;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(@NotNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }
}
