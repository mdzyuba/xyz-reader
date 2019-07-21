package com.example.xyzreader.ui.article;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.model.Article;
import com.example.xyzreader.ui.ActionBarHelper;
import com.squareup.picasso.Picasso;

import timber.log.Timber;

public class ArticleViewActivity extends AppCompatActivity {

    private long startId;

    private ArticleViewModel viewModel;

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

        initViewModel();
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
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private void initViewModel() {
        viewModel = ViewModelProviders.of(this).get(ArticleViewModel.class);
        viewModel.getArticleLiveData().observe(this, new Observer<Article>() {
            @Override
            public void onChanged(@Nullable Article article) {
                Timber.d("Article is ready");
                loadToolbarImage(article.getPhotoUrl());
            }
        });
        viewModel.loadArticle(startId);
    }

    void loadToolbarImage(String imageUrl) {
        ImageView imageView = findViewById(R.id.toolbar_image);
        Picasso.get()
               .load(imageUrl)
               .resize(imageView.getWidth(), imageView.getHeight())
               .centerCrop()
               .into(imageView);
    }
}
