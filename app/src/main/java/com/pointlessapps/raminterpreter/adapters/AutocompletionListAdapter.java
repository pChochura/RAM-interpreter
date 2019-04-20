package com.pointlessapps.raminterpreter.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.models.AutocompletionDescription;
import com.pointlessapps.raminterpreter.models.AutocompletionItem;

import java.util.List;

public class AutocompletionListAdapter extends RecyclerView.Adapter<AutocompletionListAdapter.DataObjectHolder> {

	private enum Type {Item, Description}

	private final List<AutocompletionItem> items;
	private OnClickListener onClickListener;

	class DataObjectHolder extends RecyclerView.ViewHolder {

		AppCompatTextView text;
		AppCompatTextView description;
		AppCompatImageView help;

		DataObjectHolder(View itemView) {
			super(itemView);

			description = itemView.findViewById(R.id.description);

			try {
				//Only description field is used in both description and item layouts
				text = itemView.findViewById(R.id.text);
				help = itemView.findViewById(R.id.help);
				help.setOnClickListener(v -> onClickListener.onInfoClick(items.get(getAdapterPosition())));
				itemView.findViewById(R.id.bg).setOnClickListener(v -> onClickListener.onClick(text.getText().toString()));
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	public AutocompletionListAdapter(List<AutocompletionItem> items) {
		this.items = items;
		setHasStableIds(true);
	}

	public void setOnClickListener(OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
	}

	@Override public long getItemId(int position) {
		return items.get(position).hashCode();
	}

	@Override public int getItemViewType(int position) {
		return items.get(position) instanceof AutocompletionDescription ? Type.Description.ordinal() : Type.Item.ordinal();
	}

	@NonNull
	@Override
	public DataObjectHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new DataObjectHolder(LayoutInflater.from(parent.getContext()).inflate(
				viewType == Type.Description.ordinal() ? R.layout.description_list_item_view : R.layout.autocompletion_list_item_view, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull DataObjectHolder holder, int position) {
		if(getItemViewType(position) == Type.Item.ordinal()) {
			holder.text.setText(Html.fromHtml(items.get(position).getFormatted()));
			holder.description.setText(items.get(position).getDescription());
			holder.help.setImageResource(items.get(position).isExtended() ? R.drawable.ic_info_outline : R.drawable.ic_info);
			holder.help.setVisibility(items.get(position).hasDescription() ? View.VISIBLE : View.GONE);
		} else {
			holder.description.setText(Html.fromHtml(items.get(position).getDescription()));
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public interface OnClickListener {
		void onClick(String text);
		void onInfoClick(AutocompletionItem item);
	}
}
