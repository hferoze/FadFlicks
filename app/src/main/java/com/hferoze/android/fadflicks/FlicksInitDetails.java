package com.hferoze.android.fadflicks;

import android.os.Parcel;
import android.os.Parcelable;

public class FlicksInitDetails implements Parcelable {

    private String mImagesPaths, mBackdropPaths, mOverview, mTitle, mReleaseDate;
    private float mVoteAvg, mPopularity;
    private int mId, mVoteCount;

    public FlicksInitDetails(int id, String background_path, String images_path,
                             String overview, String title, String release_date, float vote_avg, int vote_cnt, float popularity) {
        this.mId = id;
        this.mImagesPaths = images_path;
        this.mBackdropPaths = background_path;
        this.mOverview = overview;
        this.mTitle = title;
        this.mReleaseDate = release_date;
        this.mVoteAvg = vote_avg;
        this.mVoteCount = vote_cnt;
        this.mPopularity = popularity;
    }

    private FlicksInitDetails(Parcel in) {
        mId = in.readInt();
        mBackdropPaths = in.readString();
        mImagesPaths = in.readString();
        mOverview = in.readString();
        mTitle = in.readString();
        mReleaseDate = in.readString();
        mVoteAvg = in.readFloat();
        mVoteCount = in.readInt();
        mPopularity = in.readFloat();
    }

    public int getID() {
        return mId;
    }

    public String getBackgroundPath() {
        return mBackdropPaths;
    }

    public String getImagesPath() {
        return mImagesPaths;
    }

    public String getOverview() {
        return mOverview;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getReleaseDate() {
        return mReleaseDate;
    }

    public float getVoteAverage() {
        return mVoteAvg;
    }

    public int getVoteCount() {
        return mVoteCount;
    }

    public float getPopularity() {
        return mPopularity;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mId);
        out.writeString(mBackdropPaths);
        out.writeString(mImagesPaths);
        out.writeString(mOverview);
        out.writeString(mTitle);
        out.writeString(mReleaseDate);
        out.writeDouble(mVoteAvg);
        out.writeInt(mVoteCount);
        out.writeDouble(mPopularity);
    }

    public static final Parcelable.Creator<FlicksInitDetails> CREATOR = new Parcelable.Creator<FlicksInitDetails>() {
        public FlicksInitDetails createFromParcel(Parcel in) {
            return new FlicksInitDetails(in);
        }

        public FlicksInitDetails[] newArray(int size) {
            return new FlicksInitDetails[size];
        }
    };
}
