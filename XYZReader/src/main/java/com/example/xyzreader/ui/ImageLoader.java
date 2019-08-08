package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.utils.ResizeAndCropTransformation;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.palette.graphics.Palette;
import timber.log.Timber;

public class ImageLoader {

    public static void loadImage(final Context context,
                                 final String imageUrl,
                                 final AppCompatImageView thumbnail,
                                 final float aspectRatio,
                                 final TitleBackgroundUpdater titleBackgroundUpdater,
                                 final ImageLoadListener imageLoadListener) {

        ResizeAndCropTransformation resizeAndCropTransformation =
                new ResizeAndCropTransformation(aspectRatio);

        Glide.with(context)
             .load(imageUrl)
             .listener(new RequestListener<Drawable>() {
                 @Override
                 public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                             Target<Drawable> target, boolean isFirstResource) {
                     imageLoadListener.onLoadFailed();
                     return false;
                 }

                 @Override
                 public boolean onResourceReady(Drawable resource, Object model,
                                                Target<Drawable> target, DataSource dataSource,
                                                boolean isFirstResource) {
                     if (resource instanceof BitmapDrawable) {
                         updateArticleTitleBackground(context, (BitmapDrawable) resource, titleBackgroundUpdater);
                     }
                     imageLoadListener.onLoadComplete();
                     return false;
                 }
             })
             .apply(new RequestOptions().placeholder(R.drawable.image_placeholder))
             .transform(resizeAndCropTransformation)
             .into(thumbnail);
    }

    public static abstract class TitleBackgroundUpdater {
        public abstract void setBackgroundColor(int color);
    }

    public interface ImageLoadListener {
        void onLoadComplete();
        void onLoadFailed();
    }

    private static void updateArticleTitleBackground(final Context context,
                                                     BitmapDrawable bitmapDrawable,
                                                     final TitleBackgroundUpdater titleBackgroundUpdater) {
        if (bitmapDrawable.getBitmap() != null) {
            Bitmap bitmap = bitmapDrawable.getBitmap();
            Palette.from(bitmap).maximumColorCount(12)
                   .generate(new Palette.PaletteAsyncListener() {
                       @Override
                       public void onGenerated(@Nullable Palette palette) {
                           if (palette == null) {
                               Timber.e("The palette is null");
                               return;
                           }
                           int mutedColor = context.getResources()
                                                   .getColor(R.color.muted_color);
                           mutedColor = palette.getDarkMutedColor(mutedColor);
                           titleBackgroundUpdater.setBackgroundColor(mutedColor);
                       }
                   });
        }
    }
}
