package com.elvira.gallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.VH> {

    public interface OnItemClick {
        /** Tap on an item. Ignored by MainActivity while in selection mode (handled there). */
        void onClick(int position);
    }

    public interface OnItemLongClick {
        /** Long-press on an item, used to enter/extend selection mode. */
        void onLongClick(int position);
    }

    private final List<MediaItem> items;
    private final OnItemClick clickListener;
    private final OnItemLongClick longClickListener;

    /** Positions currently checked in selection mode. Empty = not in selection mode. */
    private final Set<Integer> selectedPositions = new LinkedHashSet<>();
    private boolean selectionMode = false;

    public MediaAdapter(List<MediaItem> items, OnItemClick clickListener, OnItemLongClick longClickListener) {
        this.items = items;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        MediaItem item = items.get(position);

        Glide.with(holder.thumb.getContext())
                .load(item.file)
                .signature(new ObjectKey(item.file.lastModified()))
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.color.darker_gray)
                .into(holder.thumb);

        holder.videoBadge.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);

        boolean isSelected = selectedPositions.contains(position);
        holder.selectedScrim.setVisibility(selectionMode && isSelected ? View.VISIBLE : View.GONE);
        holder.checkIcon.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.checkIcon.setImageResource(isSelected
                ? R.drawable.ic_check_circle_checked
                : R.drawable.ic_check_circle_unchecked);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (selectionMode) {
                // Route through the long-click callback so MainActivity's
                // action bar (title/subtitle counter) refreshes on every
                // tap, not just on the long-press that started selection
                // mode. Previously this called toggleSelection() directly,
                // which updated the underlying selection correctly but left
                // the "N dipilih" counter stuck at whatever it was when
                // selection mode began - making it look like only 1 item
                // was "detected" even though many were checked.
                if (longClickListener != null) longClickListener.onLongClick(pos);
            } else if (clickListener != null) {
                clickListener.onClick(pos);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return false;
            if (longClickListener != null) longClickListener.onLongClick(pos);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ---------------------------------------------------------------
    // Selection mode
    // ---------------------------------------------------------------

    public boolean isInSelectionMode() {
        return selectionMode;
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        selectionMode = !selectedPositions.isEmpty();
        notifyItemChanged(position);
    }

    public void clearSelection() {
        selectedPositions.clear();
        selectionMode = false;
        notifyDataSetChanged();
    }

    public void selectAll() {
        selectedPositions.clear();
        for (int i = 0; i < items.size(); i++) {
            selectedPositions.add(i);
        }
        selectionMode = !selectedPositions.isEmpty();
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return selectedPositions.size();
    }

    /** Positions currently checked, sorted ascending. */
    public List<Integer> getSelectedPositions() {
        return new java.util.ArrayList<>(selectedPositions);
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView videoBadge;
        View selectedScrim;
        ImageView checkIcon;

        VH(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
            videoBadge = itemView.findViewById(R.id.videoBadge);
            selectedScrim = itemView.findViewById(R.id.selectedScrim);
            checkIcon = itemView.findViewById(R.id.checkIcon);
        }
    }
}
