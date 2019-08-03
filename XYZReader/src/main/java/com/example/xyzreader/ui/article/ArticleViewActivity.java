package com.example.xyzreader.ui.article;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.ActionBarHelper;
import com.example.xyzreader.ui.ArticleListActivity;
import com.example.xyzreader.utils.TransitionHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class ArticleViewActivity extends AppCompatActivity implements IActionBarUpdater {

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

        viewPager = findViewById(R.id.pager);
        viewPager.setOffscreenPageLimit(2);
        myPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(myPagerAdapter);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                startId = ItemsContract.Items.getItemId(getIntent().getData());
                Timber.d("onCreate ArticleViewActivity with the startId: %s", startId);
            }
        }

        initArticlePageViewModel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
        }
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            ActionBarHelper.initActionBar(actionBar);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initArticlePageViewModel() {
        pageViewModel = ViewModelProviders.of(this).get(ArticlePageViewModel.class);
        pageViewModel.getItemIdsLiveData().observe(this, new Observer<List<Long>>() {
            @Override
            public void onChanged(@Nullable final List<Long> articleIds) {
                if (articleIds == null) {
                    myPagerAdapter.setArticleIds(new ArrayList<>());
                    return;
                }
                myPagerAdapter.setArticleIds(articleIds);

                long articleId = ArticleViewActivity.this.startId;
                scrollToArticle(articleIds, articleId);

                SharedElementCallback sharedElementCallback = createSharedElementCallback();
                setEnterSharedElementCallback(sharedElementCallback);
            }
        });
    }

    private void scrollToArticle(@NotNull List<Long> articleIds, long articleId) {
        final int index = articleIds.indexOf(articleId);
        Timber.d("startId: %s, index: %s", articleId, index);
        if (index >= 0) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    int currentItem = viewPager.getCurrentItem();
                    long articleId = myPagerAdapter.articleIds.get(currentItem);
                    Timber.d("Current item: %d, article id: %d", currentItem, articleId);
                    long nextArticleId = myPagerAdapter.articleIds.get(index);
                    Timber.d("Calling setCurrentItem with index: %d, article id: %d", index, nextArticleId);
                    viewPager.setCurrentItem(index, false);
                }
            });
        }
    }

    @NotNull
    private SharedElementCallback createSharedElementCallback() {
        return new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names,
                                            Map<String, View> sharedElements) {
                int currentItem = viewPager.getCurrentItem();
                long articleId = myPagerAdapter.articleIds.get(currentItem);
                ArticleViewFragment fragment = getFragmentAtPosition(currentItem);
                View fragmentView = fragment.getView();
                if (fragmentView == null) {
                    return;
                }
                String transitionName = TransitionHelper
                        .createUniqueTransitionName(getApplicationContext(), articleId);
                AppCompatImageView value = ArticleViewFragment.getImageView(fragmentView);
                sharedElements.put(transitionName, value);
            }
        };
    }

    @NotNull
    private ArticleViewFragment getFragmentAtPosition(int currentItem) {
        return (ArticleViewFragment) viewPager.getAdapter().instantiateItem(viewPager, currentItem);
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
            Long articleId = articleIds.get(position);
            Timber.d("position: %d, articleId: %d", position, articleId);
            ArticleViewFragment fragment = ArticleViewFragment.newInstance(articleId);
            return fragment;
        }

        @Override
        public int getCount() {
            return articleIds.size();
        }


        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            return super.instantiateItem(container, position);
        }
    }

    @Override
    public void updateActionBar(Toolbar toolbar) {
        setSupportActionBar(toolbar);

        initActionBar();

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.d("onOptionsItemSelected");
                navigateUpTo(new Intent(getApplicationContext(), ArticleListActivity.class));
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(getApplicationContext(), ArticleListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
