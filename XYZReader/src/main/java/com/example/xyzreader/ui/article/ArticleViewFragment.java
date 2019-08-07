package com.example.xyzreader.ui.article;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Spanned;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.model.Article;
import com.example.xyzreader.ui.ActionBarHelper;
import com.example.xyzreader.ui.ImageLoader;
import com.example.xyzreader.ui.viewmodel.ArticleViewModel;
import com.example.xyzreader.utils.TransitionHelper;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

/**
 * This fragment presents an article body.
 */
public class ArticleViewFragment extends Fragment {

    // An ID of the article to be displayed in this fragment.
    private long startId;

    private ArticleViewModel viewModel;
    private ArticleBodyRecyclerViewAdapter articleBodyRecyclerViewAdapter;

    // In a landscape mode, the fragment will perform an instructive motion - it will scroll
    // the view up to show the article title and text. These are the animation constants.
    private static final int SCROLL_UP_DURATION = 1000; // ms
    private static final int START_DELAY = 300; // ms
    // Scroll up by 2/3 of the screen height.
    private static final float SCROLL_OFFSET_RATIO = 2f / 3f;

    public ArticleViewFragment() {
    }

    public static ArticleViewFragment newInstance(long articleId) {
        Timber.d("Create new ArticleViewFragment for the article id: %d", articleId);
        Bundle args = new Bundle();
        args.putLong(ArticleViewActivity.START_ID, articleId);
        ArticleViewFragment fragment = new ArticleViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");
        if (articleBodyRecyclerViewAdapter == null) {
            articleBodyRecyclerViewAdapter = new ArticleBodyRecyclerViewAdapter();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_article_view, container, false);

        initRecyclerView(rootView);
        initShareButton(rootView);
        initTransition();
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Timber.d("onActivityCreated");
        Bundle arguments = getArguments();
        if (arguments == null) {
            Timber.e("The arguments must be provided.");
            startId = 0;
        } else {
            startId = arguments.getLong(ArticleViewActivity.START_ID, 0);
        }
        Timber.d("%d - onActivityCreated", startId);
        initViewModel(startId);
    }

    private void initRecyclerView(View rootView) {
        RecyclerView recyclerView = rootView.findViewById(R.id.article_body);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setInitialPrefetchItemCount(10);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.getRecycledViewPool().setMaxRecycledViews(R.layout.paragraph, 10);
        recyclerView.setAdapter(articleBodyRecyclerViewAdapter);
    }

    private void initViewModel(long startId) {
        viewModel = ViewModelProviders.of(this).get(ArticleViewModel.class);
        Observer<Article> articleObserver = new Observer<Article>() {
            @Override
            public void onChanged(@Nullable Article article) {
                if (article == null) {
                    Timber.e("The article is null");
                    return;
                }
                Timber.d("%d - Article is ready: %s, %s, %s", startId, article.getItemId(),
                         article.getTitle(), article.getBody().substring(0, 20));
                viewModel.getArticleLiveData().removeObserver(this);
                articleBodyRecyclerViewAdapter.setArticle(article);
                loadToolbarImage(article);
            }
        };
        viewModel.getArticleLiveData().observe(getViewLifecycleOwner(), articleObserver);
        Observer<List<Spanned>> articleBodyObserver = new Observer<List<Spanned>>() {
            @Override
            public void onChanged(@Nullable List<Spanned> paragraphs) {
                if (paragraphs == null) {
                    return;
                }
                Timber.d("%d - setParagraphs: %d", startId, paragraphs.size());
                articleBodyRecyclerViewAdapter.setParagraphs(paragraphs);
            }
        };
        viewModel.getArticleParagraphsLiveData()
                 .observe(getViewLifecycleOwner(), articleBodyObserver);
        viewModel.loadArticle(startId);
    }

    private void loadToolbarImage(Article article) {
        Timber.d("loadToolbarImage: load image: %s", article.getPhotoUrl());
        View view = getView();
        if (view == null) {
            Timber.e("loadToolbarImage: The view is null");
            return;
        }

        initActionBar(view);

        AppCompatImageView imageView = getImageView(view);

        if (imageView == null) {
            Timber.e("loadToolbarImage: The image is not found");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String transitionName =
                    TransitionHelper.createUniqueTransitionName(getContext(), article.getItemId());
            imageView.setTransitionName(transitionName);
        }

        ImageLoader.TitleBackgroundUpdater titleBackgroundUpdater = new ImageLoader.TitleBackgroundUpdater() {
            @Override
            public void setBackgroundColor(int color) {
                articleBodyRecyclerViewAdapter.setMutedColor(color);
            }
        };

        ImageLoader.ImageLoadListener imageLoadListener = new ImageLoader.ImageLoadListener() {
            @Override
            public void onLoadComplete() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().startPostponedEnterTransition();

                    // If the device is in a landscape orientation, the image might take the whole
                    // screen. In this case, perform a view scroll up to display the article title
                    // and text.
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    final int height = displayMetrics.heightPixels;
                    final int width = displayMetrics.widthPixels;
                    if (ArticleViewFragment.this.isVisible() && height < width) {
                        performScrollUpInstructiveMotion((int) (height * SCROLL_OFFSET_RATIO));
                    }
                }
            }

