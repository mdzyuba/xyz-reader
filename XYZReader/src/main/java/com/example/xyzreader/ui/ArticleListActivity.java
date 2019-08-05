package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.Spanned;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.model.Article;
import com.example.xyzreader.ui.article.ArticleUI;
import com.example.xyzreader.ui.viewmodel.ArticleListViewModel;
import com.example.xyzreader.utils.TransitionHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.SharedElementCallback;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import timber.log.Timber;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link com.example.xyzreader.ui.article.ArticleViewActivity} representing
 * item details. On tablets, the activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private AtomicBoolean enterTransitionStarted;

    private ArticleListViewModel articleListViewModel;
    private Adapter adapter;
    private boolean mIsRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            ActionBarHelper.initActionBar(getSupportActionBar());
        }

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mRecyclerView = findViewById(R.id.recycler_view);

        adapter = new Adapter();
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        articleListViewModel = ViewModelProviders.of(this).get(ArticleListViewModel.class);

        articleListViewModel.getArticlesLiveData().observe(this, new Observer<List<Article>>() {
            @Override
            public void onChanged(List<Article> articles) {
                adapter.setArticles(articles);
            }
        });

        enterTransitionStarted = new AtomicBoolean();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Context context = ArticleListActivity.this;
            Transition exitTransition =
                    TransitionInflater.from(context).inflateTransition(R.transition.grid_exit);
            getWindow().setExitTransition(exitTransition);
            // Postponing the enter transitions until the views are loaded.
            postponeEnterTransition();
        }

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private final BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        Timber.d("update UI");
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        List<Article> articles;

        Adapter() {
            articles = new ArrayList<>();
        }

        void setArticles(List<Article> articles) {
            this.articles = articles;
            notifyDataSetChanged();
        }

        @Override
        public long getItemId(int position) {
            return articles.get(position).getItemId();
        }

        @NotNull
        @Override
        public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            final View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = vh.getAdapterPosition();
                    long articleId = getItemId(position);
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                                               ItemsContract.Items.buildItemUri(articleId));

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                        String transitionName = TransitionHelper
                                .createUniqueTransitionName(getApplicationContext(), articleId);
                        vh.thumbnailView.setTransitionName(transitionName);

                        getWindow().getExitTransition().excludeTarget(vh.thumbnailView, true);

                        setExitSharedElementCallback(new SharedElementCallback() {
                            @Override
                            public void onMapSharedElements(List<String> names,
                                                            Map<String, View> sharedElements) {
                                String transitionName = TransitionHelper
                                        .createUniqueTransitionName(getApplicationContext(), articleId);
                                AppCompatImageView value = vh.thumbnailView;
                                sharedElements.put(transitionName, value);
                            }
                        });

                        ActivityOptionsCompat activityOptions = ActivityOptionsCompat
                                .makeSceneTransitionAnimation(ArticleListActivity.this, view,
                                                              transitionName);
                        startActivity(intent, activityOptions.toBundle());
                    } else {
                        startActivity(intent);
                    }
                }
            };
            view.setOnClickListener(clickListener);
            return vh;
        }

        @Override
        public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
            holder.onBind(ArticleListActivity.this, articles.get(position));
        }

        @Override
        public int getItemCount() {
            return articles.size();
        }

    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout itemHolder;
        final AppCompatImageView thumbnailView;
        final TextView titleView;
        final TextView subtitleView;

        ViewHolder(View view) {
            super(view);
            itemHolder = view.findViewById(R.id.item_holder);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }

        void onBind(Context context, Article article) {
            titleView.setText(article.getTitle());
            Spanned text = new ArticleUI().formatDateAndAuthor(article.getAuthor(),
                                                               article.getPublishedDate());
            subtitleView.setText(text);
            Timber.d("image url: %s", article.getPhotoUrl());

            ImageLoader.TitleBackgroundUpdater titleBackgroundUpdater = new ImageLoader.TitleBackgroundUpdater() {
                @Override
                public void setBackgroundColor(int color) {
                    itemHolder.setBackgroundColor(color);
                }
            };

            ImageLoader.ImageLoadListener loadListener = new ImageLoader.ImageLoadListener() {
                @Override
                public void onLoadComplete() {
                    resumePostponedEnterTransition();
                }

                @Override
                public void onLoadFailed() {
                    resumePostponedEnterTransition();
                }
            };

            ImageLoader.loadImage(context, article.getPhotoUrl(), thumbnailView,
                                  article.getAspectRatio(), titleBackgroundUpdater, loadListener);
        }
    }

    private void resumePostponedEnterTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // TODO: add a check for the selected position
            if (enterTransitionStarted.getAndSet(true)) {
                return;
            }
            ArticleListActivity.this.startPostponedEnterTransition();
        }
    }

}
