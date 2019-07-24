package com.example.xyzreader.ui.article;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.ActionBarHelper;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ArticleViewActivity extends AppCompatActivity {

    public static final String START_ID = "startId";
    private long startId;

    private ArticlePageViewModel pageViewModel;

    private ViewPager viewPager;
    private MyPagerAdapter myPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initActionBar();
        initShareButton();

        viewPager = findViewById(R.id.pager);
        myPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(myPagerAdapter);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                startId = ItemsContract.Items.getItemId(getIntent().getData());
                Timber.d("startId: %s", startId);
            }
        }

        initArticlePageViewModel();
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

    private void initArticlePageViewModel() {
        pageViewModel = ViewModelProviders.of(this).get(ArticlePageViewModel.class);
        pageViewModel.getItemIdsLiveData().observe(this, new Observer<List<Long>>() {
            @Override
            public void onChanged(@Nullable List<Long> articleIds) {
                myPagerAdapter.setArticleIds(articleIds);
            }
        });
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        private List<Long> articleIds;

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
            articleIds = new ArrayList<>();
        }

        public void setArticleIds(@NonNull List<Long> articleIds) {
            this.articleIds = articleIds;
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int position) {
            ArticleViewFragment fragment = new ArticleViewFragment();
            Bundle extras = new Bundle();
            extras.putLong(START_ID, articleIds.get(position));
            fragment.setArguments(extras);
            return fragment;
        }

        @Override
        public int getCount() {
            return articleIds.size();
        }
    }
}
