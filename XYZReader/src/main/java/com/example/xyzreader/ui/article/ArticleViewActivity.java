package com.example.xyzreader.ui.article;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.XyzReaderApp;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.model.Article;
import com.example.xyzreader.ui.ActionBarHelper;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import timber.log.Timber;

public class ArticleViewActivity extends AppCompatActivity {

    public static final String START_ID = "startId";
    private long startId;

    private ArticleViewModel viewModel;
    private LinearLayout titleBarContainer;

    private int mMutedColor = 0xFF333333;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initActionBar();
        initShareButton();

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                startId = ItemsContract.Items.getItemId(getIntent().getData());
                Timber.d("startId: %s", startId);
            }
        }

        ArticleViewFragment fragment = new ArticleViewFragment();
        Bundle extras = new Bundle();
        extras.putLong(START_ID, startId);
        fragment.setArguments(extras);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragment_holder, fragment, "foo").commit();
//        initViewModel();
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            ActionBarHelper.initActionBar(actionBar);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initShareButton() {
        FloatingActionButton fab = findViewById(R.id.fab);
        View.OnClickListener shareButtonOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(
                        ShareCompat.IntentBuilder.from(ArticleViewActivity.this)
                                                 .setType("text/plain")
                                                 .setText("Some sample text").getIntent(),
                        getString(R.string.action_share)));
            }
        };
        fab.setOnClickListener(shareButtonOnClickListener);
    }

    private void initViewModel() {
        viewModel = ViewModelProviders.of(this).get(ArticleViewModel.class);
        viewModel.getArticleLiveData().observe(this, new Observer<Article>() {
            @Override
            public void onChanged(@Nullable Article article) {
                Timber.d("Article is ready");
//                initArticlePage(article);
            }
        });
        viewModel.loadArticle(startId);
    }

//    private void initArticlePage(@Nullable Article article) {
//        TextView title = findViewById(R.id.article_title);
//        title.setText(article.getTitle());
//        TextView bylineView = findViewById(R.id.article_byline);
//        titleBarContainer = findViewById(R.id.meta_bar);
//
//        loadToolbarImage(article.getPhotoUrl());
//
//        Spanned text = new ArticleUI().formatDateAndAuthor(article.getAuthor(),
//                                                           article.getPublishedDate());
//        bylineView.setText(text);
//    }

    void loadToolbarImage(String imageUrl) {
        ImageView imageView = findViewById(R.id.toolbar_image);
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

    private void initTitleBarBackground(Bitmap bitmap) {
        Palette.from(bitmap).maximumColorCount(12).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                mMutedColor = palette.getDarkMutedColor(0xFF333333);
                titleBarContainer.setBackgroundColor(mMutedColor);
            }
        });
    }

}
