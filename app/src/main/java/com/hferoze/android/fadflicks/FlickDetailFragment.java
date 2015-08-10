package com.hferoze.android.fadflicks;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;


public class FlickDetailFragment extends Fragment {

    private final String LOG_TAG = FlickDetailFragment.class.getSimpleName();
    private final String FLICKS_QUERY_INFO_KEY = "flicks_query_info";

    private Utils mUtils;
    private ArrayList<FlickDetails> mFlickDetailsList;
    private TextView mDetailRunTime;
    private TextView mDdetailGenre;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity().getApplicationContext();
        mUtils = new Utils(getActivity());
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(FLICKS_QUERY_INFO_KEY, mFlickDetailsList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_flick_detail, container, false);

        ImageView detailPoster = (ImageView) rootView.findViewById(R.id.detailview_posterImageView);
        ImageView detailBackdrop = (ImageView) rootView.findViewById(R.id.detailview_backdropImageView);
        TextView detailTitle = (TextView) rootView.findViewById(R.id.detailview_flickTitleTextView);
        TextView detailReleaseDate = (TextView) rootView.findViewById(R.id.detailview_releaseDateTextView);
        TextView detailPopularity = (TextView) rootView.findViewById(R.id.detailview_popularityTextView);
        RatingBar detailRating = (RatingBar) rootView.findViewById(R.id.detailview_ratingBar);
        TextView detailSummary = (TextView) rootView.findViewById(R.id.detailview_flickOverview);

        mDetailRunTime = (TextView) rootView.findViewById(R.id.detailview_durationTextView);
        mDdetailGenre = (TextView) rootView.findViewById(R.id.detailview_genreTextView);

        Intent in = getActivity().getIntent();
        if (in != null && in.hasExtra(AppConstants.TITLE)) {
            Bundle info = in.getExtras();
            if(savedInstanceState == null || !savedInstanceState.containsKey(FLICKS_QUERY_INFO_KEY)) {
                mFlickDetailsList = new ArrayList<>();
                if (mUtils.isDataAvaialable()) {
                    new GetMovieDetailsTask().execute(Integer.toString(info.getInt(AppConstants.ID)));
                }
            } else {
                mFlickDetailsList = savedInstanceState.getParcelableArrayList(FLICKS_QUERY_INFO_KEY);
                if (!mFlickDetailsList.isEmpty())
                    updateFlickQueryInfo(mFlickDetailsList);
            }

            String posterUrl = buildURI(AppConstants.REMOTE_IMAGE_SIZES[AppConstants.DETAIL_POSTER_IMG_SIZE_IDX], info.getString(AppConstants.IMG_PATH));
            String backdropImgUrl = buildURI(AppConstants.REMOTE_IMAGE_SIZES[AppConstants.DETAIL_BACKDROP_IMG_SIZE_IDX], info.getString(AppConstants.BACKDROP_PATH));

            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE) {
                setImages(posterUrl, detailPoster);
                setImages(backdropImgUrl, detailBackdrop);
            } else if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT){
                setImages(posterUrl, detailPoster);
                setImages(posterUrl, detailBackdrop);
            }

            String title = info.getString(AppConstants.TITLE);
            detailTitle.setText(title);

            detailReleaseDate.setText("( "+mUtils.getYear(info.getString(AppConstants.RELEASE_DATE)) + " )");

            DecimalFormat newFormat = new DecimalFormat("#.#");
            float popVal = Float.valueOf(newFormat.format(info.getFloat(AppConstants.POPULARITY)));
            detailPopularity.setText(popVal + "/100");

            detailRating.setRating((info.getFloat(AppConstants.VOTE_AVG) * AppConstants.RATING_RANGE) / AppConstants.RATING_NORM);

            detailSummary.setText(info.getString(AppConstants.OVERVIEW));

        }
        return rootView;
    }

    private String buildURI( String img_size, String img_path) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
                .authority("image.tmdb.org")
                .appendPath("t")
                .appendPath("p")
                .appendPath(img_size)
                .appendEncodedPath(img_path)
                .build();
        return builder.build().toString();
    }

    private void setImages(String path, ImageView iv) {
        Picasso.with(getActivity()).
                load(path).
                networkPolicy(mUtils.isDataAvaialable() ? NetworkPolicy.NO_CACHE : NetworkPolicy.OFFLINE)
                .error(R.mipmap.error_img).
                into(iv);
    }

    private void updateFlickQueryInfo(ArrayList<FlickDetails> result){
        FlickDetails flickQuery = mFlickDetailsList.get(0);
        mDetailRunTime.setText(flickQuery.getRuntime() + " min");
        mDdetailGenre.setText(flickQuery.getGernes());
    }

    protected ArrayList<FlickDetails> getFlickDataFromJson(String flicksJsonStr)
            throws JSONException {

        final String TMDB_GENRE = "genres";
        final String TMDB_GENRE_NAME= "name";
        final String TMDB_RUNTIME = "runtime";

        JSONObject flickJson = new JSONObject(flicksJsonStr);
        JSONArray flickArray = flickJson.getJSONArray(TMDB_GENRE);

        StringBuilder genre = new StringBuilder();

        ArrayList<FlickDetails> resultList = new ArrayList<>();

        for (int i = 0; i < flickArray.length(); i++) {

            JSONObject flick = flickArray.getJSONObject(i);
            genre.append(flick.getString(TMDB_GENRE_NAME));
            genre.append("/");
        }

        try {
            if (genre.length()>1)
                genre.setLength(genre.length()-1); //removing the last "/"
        }catch (final StringIndexOutOfBoundsException e) {
            Log.e(LOG_TAG, "StringIndexOutOfBoundsException: ", e);
        }
        resultList.add(new FlickDetails(flickJson.getString(TMDB_RUNTIME), genre.toString()));

        return resultList;
    }

    private class GetMovieDetailsTask extends AsyncTask<String, Integer, ArrayList<FlickDetails>> {
        protected ArrayList doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String flicksJsonStr = null;
            try {
                final String URI_SCHEME="http";
                final String URI_AUTH = "api.themoviedb.org";
                final String URI_APPEND_PATH1="3";
                final String URI_APPEND_PATH2="movie";
                final String URI_API_KEY = "api_key";

                Uri.Builder builder = new Uri.Builder();
                builder.scheme(URI_SCHEME)
                        .authority(URI_AUTH)
                        .appendPath(URI_APPEND_PATH1)
                        .appendPath(URI_APPEND_PATH2)
                        .appendPath(params[0])
                        .appendQueryParameter(URI_API_KEY, mContext.getString(R.string.api_key))
                        .build();
                String myUrl = builder.build().toString();
                Log.d(LOG_TAG,"url: " + myUrl);

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

        protected void onPostExecute(ArrayList<FlickDetails> result) {
            if (result!=null && result.size()>0)
                mFlickDetailsList =result;
                updateFlickQueryInfo(mFlickDetailsList);
        }
    }
}
