package com.pointlessapps.raminterpreter.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.models.Command;

import java.util.List;

public class AutocompletionListAdapter extends RecyclerView.Adapter<AutocompletionListAdapter.DataObjectHolder> {

	private List<String> items;
	private OnClickListener onClickListener;

	class DataObjectHolder extends RecyclerView.ViewHolder {

		AppCompatTextView text;

		DataObjectHolder(View itemView) {
			super(itemView);

			text = itemView.findViewById(R.id.text);
			text.setOnClickListener(v -> onClickListener.onClick(text.getText().toString()));
		}
	}

	public AutocompletionListAdapter(List<String> items) {
		this.items = items;
		setHasStableIds(true);
	}

	public void setOnClickListener(OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
	}

	@Override public long getItemId(int position) {
		return items.get(position).hashCode();
	}

	@NonNull
	@Override
	public DataObjectHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new DataObjectHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.autocompletion_list_item_view, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull DataObjectHolder holder, int position) {
		holder.text.setText(items.get(position));
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public interface OnClickListener {
		void onClick(String text);
	}
}
