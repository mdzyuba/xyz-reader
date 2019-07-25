package com.example.xyzreader.ui.article;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.XyzReaderApp;
import com.example.xyzreader.model.Article;
import com.example.xyzreader.ui.ActionBarHelper;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import timber.log.Timber;

public class ArticleViewFragment extends Fragment {

    private long startId;
    private ArticleViewModel viewModel;
    private LinearLayout titleBarContainer;
    private int mMutedColor = 0xFF333333;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_article_view, container, false);
        initShareButton(rootView);
        return rootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments == null) {
            Timber.e("The arguments must be provided.");
            startId = 0;
        } else {
            startId = arguments.getLong(ArticleViewActivity.START_ID, 0);
        }
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
                Timber.d("%d - Article is ready: %s, %s, %s", startId, article.getItemId(), article.getTitle(), article.getBody().substring(0, 20));
                viewModel.getArticleLiveData().removeObserver(this);
                initArticlePage(article);
            }
        });
        viewModel.loadArticle(startId);
    }

    private void initArticlePage(@Nullable Article article) {
        View view = getView();
        if (view == null || article == null) {
            return;
        }
        TextView title = view.findViewById(R.id.article_title);
        title.setText(article.getTitle());
        TextView bylineView = view.findViewById(R.id.article_byline);
        titleBarContainer = view.findViewById(R.id.meta_bar);

        loadToolbarImage(article.getPhotoUrl());

        Spanned text = new ArticleUI().formatDateAndAuthor(article.getAuthor(),
                                                           article.getPublishedDate());
        bylineView.setText(text);

        RecyclerView recyclerView = view.findViewById(R.id.article_body);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        if (articleBodyRecyclerViewAdapter == null) {
            articleBodyRecyclerViewAdapter = new ArticleBodyRecyclerViewAdapter();
        }
        articleBodyRecyclerViewAdapter.setParagraphs(article.getTextParagraphs());
        recyclerView.setAdapter(articleBodyRecyclerViewAdapter);
    }

    private void initTitleBarBackground(Bitmap bitmap) {
        Palette.from(bitmap).maximumColorCount(12).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                if (palette == null) {
                    Timber.e("The palette is null");
                    return;
                }
                mMutedColor = palette.getDarkMutedColor(0xFF333333);
                titleBarContainer.setBackgroundColor(mMutedColor);
            }
        });
    }

    private void loadToolbarImage(@NonNull String imageUrl) {
        View view = getView();
        if (view == null) {
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

        ImageView imageView = view.findViewById(R.id.toolbar_image);
        if (imageView == null) {
            Timber.e("The image is not found");
            return;
        }
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (bitmap == null && titleBarContainer == null) {
                    return;
                }
                imageView.setImageBitmap(bitmap);
                initTitleBarBackground(bitmap);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Timber.e(e);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
        int width = imageView.getWidth();
        int height = imageView.getHeight();
        if (width > 0 && height > 0) {
            XyzReaderApp.getInstance()
                        .getPicasso()
                        .load(imageUrl)
                        .resize(width, height)
                        .centerCrop()
                        .placeholder(R.drawable.image_placeholder)
                        .into(target);
        }
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


    static class ArticleBodyRecyclerViewAdapter extends RecyclerView.Adapter<ParagraphViewHolder> {
        private Spanned[] paragraphs;

        ArticleBodyRecyclerViewAdapter() {
            paragraphs = new Spanned[0];
        }

        void setParagraphs(@NonNull Spanned[] paragraphs) {
            this.paragraphs = paragraphs;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ParagraphViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            final boolean shouldAttachToParentImmediately = false;
            View view = layoutInflater
                    .inflate(R.layout.paragraph, parent, shouldAttachToParentImmediately);
            return new ParagraphViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ParagraphViewHolder paragraphViewHolder, int position) {
            Spanned paragraph = paragraphs[position];
            paragraphViewHolder.bind(paragraph);
            paragraphViewHolder.paragraphTextView.setTag(paragraphViewHolder);
        }

        @Override
        public int getItemCount() {
            return paragraphs.length;
        }
    }


    static class ParagraphViewHolder extends RecyclerView.ViewHolder {
        final TextView paragraphTextView;

        ParagraphViewHolder(@NonNull View itemView) {
            super(itemView);
            paragraphTextView = (TextView) itemView;
        }

        void bind(Spanned text) {
            paragraphTextView.setText(text);
        }
    }
}
