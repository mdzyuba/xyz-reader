package com.example.xyzreader.ui.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import android.database.Cursor;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.Loader;
import android.text.Html;
import android.text.Spanned;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.model.Article;
import com.example.xyzreader.model.ArticleFactory;
import com.example.xyzreader.ui.article.ArticleUI;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ArticleViewModel extends AndroidViewModel {

    private ArticleLoader articleLoader;
    private final MutableLiveData<String> photoUrlLiveData;
    private final MutableLiveData<Article> articleLiveData;
    private final ArrayList<Spanned> articleParagraphs;
    private final MutableLiveData<List<Spanned>> articleParagraphsLiveData;

    public ArticleViewModel(@NonNull Application application) {
        super(application);
        articleLiveData = new MutableLiveData<>();
        articleParagraphs = new ArrayList<>();
        articleParagraphsLiveData = new MutableLiveData<>();
        photoUrlLiveData = new MutableLiveData<>();
    }

    public void loadArticle(long itemId) {
        Timber.d("loadArticle: %d", itemId);
        articleLoader = ArticleLoader.newInstanceForItemId(getApplication(), itemId);
        articleLoader.registerListener(0, new Loader.OnLoadCompleteListener<Cursor>() {
            @Override
            public void onLoadComplete(@NonNull Loader<Cursor> loader, @Nullable Cursor cursor) {
                if (cursor != null) {
                    if (cursor.moveToFirst() && !cursor.isAfterLast()) {
                        Article article = new ArticleFactory().createArticle(cursor);
                        photoUrlLiveData.postValue(article.getPhotoUrl());
                        articleLiveData.postValue(article);
                        Timber.d("Article is loaded: %d, %s", article.getItemId(),
                                 article.getTitle());
                        new ArticleFormatterTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                                                     article.getBody());
                    }
                    cursor.close();
                }
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

    public MutableLiveData<List<Spanned>> getArticleParagraphsLiveData() {
        return articleParagraphsLiveData;
    }

    class ArticleFormatterTask extends AsyncTask<String, Void, List<Spanned> > {

        @Override
        protected List<Spanned>  doInBackground(String... strings) {
            String articleBody = strings[0];
            String[] paragraphs = articleBody.split(ArticleUI.PARAGRAPH_SEPARATOR);
            Spanned[] formattedParagraphs = new Spanned[paragraphs.length];
            for (int i = 0; i < paragraphs.length; i++) {
                String p = paragraphs[i];
                String line = p.replaceAll(ArticleUI.LINE_BREAK, " ");
                formattedParagraphs[i] = Html.fromHtml(line);
                articleParagraphs.add(formattedParagraphs[i]);
            }
            return articleParagraphs;
        }

        @Override
        protected void onPostExecute(List<Spanned> spanneds) {
            articleParagraphsLiveData.postValue(spanneds);
        }
    }
}
