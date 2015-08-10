package com.hferoze.android.fadflicks;

import android.os.Parcel;
import android.os.Parcelable;

public class FlickDetails implements Parcelable {

    private String mRuntime, mGenres;

    public FlickDetails(String runtime, String genres) {
        this.mRuntime = runtime;
        this.mGenres=genres;
    }

    private FlickDetails(Parcel in) {
        mRuntime=in.readString();
        mGenres = in.readString();
    }

    public String getRuntime(){
        return mRuntime;
    }

    public String getGernes(){
        return mGenres;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mRuntime);
        out.writeString(mGenres);
    }

    public static final Parcelable.Creator<FlickDetails> CREATOR = new Parcelable.Creator<FlickDetails>() {
        public FlickDetails createFromParcel(Parcel in) {
            return new FlickDetails(in);
        }

        public FlickDetails[] newArray(int size) {
            return new FlickDetails[size];
        }
    };
}
