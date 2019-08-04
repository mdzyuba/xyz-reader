package com.example.xyzreader.ui.viewmodel;


import com.example.xyzreader.model.Article;
import com.example.xyzreader.ui.ArticleListActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ArticleListViewModelTest {

    public static final int TIMEOUT = 2; // seconds

    @Rule
    public ActivityTestRule<ArticleListActivity> activityRule =
            new ActivityTestRule<>(ArticleListActivity.class);

    @Test
    public void getArticlesLiveData() throws Throwable {
        final ArticleListActivity activity = activityRule.getActivity();

        final CountDownLatch latch = new CountDownLatch(1);
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArticleListViewModel viewModel =
                        ViewModelProviders.of(activity).get(ArticleListViewModel.class);

                final Observer<List<Article>> observer = new Observer<List<Article>>() {
                    @Override
                    public void onChanged(List<Article> articles) {
                        viewModel.getArticlesLiveData().removeObserver(this);

                        assertNotNull(articles);

                        assertTrue("Expecting several articles, received: " + articles.size(),
                                   articles.size() > 1);

                        latch.countDown();
                    }
                };
                viewModel.getArticlesLiveData().observe(activity, observer);
            }
        });
        assertThat("The view model observer callback has not been called.",
                   latch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
    }
}