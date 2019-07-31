package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.AppCompatImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.request.RequestOptions;
import com.example.xyzreader.R;
import com.example.xyzreader.utils.ResizeAndCropTransformation;

import java.security.MessageDigest;

import timber.log.Timber;

public class ImageLoader {

    public static void loadImage(final Context context,
                                 final String imageUrl,
                                 final AppCompatImageView thumbnail,
                                 final float aspectRatio,
                                 final TitleBackgroundUpdater titleBackgroundUpdater) {

        ResizeAndCropTransformation resizeAndCropTransformation =
                new ResizeAndCropTransformation(aspectRatio);

        Transformation<Bitmap> bitmapPaletteTransformation = new Transformation<Bitmap>() {
            @NonNull
            @Override
            public Resource<Bitmap> transform(@NonNull Context context,
                                              @NonNull Resource<Bitmap> resource,
                                              int outWidth,
                                              int outHeight) {
                final Bitmap bitmap = Bitmap.createBitmap(resource.get());
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        if (titleBackgroundUpdater != null) {
                            titleBackgroundUpdater.updateCardBackground(bitmap);
                        }
                        Timber.d("outWidth: %d, outHeight: %d", outWidth, outHeight);
                        return null;
                    }
                }.execute();
                return resource;
            }

            @Override
            public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {

            }
        };

        Glide.with(context)
             .load(imageUrl)
             .apply(new RequestOptions().placeholder(R.drawable.image_placeholder))
             .transform(new MultiTransformation(resizeAndCropTransformation,
                                                bitmapPaletteTransformation))
             .into(thumbnail);
    }

    public static abstract class TitleBackgroundUpdater {
        private int mutedColor = 0xFF333333;

        void updateCardBackground(Bitmap bitmap) {
            Palette.from(bitmap).maximumColorCount(12).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@Nullable Palette palette) {
                    if (palette == null) {
                        Timber.e("The palette is null");
                        return;
                    }
                    mutedColor = palette.getDarkMutedColor(0xFF333333);
                    setBackgroundColor(mutedColor);
                    Timber.d("mutedColor: %d", mutedColor);
                }
            });
        }

        public abstract void setBackgroundColor(int color);
    }

}
