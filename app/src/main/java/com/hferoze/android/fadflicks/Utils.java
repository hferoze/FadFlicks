package com.hferoze.android.fadflicks;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.List;

public class Utils {

    private Context mContext;

    public Utils(Context ctx) {
        mContext = ctx;
    }

    public boolean isDataAvaialable() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isDataConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (isDataConnected) {
            if ((activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                    || (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public String getYear(String date) {
        if (date != null && !date.equals("null") &&
                date.length() >= AppConstants.YEAR_STR_LENGTH) {
            if (date.substring(0, AppConstants.YEAR_STR_LENGTH).matches("^-?\\d+$"))
                return date.substring(0, AppConstants.YEAR_STR_LENGTH);
        }
        return "";
    }

    public boolean isIntentSafe(Intent intent){
        //From developer.android.com
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        return activities.size() > 0;
    }

}
