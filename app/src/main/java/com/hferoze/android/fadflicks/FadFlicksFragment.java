package com.hferoze.android.fadflicks;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class FadFlicksFragment extends Fragment
        implements FlickDataFetchAsyncTask.OnAsyncTaskCompleteListener {

    private static final int DATA_STATE_CONNECTED=1;
    private static final int DATA_STATE_DISCONNECTED=0;
    private static final int LAUNCH_WAIT=3000;

    private static final String DATA_SETTINGS_PKG="com.android.settings";
    private static final String DATA_SETTINGS_CLASS="com.android.settings.Settings$DataUsageSummaryActivity";
    private static final String FLICKS_DETAILS_LIST_KEY = "flicks_details";
    private static final String SORT_BY_POPULARITY = "popularity.desc";
    private static final String SORT_BY_HIGHEST_RATED = "vote_average.desc";
    private static final String REQUEST_POSTED_KEY="launch_activity_request";
    private static final String ALERT_CANCELLED_KEY="alert_cancelled";

    private boolean mAlertCancelledState = false;
    private boolean mActivityLaunchPost=false;
    private boolean mIsDoneDownloadingData=false;

    private int data_state;

    private GridView mFlicksGridView;
    private FlicksGridImagesAdapter mFlicksGridImagesAdapter;
    private FlickDataFetchAsyncTask mFlickDataFetchTask;
    private Utils mUtils;
    private ArrayList<FlicksInitDetails> mFlicksInitDetails;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedPrefEditor;
    private MenuItem mCurrentOrder;
    private Dialog mAlert;
    private ActionBar mActionBar;
    private TextView mNoDataTextView;
    private RelativeLayout mSplashView;
    private Context mContext;

    @Override
    public void onResume(){
        super.onResume();
        updateFlicks(mSharedPref.getString(
                getString(R.string.pref_sort_order),
                getString(R.string.pref_sort_default_value)));
    }

    private void updateFlicks(String sort_order) {
        if (mUtils.isDataAvaialable()) {
            dataState(DATA_STATE_CONNECTED);
            mAlertCancelledState=false;
            mNoDataTextView.setVisibility(View.GONE);
            if (mAlert!=null && mAlert.isShowing())
                mAlert.dismiss();

            if (!mIsDoneDownloadingData && mSharedPref != null) {
                mFlickDataFetchTask = new FlickDataFetchAsyncTask(getActivity(), sort_order, this);
                mFlickDataFetchTask.execute();
            }
        }
        else {
            if (!mAlertCancelledState) {
                dataAlert(DATA_STATE_DISCONNECTED);
            } else {
                Toast.makeText(mContext, getResources().getString(R.string.data_unavailable_msg), Toast.LENGTH_SHORT).show();
            }
            mNoDataTextView.setVisibility(View.VISIBLE);
            dataState(DATA_STATE_DISCONNECTED);
            updateGridBasedOnDataConnectionState();
        }
    }

    private void dataState(int data_state) {
        this.data_state=data_state;
    }

    private void dataAlert(int data_state)
    {
        if (data_state == DATA_STATE_DISCONNECTED){
            if (mAlert!=null && !mAlert.isShowing()){
                mAlert.show();
            }
        }
    }

    private void launchCellularDataSettings(){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                DATA_SETTINGS_PKG,
                DATA_SETTINGS_CLASS));
        startActivity(intent);
    }
    private void launchWifiSettings(){
        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
    }
    private void alertCancelled(){
        mAlertCancelledState=true;
        if (mAlert!=null && mAlert.isShowing()){
            mAlert.dismiss();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity().getApplicationContext();
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSharedPrefEditor = mSharedPref.edit();
        mUtils = new Utils(mContext);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFlickDataFetchTask.getStatus() == AsyncTask.Status.RUNNING ||
                mFlickDataFetchTask.getStatus() == AsyncTask.Status.PENDING ) {
            mFlickDataFetchTask.cancel(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(FLICKS_DETAILS_LIST_KEY, mFlicksInitDetails);
        outState.putBoolean(ALERT_CANCELLED_KEY,mAlertCancelledState);
        outState.putBoolean(REQUEST_POSTED_KEY, mActivityLaunchPost);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_fad_flicks, container, false);

        mActionBar = ((FadFlicksActivity) getActivity()).getSupportActionBar();

        mAlert = new Dialog(getActivity());
        mAlert.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mAlert.setContentView(R.layout.no_data_dialog);

        Button btnAlertCell = (Button) mAlert.findViewById(R.id.btn_alert_data_settings);
        btnAlertCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCellularDataSettings();
            }
        });
        Button btnAlertWifi = (Button) mAlert.findViewById(R.id.btn_alert_wifi_settings);
        btnAlertWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchWifiSettings();
            }
        });
        Button btnAlertCancel = (Button) mAlert.findViewById(R.id.btn_alert_cancel);
        btnAlertCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertCancelled();
            }
        });

        mFlickDataFetchTask = new FlickDataFetchAsyncTask(getActivity(), null, this);
        mSplashView = (RelativeLayout) rootView.findViewById(R.id.splash_view);

        if(savedInstanceState == null
                || !savedInstanceState.containsKey(FLICKS_DETAILS_LIST_KEY)
                || !savedInstanceState.containsKey(REQUEST_POSTED_KEY)) {
            mFlicksInitDetails = new ArrayList<>();
            mActivityLaunchPost=true;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    clearView(mSplashView);
                }
            }, LAUNCH_WAIT);
        }
        else {
            mFlicksInitDetails = savedInstanceState.getParcelableArrayList(FLICKS_DETAILS_LIST_KEY);
            mAlertCancelledState = savedInstanceState.getBoolean(ALERT_CANCELLED_KEY);
            if (savedInstanceState.getBoolean(REQUEST_POSTED_KEY)){
                mSplashView.setVisibility(View.INVISIBLE);
            }
        }

        mNoDataTextView = (TextView)rootView.findViewById(R.id.noDataTextView);
        mFlicksGridImagesAdapter = new FlicksGridImagesAdapter(
                getActivity(),
                mFlicksInitDetails);

        mFlicksGridView = (GridView) rootView.findViewById(R.id.main_grid_view);
        mFlicksGridView.setAdapter(mFlicksGridImagesAdapter);

        mFlicksGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FlicksInitDetails flicks = mFlicksInitDetails.get(position);

                Intent detailActivityIntent = new Intent(getActivity(), FlickDetailActivity.class);
                detailActivityIntent.putExtra(AppConstants.ID, flicks.getID());
                detailActivityIntent.putExtra(AppConstants.BACKDROP_PATH, flicks.getBackgroundPath());
                detailActivityIntent.putExtra(AppConstants.IMG_PATH, flicks.getImagesPath());
                detailActivityIntent.putExtra(AppConstants.OVERVIEW, flicks.getOverview());
                detailActivityIntent.putExtra(AppConstants.RELEASE_DATE, flicks.getReleaseDate());
                detailActivityIntent.putExtra(AppConstants.VOTE_AVG, flicks.getVoteAverage());
                detailActivityIntent.putExtra(AppConstants.VOTE_CNT, flicks.getVoteCount());
                detailActivityIntent.putExtra(AppConstants.POPULARITY, flicks.getPopularity());
                detailActivityIntent.putExtra(AppConstants.TITLE, flicks.getTitle());

                startActivity(detailActivityIntent);
            }
        });

        mFlicksGridView.setOnTouchListener(new View.OnTouchListener() {
            final static float DISTANCE_THRESH = 10;
            float downY = 0;
            float upY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        downY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:
                        upY = event.getRawY();
                        if (upY - downY < DISTANCE_THRESH) {  //going down
                            if (mActionBar.isShowing())
                                mActionBar.hide();
                        } else if (upY - downY > DISTANCE_THRESH) { //going up
                            if (!mActionBar.isShowing())
                                mActionBar.show();
                        }
                        break;
                }
                return false;
            }
        });

        return rootView;
    }

    private void clearView(final RelativeLayout splash){

        if(splash!=null) {
            ObjectAnimator animX = ObjectAnimator.ofFloat(splash, "alpha", 1, 0);
            animX.setDuration(700);
            animX.start();
            animX.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    splash.setVisibility(View.INVISIBLE);
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                }
                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
    }

    @Override
    public void updateGrid(ArrayList<FlicksInitDetails> flicksInitDetails) {
        this.mFlicksInitDetails = flicksInitDetails;
        updateGridBasedOnDataConnectionState();
    }

    private void updateGridBasedOnDataConnectionState(){
        if (this.data_state==DATA_STATE_CONNECTED) {
            mFlicksGridImagesAdapter = new FlicksGridImagesAdapter(getActivity(), mFlicksInitDetails);
            mIsDoneDownloadingData = true;
            mFlicksGridView.invalidateViews();
            mFlicksGridView.setAdapter(mFlicksGridImagesAdapter);
        } else {
            mFlicksGridImagesAdapter = new FlicksGridImagesAdapter(getActivity(), mFlicksInitDetails);
            mFlicksGridView.invalidateViews();
            mFlicksGridView.setAdapter(mFlicksGridImagesAdapter);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fad_flicks_fragment, menu);

        String opt =mSharedPref.getString(
                getString(R.string.pref_sort_order),
                getString(R.string.pref_sort_default_value));

        if (opt.equals(SORT_BY_POPULARITY)) {
            menu.findItem(R.id.action_popularity).setEnabled(false);
            menu.findItem(R.id.action_highest_rated).setEnabled(true);
            mCurrentOrder = menu.findItem(R.id.action_popularity);
        }
        else {
            menu.findItem(R.id.action_popularity).setEnabled(true);
            menu.findItem(R.id.action_highest_rated).setEnabled(false);
            mCurrentOrder = menu.findItem(R.id.action_highest_rated);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_popularity) {
            if (!mSharedPref.getString(getString(R.string.pref_sort_order),
                    getString(R.string.pref_sort_default_value)).equals(SORT_BY_POPULARITY)) {
                if (!mUtils.isDataAvaialable()) {
                    updateFlicks(mSharedPref.getString(
                            getString(R.string.pref_sort_order),
                            getString(R.string.pref_sort_default_value)));
                    return true;
                }
                mIsDoneDownloadingData = false;
                item.setEnabled(false);
                mCurrentOrder.setEnabled(true);
                mCurrentOrder = item;
                mSharedPrefEditor.putString(
                        getString(R.string.pref_sort_order),
                        SORT_BY_POPULARITY);
                mSharedPrefEditor.commit();
                updateFlicks(mSharedPref.getString(
                        getString(R.string.pref_sort_order),
                        getString(R.string.pref_sort_default_value)));
            }
            return true;
        } else if (id == R.id.action_highest_rated) {
            if (!mSharedPref.getString(getString(R.string.pref_sort_order),
                    getString(R.string.pref_sort_default_value)).equals(SORT_BY_HIGHEST_RATED) ) {
                if (!mUtils.isDataAvaialable()) {
                    updateFlicks(mSharedPref.getString(
                            getString(R.string.pref_sort_order),
                            getString(R.string.pref_sort_default_value)));
                    return true;
                }
                mIsDoneDownloadingData = false;
                item.setEnabled(false);
                mCurrentOrder.setEnabled(true);
                mCurrentOrder = item;
                mSharedPrefEditor.putString(
                        getString(R.string.pref_sort_order),
                        SORT_BY_HIGHEST_RATED);
                mSharedPrefEditor.commit();
                updateFlicks(mSharedPref.getString(
                        getString(R.string.pref_sort_order),
                        getString(R.string.pref_sort_default_value)));
            }
            return true;
        } else if (id == R.id.refresh) {
            mIsDoneDownloadingData = false;
            updateFlicks(mSharedPref.getString(
                    getString(R.string.pref_sort_order),
                    getString(R.string.pref_sort_default_value)));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
