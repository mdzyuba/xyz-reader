package com.example.xyzreader.model;

import android.database.Cursor;

import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;

public class ArticleFactory {

    private SimpleDateFormat dateFormat;

    public ArticleFactory() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    }

    public Article createArticle(Cursor cursor) {
        Article article = new Article();
        article.setItemId(cursor.getLong(ArticleLoader.Query._ID));
        article.setPublishedDate(parsePublishedDate(cursor));
        article.setTitle(cursor.getString(ArticleLoader.Query.TITLE));
        article.setAuthor(cursor.getString(ArticleLoader.Query.AUTHOR));
        article.setBody(cursor.getString(ArticleLoader.Query.BODY)
                              .replaceAll("(\r\n|\n)", "<br />"));
        article.setPhotoUrl(cursor.getString(ArticleLoader.Query.PHOTO_URL));
        return article;
    }

    private Date parsePublishedDate(Cursor cursor) {
        try {
            String date = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Timber.e(ex);
            Timber.i( "passing today's date");
            return new Date();
        }
    }
}
