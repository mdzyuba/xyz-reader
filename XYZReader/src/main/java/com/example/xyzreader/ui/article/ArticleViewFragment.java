package com.example.xyzreader.ui.article;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.model.Article;
import com.example.xyzreader.ui.ActionBarHelper;
import com.example.xyzreader.ui.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ArticleViewFragment extends Fragment {

    private long startId;
    private ArticleViewModel viewModel;
    private ArticleBodyRecyclerViewAdapter articleBodyRecyclerViewAdapter;

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

        RecyclerView recyclerView = rootView.findViewById(R.id.article_body);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setInitialPrefetchItemCount(10);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.getRecycledViewPool().setMaxRecycledViews(R.layout.paragraph, 10);
        recyclerView.setAdapter(articleBodyRecyclerViewAdapter);
        initShareButton(rootView);
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

    private void initViewModel(long startId) {
        viewModel = ViewModelProviders.of(this).get(ArticleViewModel.class);
        viewModel.getArticleLiveData().observe(getViewLifecycleOwner(), new Observer<Article>() {
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
        });
        viewModel.getArticleParagraphsLiveData()
                 .observe(getViewLifecycleOwner(), new Observer<List<Spanned>>() {
                     @Override
                     public void onChanged(@Nullable List<Spanned> paragraphs) {
                         if (paragraphs == null) {
                             return;
                         }
                         Timber.d("%d - setParagraphs: %d", startId, paragraphs.size());
                         articleBodyRecyclerViewAdapter.setParagraphs(paragraphs);
                     }
                 });
        viewModel.loadArticle(startId);
    }

    private void loadToolbarImage(Article article) {
        Timber.d("loadToolbarImage: load image: %s", article.getPhotoUrl());
        View view = getView();
        if (view == null) {
            Timber.e("loadToolbarImage: The view is null");
            return;
        }

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

        AppCompatImageView imageView = view.findViewById(R.id.toolbar_image);
        if (imageView == null) {
            Timber.e("loadToolbarImage: The image is not found");
            return;
        }
        ImageLoader.TitleBackgroundUpdater titleBackgroundUpdater = new ImageLoader.TitleBackgroundUpdater() {
            @Override
            public void setBackgroundColor(int color) {
                article.setMutedColor(color);
                articleBodyRecyclerViewAdapter.setMutedColor(color);
            }
        };
        ImageLoader.loadImage(getContext(), article.getPhotoUrl(), imageView,
                              article.getAspectRatio(), titleBackgroundUpdater);
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
            long t1 = SystemClock.elapsedRealtime();
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            final boolean shouldAttachToParentImmediately = false;
            View view = layoutInflater.inflate(viewType, parent, shouldAttachToParentImmediately);

            if (viewType == R.layout.paragraph) {
                ParagraphViewHolder paragraphViewHolder = new ParagraphViewHolder(view);
                long t2 = SystemClock.elapsedRealtime();
                Timber.d("time to create a view holder: %d ms", (t2 - t1));
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
            Timber.d("size: %d", size);
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

            public void setMutedColor(int mutedColor) {
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
