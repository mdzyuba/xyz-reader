package com.example.xyzreader.ui.article;

import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A helper class to format an Article UI elements.
 */
public class ArticleUI {
    public static final String PARAGRAPH_SEPARATOR = "<br\\s*\\/*><br\\s*\\/*>";
    public static final String LINE_BREAK = "<br\\s*\\/*>";
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    // Use default locale format
    private SimpleDateFormat outputFormat;

    public ArticleUI() {
        outputFormat = new SimpleDateFormat();
    }

    public Spanned formatDateAndAuthor(String author, Date publishedDate) {
        Spanned text;
        String date = formatDate(publishedDate);
        text = Html.fromHtml(date + " by <font color='#ffffff'>" + author + "</font>");
        return text;
    }

    public String formatDate(Date publishedDate) {
        String date;
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            date = DateUtils
                    .getRelativeTimeSpanString(publishedDate.getTime(), System.currentTimeMillis(),
                                               DateUtils.HOUR_IN_MILLIS,
                                               DateUtils.FORMAT_ABBREV_ALL).toString();
        } else {
            // If date is before 1902, just show the string
            date = outputFormat.format(publishedDate);
        }
        return date;
    }

}
