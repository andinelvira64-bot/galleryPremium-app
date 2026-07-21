package com.elvira.gallery;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MediaPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_PHOTO = 0;
    private static final int TYPE_VIDEO = 1;

    private final List<String> paths;
    private final List<Integer> types;

    public MediaPagerAdapter(List<String> paths, List<Integer> types) {
        this.paths = paths;
        this.types = types;
    }

    @Override
    public int getItemViewType(int position) {
        return types.get(position) == MediaItem.TYPE_VIDEO ? TYPE_VIDEO : TYPE_PHOTO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_VIDEO) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video_page, parent, false);
            return new VideoVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_photo_page, parent, false);
            return new PhotoVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String path = paths.get(position);
        if (holder instanceof PhotoVH) {
            Glide.with(((PhotoVH) holder).photoView.getContext())
                    .load(path)
                    .fitCenter()
                    .into(((PhotoVH) holder).photoView);
        } else if (holder instanceof VideoVH) {
            VideoVH vh = (VideoVH) holder;
            vh.videoView.setVideoURI(Uri.parse(path));
            MediaController controller = new MediaController(vh.videoView.getContext());
            controller.setAnchorView(vh.videoView);
            vh.videoView.setMediaController(controller);
            vh.videoView.setOnPreparedListener(mp -> mp.setLooping(false));
        }
    }

    @Override
    public int getItemCount() {
        return paths.size();
    }

    static class PhotoVH extends RecyclerView.ViewHolder {
        ImageView photoView;
        PhotoVH(View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }

    static class VideoVH extends RecyclerView.ViewHolder {
        VideoView videoView;
        VideoVH(View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
        }
    }
}
