package com.hferoze.android.fadflicks;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FlickDataFetchAsyncTask extends AsyncTask<String, Integer, ArrayList<FlicksInitDetails>>
{
    private final String LOG_TAG = FlickDataFetchAsyncTask.class.getSimpleName();

    private String mSortOrder=null;
    private OnAsyncTaskCompleteListener mListener;

    private Context mContext;

    public interface OnAsyncTaskCompleteListener {
        void updateGrid(ArrayList<FlicksInitDetails> flicksInitDetails);
    }

    public FlickDataFetchAsyncTask (Context context, String currentSortOrder, OnAsyncTaskCompleteListener l){
        mContext = context;
        this.mSortOrder = currentSortOrder;
        mListener = l;
    }

    @Override
    protected void onPreExecute() {

        View rootView = ((Activity)mContext).getWindow().getDecorView().findViewById(android.R.id.content);
        ProgressBar mainProgressBar = (ProgressBar)rootView.findViewById(R.id.mainProgressBar);
        mainProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPostExecute(ArrayList<FlicksInitDetails> flicksInitDetails) {
        if(flicksInitDetails != null) {
            mListener.updateGrid(flicksInitDetails);
            View rootView = ((Activity)mContext).getWindow().getDecorView().findViewById(android.R.id.content);
            ProgressBar mainProgressBar = (ProgressBar)rootView.findViewById(R.id.mainProgressBar);
            mainProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected ArrayList<FlicksInitDetails> doInBackground(String... params) {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String flicksJsonStr = null;
        try {

            final String URI_SCHEME="http";
            final String URI_AUTH = "api.themoviedb.org";
            final String URI_APPEND_PATH1="3";
            final String URI_APPEND_PATH2="discover";
            final String URI_APPEND_PATH3="movie";
            final String URI_PRIM_REL_DATE_PARAM="primary_release_date.gte";
            final String PRIM_REL_DATE="2012-1-1";
            final String URI_SORT_ORDER = "sort_by";
            final String URI_API_KEY = "api_key";

            Uri.Builder builder = new Uri.Builder();
            builder.scheme(URI_SCHEME)
                    .authority(URI_AUTH)
                    .appendPath(URI_APPEND_PATH1)
                    .appendPath(URI_APPEND_PATH2)
                    .appendPath(URI_APPEND_PATH3)
                    .appendQueryParameter(URI_PRIM_REL_DATE_PARAM, PRIM_REL_DATE)
                    .appendQueryParameter(URI_SORT_ORDER, mSortOrder)
                    .appendQueryParameter(URI_API_KEY, mContext.getString(R.string.api_key))
                    .build();
            String myUrl = builder.build().toString();

            URL url = new URL(myUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            flicksJsonStr = buffer.toString();

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            return null;
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        try {
            return getFlickDataFromJson(flicksJsonStr);
        }
        catch(JSONException e) {
            Log.e(LOG_TAG,e.toString());
            e.printStackTrace();
        }
        return null;
    }

    private ArrayList<FlicksInitDetails> getFlickDataFromJson(String flicksJsonStr)
            throws JSONException {

        final String TMDB_ID = "id";
        final String TMDB_RESULTS = "results";
        final String TMDB_TITLE = "title";
        final String TMDB_OVERVIEW = "overview";
        final String TMDB_RELEASE_DATE = "release_date";
        final String TMDB_BACKDROP_PATH = "backdrop_path";
        final String TMDB_POSTER_PATH = "poster_path";
        final String TMDB_POPULARITY = "popularity";
        final String TMDB_VOTE_AVG = "vote_average";
        final String TMDB_VOTE_CNT = "vote_count";


        JSONObject flicksJson = new JSONObject(flicksJsonStr);
        JSONArray flicksArray = flicksJson.getJSONArray(TMDB_RESULTS);

        ArrayList<FlicksInitDetails> resultList = new ArrayList<>();
        for(int i = 0; i < flicksArray.length(); i++) {

            int id;
            String title;
            String overview;
            String release_date;
            String backdrop_path;
            String poster_path;
            float popularity;
            float vote_avg;
            int vote_cnt;

            JSONObject flick = flicksArray.getJSONObject(i);
            id = flick.getInt(TMDB_ID);
            title = flick.getString(TMDB_TITLE);
            overview = flick.getString(TMDB_OVERVIEW);
            release_date = flick.getString(TMDB_RELEASE_DATE);
            backdrop_path = flick.getString(TMDB_BACKDROP_PATH);
            poster_path = flick.getString(TMDB_POSTER_PATH);
            popularity = (float)flick.getDouble(TMDB_POPULARITY);
            vote_avg = (float)flick.getDouble(TMDB_VOTE_AVG);
            vote_cnt = flick.getInt(TMDB_VOTE_CNT);

            resultList.add(new FlicksInitDetails(id, backdrop_path, poster_path, overview,title, release_date, vote_avg, vote_cnt, popularity));
        }

        return resultList;
    }
}