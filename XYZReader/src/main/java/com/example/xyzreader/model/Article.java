package com.example.xyzreader.model;

import java.util.Date;

public class Article {

    private long itemId;
    private Date publishedDate;
    private String title;
    private String author;
    private String body;
    private String photoUrl;
    private float aspectRatio;
    private Integer titleBackground;

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
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public Integer getTitleBackground() {
        return titleBackground;
    }

    public void setTitleBackground(int titleBackground) {
        this.titleBackground = titleBackground;
    }

    @Override
    public String toString() {
        return "Article{" + "itemId=" + itemId + ", publishedDate=" + publishedDate + ", title='" +
               title + '\'' + ", author='" + author + '\'' + ", body='" + body.substring(1, 20) + '\'' +
               ", photoUrl='" + photoUrl + '\'' + ", aspectRatio=" + aspectRatio + '}';
    }

}
