package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.core.app.SharedElementCallback;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.utils.TransitionHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link com.example.xyzreader.ui.article.ArticleViewActivity} representing
 * item details. On tablets, the activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private AtomicBoolean enterTransitionStarted;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private static final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private static final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarHelper.initActionBar(getSupportActionBar());

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mRecyclerView = findViewById(R.id.recycler_view);

        // TODO: Replace with a ViewModel
        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }

        enterTransitionStarted = new AtomicBoolean();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Context context = ArticleListActivity.this;
            Transition exitTransition =
                    TransitionInflater.from(context).inflateTransition(R.transition.grid_exit);
            getWindow().setExitTransition(exitTransition);
            // Postponing the enter transitions until the views are loaded.
            postponeEnterTransition();
        }
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

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            String title = mCursor.getString(ArticleLoader.Query.TITLE);
            String url = mCursor.getString(ArticleLoader.Query.THUMB_URL);
            String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
            float aspectRatio = mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO);
            Date publishedDate = parsePublishedDate(mCursor);

            holder.onBind(ArticleListActivity.this, title, url, author, aspectRatio, publishedDate);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }

    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout itemHolder;
        AppCompatImageView thumbnailView;
        TextView titleView;
        TextView subtitleView;

        ViewHolder(View view) {
            super(view);
            itemHolder = view.findViewById(R.id.item_holder);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }

        void onBind(Context context, String title, String url, String author,
                    float aspectRatio, Date publishedDate) {
            titleView.setText(title);
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                        + "<br/>" + " by "
                        + author));
            } else {
                subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + author));
            }
            Timber.d("image url: %s", url);

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

            ImageLoader.loadImage(context, url, thumbnailView,
                                  aspectRatio, titleBackgroundUpdater, loadListener);
        }
    }

    void resumePostponedEnterTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // TODO: add a check for the selected position
            if (enterTransitionStarted.getAndSet(true)) {
                return;
            }
            ArticleListActivity.this.startPostponedEnterTransition();
        }
    }

    private Date parsePublishedDate(Cursor cursor) {
        try {
            String date = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Timber.e(ex);
            Timber.i("passing today's date");
            return new Date();
        }
    }
}
