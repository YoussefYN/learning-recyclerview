package com.example.youssef.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class PhotoGalleryFragment extends Fragment {
    boolean f = true;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private ArrayList<GalleryItem> mItems;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private QueryPreferences mQueryPreferences = new QueryPreferences();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        updateItems(null);

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        mThumbnailDownloader
                .setThumbnailDownloadListener(
                        new ThumbnailDownloader
                                .ThumbnailDownloadListener<PhotoHolder>() {
                            @Override
                            public void onThumbnailDownloaded(PhotoHolder t, Bitmap bm) {
                                t.bind(new BitmapDrawable(getResources(), bm));
                            }
                        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_clear) {
            mQueryPreferences.setStoredQuery(getContext(), null);
            updateItems(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        SearchView searchView =
                (SearchView) menu.findItem(R.id.menu_item_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                mQueryPreferences.setStoredQuery(getContext(), s);
                updateItems(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    private void updateItems(String query) {
        new FetchItemsTask(query).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = v.findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    private void updateUI() {
        if (f) {
            if (mAdapter == null) {
                if (mItems != null) {
                    f = false;
                    Log.i("Thumbnail", mItems.size() + "");
                    mAdapter = new PhotoAdapter(mItems);
                    mRecyclerView.setAdapter(mAdapter);
                }
            } else {
                System.out.println("KOKOK");
                mAdapter.notifyDataSetChanged();
            }
        }
    }


    private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {

        private String mQuery;

        public FetchItemsTask(String query) {
            if (query == null) {
                mQuery = mQueryPreferences.getStoredQuery(getContext());
            } else {
                mQuery = query;
            }
        }

        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos();
            }
            return new FlickrFetchr().searchPhotos(mQuery);
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            if (mItems != null) {
                mItems.clear();
                mItems.addAll(items);
            } else {
                mItems = items;
            }
            updateUI();
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        ArrayList<GalleryItem> mItems;

        public PhotoAdapter(ArrayList<GalleryItem> items) {
            mItems = items;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());

            View view = layoutInflater
                    .inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem item = mItems.get(position);
            Picasso.with(getContext())
                    .load(item.getUrl())
                    .placeholder(R.drawable.ic_launcher)
                    .into(holder.mImageView);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;


        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView;
        }

        public void bind(Drawable item) {
            mImageView.setImageDrawable(item);
        }
    }
}
