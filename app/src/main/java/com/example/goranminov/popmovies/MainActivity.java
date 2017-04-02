package com.example.goranminov.popmovies;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.goranminov.popmovies.utilities.MovieDatabaseJsonUtils;
import com.example.goranminov.popmovies.utilities.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/*
 * I have followed the examples from the Sunshine app that was provided during my Nanodegree course.
 * I have followed the AsyncTask class the documentation on android.developer Website
 * as well as the AsyncTask class from the Sunshine app.
 */
public class MainActivity extends AppCompatActivity implements MovieAdapter.MovieAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<String[]>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private RecyclerView mRecyclerView;
    private MovieAdapter mMovieAdapter;
    private TextView mErrorMessage;
    private ProgressBar mLoadingData;
    private static final int MOVIE_DATABASE_LOADER_ID = 29;
    private static boolean PREFERENCE_HAVE_BEEN_UPDATED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
         * We get the reference to our RecyclerView so we can later attach the adapter.
         */
        mRecyclerView = (RecyclerView) findViewById(R.id.movies_data_recycler_view);

        /*
         * We get the reference to our Error Message TextView so we can display the
         * error message in case there is a problem to retrieve the data from
         * the Movie Database website
         */
        mErrorMessage = (TextView) findViewById(R.id.error_message);

        /*
         * We get the reference to our ProgressBar so we will indicate to the user that we are
          * loading the data.
         */
        mLoadingData = (ProgressBar) findViewById(R.id.loading_data_progress_bar);

        /*
         * We attach GridLayoutManager to our RecyclerView as we need to display our results
         * in Grid style.
         */
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        /*
         * The MovieAdapter is responsible to attach our data and display it.
         */
        mMovieAdapter = new MovieAdapter(this);
        mRecyclerView.setAdapter(mMovieAdapter);

        LoaderManager.LoaderCallbacks<String[]> callbacks = MainActivity.this;
        Bundle bundle = null;
        getSupportLoaderManager().initLoader(MOVIE_DATABASE_LOADER_ID, bundle, callbacks);

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    /*
     * Method used to set the error message as invisible and the RecyclerView
     * as visible.
     */
    private void showMovieData() {
        mErrorMessage.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    /*
     * Method used to set the error message as visible and the RecyclerView
     * as invisible.
     */
    private void showErrorData() {
        mErrorMessage.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
    }

    /**
     * Override the onClick method so we will be able to handle the RecyclerView item clicks.
     *
     * @param selectedMovie The information for the movie that was clicked.
     */
    @Override
    public void onClick(String selectedMovie) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, selectedMovie);
        startActivity(intent);
    }

    @Override
    public Loader<String[]> onCreateLoader(int id, final Bundle args) {

        return new AsyncTaskLoader<String[]>(this) {

            String[] mMovieData = null;

            @Override
            protected void onStartLoading() {
                if (mMovieData != null) {
                    deliverResult(mMovieData);
                } else {
                    mLoadingData.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            @Override
            public String[] loadInBackground() {
                SharedPreferences sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
                String sortingQuery = sharedPreferences.getString(getString(R.string.pref_sort_key),
                        getString(R.string.pref_sort_popular_label));
                URL url = NetworkUtils.buildUrl(sortingQuery);

                try {
                    String moviesJsonString = NetworkUtils.getResponseFromHttpUrl(url);
                    String[] movieData = MovieDatabaseJsonUtils.getMovieDataFromJson(MainActivity.this,
                            moviesJsonString);
                    return movieData;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            public void deliverResult(String[] data) {
                mMovieData = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<String[]> loader, String[] data) {
        mLoadingData.setVisibility(View.INVISIBLE);
        mMovieAdapter.setMovieData(data);
        if (data == null) {
            showErrorData();
            mMovieAdapter.setMovieData(data);
        } else {
            showMovieData();
        }
    }

    @Override
    public void onLoaderReset(Loader<String[]> loader) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (PREFERENCE_HAVE_BEEN_UPDATED) {
            getSupportLoaderManager().restartLoader(MOVIE_DATABASE_LOADER_ID, null, this);
            PREFERENCE_HAVE_BEEN_UPDATED = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        PREFERENCE_HAVE_BEEN_UPDATED = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        /*
         * We inflate our menu layout to this menu and display it in the Toolbar.
         */
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}