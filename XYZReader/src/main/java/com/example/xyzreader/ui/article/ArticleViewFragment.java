package com.example.xyzreader.ui.article;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.XyzReaderApp;
import com.example.xyzreader.model.Article;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import timber.log.Timber;

public class ArticleViewFragment extends Fragment {

    private long startId;

    private ArticleViewModel viewModel;
    private LinearLayout titleBarContainer;

    private int mMutedColor = 0xFF333333;

    public ArticleViewFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_article_view, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle arguments = getArguments();
        startId = arguments.getLong(ArticleViewActivity.START_ID);
        initViewModel(startId);
    }

    private void initViewModel(long startId) {
        viewModel = ViewModelProviders.of(getActivity()).get(ArticleViewModel.class);
        viewModel.getArticleLiveData().observe(getViewLifecycleOwner(), new Observer<Article>() {
            @Override
            public void onChanged(@Nullable Article article) {
                Timber.d("Article is ready");
                initArticlePage(article);
            }
        });
        viewModel.loadArticle(startId);
    }

    private void initArticlePage(@Nullable Article article) {
        TextView title = getView().findViewById(R.id.article_title);
        title.setText(article.getTitle());
        TextView bylineView = getView().findViewById(R.id.article_byline);
        titleBarContainer = getView().findViewById(R.id.meta_bar);

        loadToolbarImage(article.getPhotoUrl());

        Spanned text = new ArticleUI().formatDateAndAuthor(article.getAuthor(),
                                                           article.getPublishedDate());
        bylineView.setText(text);
    }

    private void initTitleBarBackground(Bitmap bitmap) {
        Palette.from(bitmap).maximumColorCount(12).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                mMutedColor = palette.getDarkMutedColor(0xFF333333);
                titleBarContainer.setBackgroundColor(mMutedColor);
            }
        });
    }

    void loadToolbarImage(String imageUrl) {
        ImageView imageView = getActivity().findViewById(R.id.toolbar_image);
        if (imageView == null) {
            Timber.e("The image is not found");
            return;
        }
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (bitmap == null && titleBarContainer == null) {
                    return;
                }
                imageView.setImageBitmap(bitmap);
                initTitleBarBackground(bitmap);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Timber.e(e);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
        XyzReaderApp.getInstance().getPicasso()
                    .load(imageUrl)
                    .resize(imageView.getWidth(), imageView.getHeight())
                    .centerCrop()
                    .placeholder(R.drawable.image_placeholder)
                    .into(target);
    }

}
