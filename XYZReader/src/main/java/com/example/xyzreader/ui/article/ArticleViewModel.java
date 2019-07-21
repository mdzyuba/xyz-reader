package com.example.xyzreader.ui.article;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.model.Article;
import com.example.xyzreader.model.ArticleFactory;

public class ArticleViewModel extends AndroidViewModel {

    private ArticleLoader articleLoader;
    private MutableLiveData<Article> articleLiveData;

    public ArticleViewModel(@NonNull Application application) {
        super(application);
        articleLiveData = new MutableLiveData<>();
    }

    public void loadArticle(long itemId) {
        articleLoader = ArticleLoader.newInstanceForItemId(getApplication(), itemId);
        articleLoader.registerListener(0, new Loader.OnLoadCompleteListener<Cursor>() {
            @Override
            public void onLoadComplete(@NonNull Loader<Cursor> loader, @Nullable Cursor cursor) {
                if (cursor.moveToFirst() && !cursor.isAfterLast()) {
                    Article article = new ArticleFactory().createArticle(cursor);
                    articleLiveData.postValue(article);
                }
                cursor.close();
            }
        });
        articleLoader.registerOnLoadCanceledListener(new Loader.OnLoadCanceledListener<Cursor>() {
            @Override
            public void onLoadCanceled(@NonNull Loader<Cursor> loader) {
                articleLiveData.postValue(null);
            }
        });
        articleLoader.startLoading();
    }

    public MutableLiveData<Article> getArticleLiveData() {
        return articleLiveData;
    }
}
