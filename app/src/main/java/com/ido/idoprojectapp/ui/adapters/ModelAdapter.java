package com.ido.idoprojectapp.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ido.idoprojectapp.utills.helpers.CustomDialogHelper;
import com.ido.idoprojectapp.R;
import com.ido.idoprojectapp.deta.model.Model;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ModelViewHolder> {

    public interface OnDownloadClickListener {
        void onDownloadClick(Model model, int position);
    }

    public interface OnCancelClickListener {
        void onCancelClick(Model model, int position);
    }

    public interface OnUseModelClickListener {
        void onUseModelClick(Model model, int position);
    }

    public interface OnDeleteModelClickListener {
        void onDeleteModelClick(Model model, int position);
    }

    private final Context context;
    private final List<Model> models;
    private final OnDownloadClickListener downloadListener;
    private final OnCancelClickListener cancelListener;
    private final OnUseModelClickListener useModelListener;
    private final OnDeleteModelClickListener deleteListener;
    private Map<String, Integer> downloadProgressMap;
    private String currentModelFilename;

    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            Model model = models.get(position);
            Integer progress = (downloadProgressMap != null) ? downloadProgressMap.get(model.getFilename()) : null;

            if (progress != null) {
                holder.downloadButton.setVisibility(View.GONE);
                holder.cancelButton.setVisibility(View.VISIBLE);
                holder.downloadProgressLayout.setVisibility(View.VISIBLE);
                holder.downloadProgressBar.setProgress(progress);
                holder.downloadProgressText.setText(progress + "%");
            } else {
                super.onBindViewHolder(holder, position, payloads);
            }
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    public ModelAdapter(Context context, List<Model> models, String currentModelFilename,
                        OnDownloadClickListener downloadListener,
                        OnCancelClickListener cancelListener,
                        OnUseModelClickListener useModelListener,
                        OnDeleteModelClickListener deleteListener) {
        this.context = context;
        this.models = models;
        this.currentModelFilename = currentModelFilename;
        this.downloadListener = downloadListener;
        this.cancelListener = cancelListener;
        this.useModelListener = useModelListener;
        this.deleteListener = deleteListener;
    }

    public void setDownloadProgressMap(Map<String, Integer> downloadProgressMap) {
        this.downloadProgressMap = downloadProgressMap;
    }

    public void setCurrentModelFilename(String newFilename) {
        this.currentModelFilename = newFilename;
    }

    // ====== View Binding ======

    @NonNull
    @Override
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_option, parent, false);
        return new ModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        Model model = models.get(position);
        Integer progress = (downloadProgressMap != null) ? downloadProgressMap.get(model.getFilename()) : null;
        holder.bind(context, model, currentModelFilename, progress, downloadListener, cancelListener, useModelListener, deleteListener);
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    static class ModelViewHolder extends RecyclerView.ViewHolder {
        TextView modelName, modelSize, modelDescription, downloadProgressText;
        Button downloadButton, cancelButton;
        LinearLayout downloadProgressLayout;
        ProgressBar downloadProgressBar;

        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            modelName = itemView.findViewById(R.id.modelName);
            modelSize = itemView.findViewById(R.id.modelSize);
            modelDescription = itemView.findViewById(R.id.modelDescription);
            downloadButton = itemView.findViewById(R.id.downloadButton);
            cancelButton = itemView.findViewById(R.id.cancelButton);
            downloadProgressLayout = itemView.findViewById(R.id.downloadProgressLayout);
            downloadProgressBar = itemView.findViewById(R.id.downloadProgressBar);
            downloadProgressText = itemView.findViewById(R.id.downloadProgressText);
        }

        public void bind(final Context context, final Model model, String currentModelFilename, Integer progress,
                         final OnDownloadClickListener downloadListener,
                         final OnCancelClickListener cancelListener,
                         final OnUseModelClickListener useModelListener,
                         final OnDeleteModelClickListener deleteListener) {
            modelName.setText(model.getName());
            modelSize.setText(model.getSize());
            modelDescription.setText(model.getDescription());

            File modelFile = new File(context.getFilesDir(), model.getFilename());
            boolean fileExists = modelFile.exists();

            itemView.setOnLongClickListener(v -> {
                if (fileExists) {
                    CustomDialogHelper.showConfirmation(
                            context,
                            "Delete Model",
                            "Are you sure you want to delete the downloaded file for '" + model.getName() + "'?",
                            "Delete",
                            "Cancel",
                            () -> deleteListener.onDeleteModelClick(model, getAdapterPosition())
                    );
                    return true;
                }
                return false;
            });


            if (progress != null) {
                downloadButton.setVisibility(View.GONE);
                cancelButton.setVisibility(View.VISIBLE);
                downloadProgressLayout.setVisibility(View.VISIBLE);
                downloadProgressBar.setProgress(progress);
                downloadProgressText.setText(progress + "%");

            } else {
                downloadButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.GONE);
                downloadProgressLayout.setVisibility(View.GONE);

                if (model.getFilename().equals(currentModelFilename)) {
                    downloadButton.setText("Active");
                    downloadButton.setEnabled(false);
                    downloadButton.setOnClickListener(null);
                } else if (fileExists) {
                    downloadButton.setText("Use Model");
                    downloadButton.setEnabled(true);
                    downloadButton.setOnClickListener(v -> useModelListener.onUseModelClick(model, getAdapterPosition()));
                } else {
                    downloadButton.setText("Download");
                    downloadButton.setEnabled(true);
                    downloadButton.setOnClickListener(v -> downloadListener.onDownloadClick(model, getAdapterPosition()));
                }
            }
            cancelButton.setOnClickListener(v -> cancelListener.onCancelClick(model, getAdapterPosition()));
        }
    }


}