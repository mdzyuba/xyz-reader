package com.example.xyzreader.ui.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.Loader;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.model.Article;
import com.example.xyzreader.model.ArticleFactory;

import java.util.ArrayList;
import java.util.List;


public class ArticleListViewModel extends AndroidViewModel {

    private ArticleLoader articleLoader;
    private final MutableLiveData<List<Article>> articlesLiveData;

    public ArticleListViewModel(@NonNull Application application) {
        super(application);
        articlesLiveData = new MutableLiveData<>();
        loadArticles();
    }

    public void loadArticles() {
        articleLoader = ArticleLoader.newAllArticlesInstance(getApplication());
        articleLoader.registerListener(0, new Loader.OnLoadCompleteListener<Cursor>() {
            @Override
            public void onLoadComplete(@NonNull Loader<Cursor> loader, @Nullable Cursor cursor) {
                if (cursor != null && cursor.moveToFirst()) {
                    List<Article> articles = new ArrayList<>();
                    while (!cursor.isAfterLast()) {
                        Article article = new ArticleFactory().createArticle(cursor);
                        articles.add(article);
                        cursor.moveToNext();
                    }
                    articlesLiveData.postValue(articles);
                    cursor.close();
                }
            }
        });
        articleLoader.registerOnLoadCanceledListener(new Loader.OnLoadCanceledListener<Cursor>() {
            @Override
            public void onLoadCanceled(@NonNull Loader<Cursor> loader) {
                List<Article> articles = new ArrayList<>();
                articlesLiveData.postValue(articles);
            }
        });
        articleLoader.startLoading();
    }

    public LiveData<List<Article>> getArticlesLiveData() {
        return articlesLiveData;
    }
}
