package com.example.marvel.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

import androidx.recyclerview.widget.RecyclerView;

import com.example.marvel.R;
import com.google.android.material.button.MaterialButton;

public final class ListFooter {

    private enum State { HIDDEN, SKELETON, LOADING, MESSAGE }

    @LayoutRes
    private final int skeletonLayout;

    private State state = State.HIDDEN;
    private String message = "";
    private String action = "";

    public ListFooter(@LayoutRes int skeletonLayout) {
        this.skeletonLayout = skeletonLayout;
    }

    public boolean isVisible() {
        return state != State.HIDDEN;
    }

    public int count() {
        return isVisible() ? 1 : 0;
    }

    public void showSkeleton(RecyclerView.Adapter<?> adapter, int position) {
        set(adapter, position, State.SKELETON, "", "");
    }

    public void showLoading(RecyclerView.Adapter<?> adapter, int position) {
        set(adapter, position, State.LOADING, "", "");
    }

    public void showMessage(RecyclerView.Adapter<?> adapter, int position, String message, String action) {
        set(adapter, position, State.MESSAGE, message, action);
    }

    public void hide(RecyclerView.Adapter<?> adapter, int position) {
        set(adapter, position, State.HIDDEN, "", "");
    }

    public void reset() {
        state = State.HIDDEN;
        message = "";
        action = "";
    }

    private void set(RecyclerView.Adapter<?> adapter, int position, State newState, String newMessage,
                     String newAction) {
        boolean wasVisible = isVisible();
        state = newState;
        message = newMessage;
        action = newAction;
        boolean nowVisible = isVisible();
        if (wasVisible && nowVisible) adapter.notifyItemChanged(position);
        else if (nowVisible) adapter.notifyItemInserted(position);
        else if (wasVisible) adapter.notifyItemRemoved(position);
    }

    public Holder createHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        Holder holder = new Holder(inflater.inflate(R.layout.item_list_footer, parent, false));
        View skeleton = inflater.inflate(skeletonLayout, holder.skeletonBox, false);
        skeleton.setVisibility(View.VISIBLE);
        holder.skeletonBox.addView(skeleton);
        return holder;
    }

    public void bind(Holder holder, Runnable onAction) {
        boolean skeleton = state == State.SKELETON;
        boolean loading = state == State.LOADING;
        if (skeleton) Skeleton.show(holder.skeletonBox);
        else Skeleton.hide(holder.skeletonBox);
        holder.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        holder.messageBox.setVisibility(state == State.MESSAGE ? View.VISIBLE : View.GONE);
        holder.message.setText(message);
        holder.action.setText(action);
        holder.action.setOnClickListener(v -> onAction.run());
    }

    public static final class Holder extends RecyclerView.ViewHolder {
        final FrameLayout skeletonBox;
        final ProgressBar progress;
        final View messageBox;
        final TextView message;
        final MaterialButton action;

        Holder(View itemView) {
            super(itemView);
            skeletonBox = itemView.findViewById(R.id.footer_skeleton);
            progress = itemView.findViewById(R.id.footer_progress);
            messageBox = itemView.findViewById(R.id.footer_error);
            message = itemView.findViewById(R.id.footer_message);
            action = itemView.findViewById(R.id.footer_retry);
        }
    }
}
