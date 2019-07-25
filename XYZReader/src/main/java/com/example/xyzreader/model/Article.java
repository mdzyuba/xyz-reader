package com.example.xyzreader.model;

import android.text.Html;
import android.text.Spanned;

import java.util.Date;

public class Article {

    private static final String PARAGRAPH_SEPARATOR = "<br\\s*\\/*><br\\s*\\/*>";
    private static final String PARAGRAPH_SEPARATOR_OLD = "\\r\\n\\r\\n";
    private static final String LINE_BREAK = "<br\\s*\\/*>";
    private long itemId;
    private Date publishedDate;
    private String title;
    private String author;
    private String body;
    private String photoUrl;

    private Spanned[] textParagraphs;

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public Date getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(Date publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
        String[] paragraphs = body.split(PARAGRAPH_SEPARATOR);
        Spanned[] formattedParagraphs = new Spanned[paragraphs.length];
        for (int i = 0; i < paragraphs.length; i++) {
            String p = paragraphs[i];
            String line = p.replaceAll(LINE_BREAK, " ");
            formattedParagraphs[i] = Html.fromHtml(line);
        }
        textParagraphs = formattedParagraphs;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Spanned[] getTextParagraphs() {
        return textParagraphs;
    }
}
