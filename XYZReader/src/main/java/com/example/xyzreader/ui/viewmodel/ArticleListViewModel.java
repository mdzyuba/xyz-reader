package com.example.xyzreader.ui.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.Loader;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.model.Article;
import com.example.xyzreader.model.ArticleFactory;

import java.util.ArrayList;
import java.util.List;


public class ArticleListViewModel extends AndroidViewModel {

    private ArticleLoader articleLoader;
    private final MutableLiveData<List<Article>> articlesLiveData;

    private final BroadcastReceiver dataRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                boolean isRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                if (!isRefreshing) {
                    loadArticles();
                }
            }
        }
    };

    public ArticleListViewModel(@NonNull Application application) {
        super(application);
        articlesLiveData = new MutableLiveData<>();
        getApplication().registerReceiver(dataRefreshingReceiver,
                         new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
        loadArticles();
    }

    private void loadArticles() {
        articleLoader = ArticleLoader.newAllArticlesInstance(getApplication());
        final Loader.OnLoadCompleteListener<Cursor> onLoadCompleteListener =
                new Loader.OnLoadCompleteListener<Cursor>() {
                    @Override
                    public void onLoadComplete(@NonNull Loader<Cursor> loader,
                                               @Nullable Cursor cursor) {
                        List<Article> articles = new ArrayList<>();
                        if (cursor != null && cursor.moveToFirst()) {
                            while (!cursor.isAfterLast()) {
                                Article article = new ArticleFactory().createArticle(cursor);
                                articles.add(article);
                                cursor.moveToNext();
                            }
                            cursor.close();
                            loader.unregisterListener(this);
                        } else {
                            getApplication().startService(new Intent(getApplication(), UpdaterService.class));
                        }
                        articlesLiveData.postValue(articles);
                    }
                };
        articleLoader.registerListener(0, onLoadCompleteListener);
        Loader.OnLoadCanceledListener<Cursor> cursorOnLoadCanceledListener =
                new Loader.OnLoadCanceledListener<Cursor>() {
                    @Override
                    public void onLoadCanceled(@NonNull Loader<Cursor> loader) {
                        List<Article> articles = new ArrayList<>();
                        articlesLiveData.postValue(articles);
                    }
                };
        articleLoader.registerOnLoadCanceledListener(cursorOnLoadCanceledListener);
        articleLoader.startLoading();
    }

    public LiveData<List<Article>> getArticlesLiveData() {
        return articlesLiveData;
    }
}