            @Override
            public void onLoadFailed() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().startPostponedEnterTransition();
                }
            }
        };

        ImageLoader.loadImage(getContext(), article.getPhotoUrl(), imageView,
                              article.getAspectRatio(), titleBackgroundUpdater, imageLoadListener);
    }

    private void performScrollUpInstructiveMotion(int verticalOffset) {
        Timber.d("delta: %d", verticalOffset);
        final AppBarLayout appBarLayout = getView().findViewById(R.id.app_bar);
        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        final AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
        if (behavior != null) {
            ValueAnimator valueAnimator = ValueAnimator.ofInt();
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    behavior.setTopAndBottomOffset((Integer) animation.getAnimatedValue());
                    appBarLayout.requestLayout();
                }
            });
            valueAnimator.setIntValues(0, -1 * verticalOffset);
            valueAnimator.setStartDelay(START_DELAY);
            valueAnimator.setDuration(SCROLL_UP_DURATION);
            valueAnimator.start();
        } else {
            Timber.d("The behavior is null");
        }
    }

    private void initActionBar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setLogo(R.drawable.logo);

        if (getActivity() instanceof IActionBarUpdater) {
            ((IActionBarUpdater) getActivity()).updateActionBar(toolbar);
        }

        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        if (appCompatActivity != null) {
            ActionBar actionBar = appCompatActivity.getSupportActionBar();
            if (actionBar != null) {
                ActionBarHelper.initActionBar(actionBar);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    public static AppCompatImageView getImageView(View view) {
        return view.findViewById(R.id.toolbar_image);
    }

    private void initShareButton(View rootView) {
        FloatingActionButton fab = rootView.findViewById(R.id.fab);
        View.OnClickListener shareButtonOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentActivity activity = getActivity();
                if (activity == null) {
                    return;
                }
                startActivity(Intent.createChooser(
                        ShareCompat.IntentBuilder.from(activity)
                                                 .setType("text/plain")
                                                 .setText("Some sample text").getIntent(),
                        getString(R.string.action_share)));
            }
        };
        fab.setOnClickListener(shareButtonOnClickListener);
    }

    private void initTransition() {
        Transition transition = TransitionInflater.from(getContext())
                .inflateTransition(R.transition.article_view_transition);
        setSharedElementEnterTransition(transition);
        setSharedElementReturnTransition(transition);
    }

    static class ArticleBodyRecyclerViewAdapter extends RecyclerView.Adapter<ArticleBodyRecyclerViewAdapter.ParagraphViewHolder> {
        private Article article;
        private List<Spanned> paragraphs;
        private int mutedColor = 0xFF333333;

        ArticleBodyRecyclerViewAdapter() {
            paragraphs = new ArrayList<>();
        }

        void setParagraphs(@NonNull List<Spanned> paragraphs) {
            this.paragraphs = paragraphs;
            notifyDataSetChanged();
        }

        void setArticle(Article article) {
            this.article = article;
            notifyDataSetChanged();
        }

        void setMutedColor(int mutedColor) {
            this.mutedColor = mutedColor;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ParagraphViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            final boolean shouldAttachToParentImmediately = false;
            View view = layoutInflater.inflate(viewType, parent, shouldAttachToParentImmediately);

            if (viewType == R.layout.paragraph) {
                ParagraphViewHolder paragraphViewHolder = new ParagraphViewHolder(view);
                return paragraphViewHolder;
            } else if (viewType == R.layout.article_view_title) {
                TitleViewHolder titleViewHolder = new TitleViewHolder(view);
                titleViewHolder.setMutedColor(mutedColor);
                return titleViewHolder;
            }
            return new ParagraphViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ParagraphViewHolder paragraphViewHolder, int position) {
            paragraphViewHolder.bind(position);
            paragraphViewHolder.setTag(paragraphViewHolder);
        }

        @Override
        public int getItemCount() {
            int size = paragraphs.size();
            return size;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return R.layout.article_view_title;
            }
            return R.layout.paragraph;
        }

        class ParagraphViewHolder extends RecyclerView.ViewHolder {
            TextView paragraphTextView;

            ParagraphViewHolder(@NonNull View itemView) {
                super(itemView);
                if (itemView instanceof TextView) {
                    paragraphTextView = (TextView) itemView;
                }
            }

            void bind(int position) {
                if (position > 0) {
                    Spanned paragraph = paragraphs.get(position);
                    paragraphTextView.setText(paragraph);
                }
            }

            void setTag(ParagraphViewHolder viewHolder) {
                this.paragraphTextView.setTag(viewHolder);
            }
        }

        class TitleViewHolder extends ParagraphViewHolder {
            LinearLayout metaBar;
            TextView title;
            TextView byline;
            int mutedColor = 0xFF333333;

            TitleViewHolder(@NonNull View itemView) {
                super(itemView);
                if (itemView instanceof LinearLayout) {
                    metaBar = itemView.findViewById(R.id.meta_bar);
                    title = itemView.findViewById(R.id.article_title);
                    byline = itemView.findViewById(R.id.article_byline);
                }
            }

            void setMutedColor(int mutedColor) {
                this.mutedColor = mutedColor;
            }

            @Override
            void bind(int position) {
                if (position > 0) {
                    super.bind(position);
                    return;
                }
                title.setText(article.getTitle());
                Spanned text = new ArticleUI().formatDateAndAuthor(article.getAuthor(),
                                                                   article.getPublishedDate());
                byline.setText(text);
                metaBar.setBackgroundColor(mutedColor);
            }

            @Override
            void setTag(ParagraphViewHolder viewHolder) {
                metaBar.setTag(viewHolder);
            }
        }
    }
}
