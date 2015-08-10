package com.hferoze.android.fadflicks;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FlicksGridImagesAdapter extends BaseAdapter {

    private List<FlicksInitDetails> mFlicksInitDetails;
    private Utils mUtils;
    private Context mContext;

    public FlicksGridImagesAdapter(Context c,
                                   List<FlicksInitDetails> flicksInitDetailsArray) {
        mContext = c;
        mUtils = new Utils(mContext);
        mFlicksInitDetails = flicksInitDetailsArray;
    }

    @Override
    public int getCount() {
        return mFlicksInitDetails.size();
    }

    @Override
    public Object getItem(int arg0) {
        return mFlicksInitDetails.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.grid_images_layout, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.ivFlickPoster = (ImageView) convertView.findViewById(R.id.gridview_imageView);
            viewHolder.rbFlickRating = (RatingBar) convertView.findViewById(R.id.gridview_ratingBar);
            viewHolder.tvFlickReleasDate = (TextView) convertView.findViewById(R.id.gridview_releaseDateTextView);
            viewHolder.tvFlickTitle = (TextView) convertView.findViewById(R.id.gridview_flickTitleTextView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        FlicksInitDetails flicks = mFlicksInitDetails.get(position);

        String imgUrl = buildURI(AppConstants.REMOTE_IMAGE_SIZES[AppConstants.GRID_IMAGE_SIZE_IDX], flicks.getImagesPath());
        setImages(imgUrl, viewHolder.ivFlickPoster);

        viewHolder.tvFlickTitle.setText(flicks.getTitle());
        viewHolder.tvFlickTitle.setSelected(true);
        viewHolder.rbFlickRating.setRating((flicks.getVoteAverage() * AppConstants.RATING_RANGE) / AppConstants.RATING_NORM);
        viewHolder.tvFlickReleasDate.setText(mUtils.getYear(flicks.getReleaseDate()));
        return convertView;
    }

    private String buildURI(String img_size, String img_path) {
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
        Picasso.with(mContext).
                load(path).
                networkPolicy(mUtils.isDataAvaialable() ? NetworkPolicy.NO_CACHE : NetworkPolicy.OFFLINE)
                .error(R.mipmap.error_img).
                into(iv);
    }

    private static class ViewHolder {
        ImageView ivFlickPoster;
        RatingBar rbFlickRating;
        TextView tvFlickReleasDate;
        TextView tvFlickTitle;
    }
}
