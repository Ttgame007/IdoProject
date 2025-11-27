package com.ido.idoprojectapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.massageViewHolder> {

    private List<Message> messages;

    private int lastPosition = -1;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    // ====== View Binding ======

    @NonNull
    @Override
    public massageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new MessageAdapter.massageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.massageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.textView.setText(message.getContent());

        if (message.getSender() == 1) {
            holder.textView.setBackgroundResource(R.drawable.ai_message_background);
            holder.textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        } else {
            holder.textView.setBackgroundResource(R.drawable.user_message_background);
            holder.textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        }

        if (position > lastPosition) {
            android.view.animation.Animation animation =
                    android.view.animation.AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_animation_fall_down);
            holder.itemView.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class massageViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        massageViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}