package com.pointlessapps.raminterpreter.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pointlessapps.raminterpreter.R;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class FilesListAdapter extends RecyclerView.Adapter<FilesListAdapter.DataObjectHolder> {

	private final List<File> items;
	private OnClickListener onClickListener;
	private String selectedFile;
	private int parentOffset;
	private Context context;

	class DataObjectHolder extends RecyclerView.ViewHolder {

		private final AppCompatImageView image;
		private final AppCompatTextView text;
		private final View selection;

		DataObjectHolder(View itemView) {
			super(itemView);

			image = itemView.findViewById(R.id.image);
			text = itemView.findViewById(R.id.text);
			selection = itemView.findViewById(R.id.selection);

			itemView.findViewById(R.id.bg).setOnClickListener(v -> onClickListener.onClick(getItemId() >= 0 ? items.get(getAdapterPosition() - parentOffset) : null));
		}
	}

	public FilesListAdapter(File currentDirectory, List<File> items) {
		this.items = items;
		setCurrentDirectory(currentDirectory);
		setHasStableIds(true);
	}

	public void setSelectedFile(File selectedFile) {
		this.selectedFile = selectedFile != null ? selectedFile.getAbsolutePath() : "";
	}

	public void setCurrentDirectory(File currentDirectory) {
		parentOffset = currentDirectory.getParentFile() != null ? 1 : 0;
	}

	public void setOnClickListener(OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
	}

	@Override public long getItemId(int position) {
		return position - parentOffset;
	}

	@NonNull
	@Override
	public DataObjectHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new DataObjectHolder(LayoutInflater.from(context = parent.getContext()).inflate(R.layout.directory_item_list_view, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull DataObjectHolder holder, int position) {
		if(parentOffset != 0 && position == 0) {
			holder.image.setImageResource(R.drawable.ic_parent_directory);
			holder.text.setText("..");
		} else {
			File file = items.get(position - parentOffset);
			holder.image.setImageResource(getImage(file));
			holder.text.setText(file.getName());
			holder.selection.setBackgroundColor(context.getResources().getColor(file.getAbsolutePath().equals(selectedFile) ? R.color.colorCommand : R.color.colorTransparent));
		}
	}

	private int getImage(File file) {
		return file.isDirectory() ? R.drawable.ic_folder : R.drawable.ic_file;
	}

	@Override
	public int getItemCount() {
		return items.size() + parentOffset;
	}

	public interface OnClickListener {
		void onClick(@Nullable File file);
	}
}