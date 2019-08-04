package com.example.xyzreader.ui.article;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.Loader;

import com.example.xyzreader.data.ArticleLoader;

import java.util.ArrayList;
import java.util.List;

public class ArticlePageViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Long>> itemIdsLiveData;

    public ArticlePageViewModel(@NonNull Application application) {
        super(application);
        itemIdsLiveData = new MutableLiveData<>();
        loadItemIds();
    }

    private void loadItemIds() {
        ArticleLoader articleLoader = ArticleLoader.newInstanceForAllItemIds(getApplication());
        articleLoader.registerListener(0, new Loader.OnLoadCompleteListener<Cursor>() {
            @Override
            public void onLoadComplete(@NonNull Loader<Cursor> loader, @Nullable Cursor cursor) {
                List<Long> ids = new ArrayList<>();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    Long id = cursor.getLong(0);
                    ids.add(id);
                }
                itemIdsLiveData.postValue(ids);
            }
        });
        articleLoader.registerOnLoadCanceledListener(new Loader.OnLoadCanceledListener<Cursor>() {
            @Override
            public void onLoadCanceled(@NonNull Loader<Cursor> loader) {
                itemIdsLiveData.postValue(new ArrayList<>());
            }
        });
        articleLoader.startLoading();
    }

    public MutableLiveData<List<Long>> getItemIdsLiveData() {
        return itemIdsLiveData;
    }
}
