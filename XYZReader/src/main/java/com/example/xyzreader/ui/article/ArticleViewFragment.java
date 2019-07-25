package com.example.xyzreader.ui.article;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ArticleViewFragment extends Fragment {

    private long startId;
    private ArticleViewModel viewModel;
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
                Timber.d("%d - Article is ready: %s, %s, %s", startId, article.getItemId(), article.getTitle(), article.getBody().substring(0, 20));
                viewModel.getArticleLiveData().removeObserver(this);
                articleBodyRecyclerViewAdapter.setArticle(article);
            }
        });
        viewModel.getArticleParagraphsLiveData()
                 .observe(getViewLifecycleOwner(), new Observer<List<Spanned>>() {
                     @Override
                     public void onChanged(@Nullable List<Spanned> paragraphs) {
                         Timber.d("%d - setParagraphs: %d", startId, paragraphs.size());
                         articleBodyRecyclerViewAdapter.setParagraphs(paragraphs);
                     }
                 });
        viewModel.getPhotoUrlLiveData().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String photoUrl) {
                if (photoUrl != null) {
                    loadToolbarImage(photoUrl);
                }
            }
        });
        viewModel.loadArticle(startId);
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
                articleBodyRecyclerViewAdapter.setMutedColor(mMutedColor);
                Timber.d("mMutedColor: %d", mMutedColor);
            }
        });
    }

    private void loadToolbarImage(@NonNull String imageUrl) {
        Timber.d("loadToolbarImage: load image: %s", imageUrl);
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

        ImageView imageView = view.findViewById(R.id.toolbar_image);
        if (imageView == null) {
            Timber.e("loadToolbarImage: The image is not found");
            return;
        }


        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                if (bitmap == null ) { // | titleBarContainer == null
                    Timber.e("loadToolbarImage: %s", bitmap);
                    return;
                }
                Timber.d("loadToolbarImage: setImageBitmap: %d bytes, %d width, %d height",
                         bitmap.getByteCount(), bitmap.getWidth(), bitmap.getHeight());
                imageView.setImageBitmap(bitmap);
                initTitleBarBackground(bitmap);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Timber.e(e, "loadToolbarImage: %s", e.getMessage());
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
        int width = imageView.getWidth();
        int height = imageView.getHeight();
        if (width > 0 && height > 0) {
            Timber.d("loadToolbarImage: loading the image: %d, %d", width, height);
            XyzReaderApp.getInstance()
                        .getPicasso()
                        .load(imageUrl)
                        .resize(width, height)
                        .centerCrop()
                        .placeholder(R.drawable.image_placeholder)
                        .into(target);
        } else {
            Timber.e("loadToolbarImage: The image width: %d or height: %d is 0", width, height);

            imageView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
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
                    } else {
                        Timber.e("loadToolbarImage: again: The image width: %d or height: %d is 0", width,
                                 height);
                    }
                }
            });
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

        public void setArticle(Article article) {
            this.article = article;
            notifyDataSetChanged();
        }

        public void setMutedColor(int mutedColor) {
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
                return titleViewHolder;
            }
            return null;
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

            public TitleViewHolder(@NonNull View itemView) {
                super(itemView);
                if (itemView instanceof LinearLayout) {
                    metaBar = itemView.findViewById(R.id.meta_bar);
                    title = itemView.findViewById(R.id.article_title);
                    byline = itemView.findViewById(R.id.article_byline);
                }
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
