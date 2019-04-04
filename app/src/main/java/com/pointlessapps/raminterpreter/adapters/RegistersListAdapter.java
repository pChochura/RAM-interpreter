package com.pointlessapps.raminterpreter.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pointlessapps.raminterpreter.R;

public class RegistersListAdapter extends RecyclerView.Adapter<RegistersListAdapter.DataObjectHolder> {

	private SparseIntArray registers;
	private Context context;

	class DataObjectHolder extends RecyclerView.ViewHolder {

		AppCompatTextView title, value;

		DataObjectHolder(View itemView) {
			super(itemView);

			title = itemView.findViewById(R.id.title);
			value = itemView.findViewById(R.id.value);
		}
	}

	public RegistersListAdapter(SparseIntArray registers) {
		this.registers = registers;
		setHasStableIds(true);
	}

	@Override public long getItemId(int position) {
		return position;
	}

	@NonNull
	@Override
	public DataObjectHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new DataObjectHolder(LayoutInflater.from(context = parent.getContext()).inflate(R.layout.registers_list_item_view, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull DataObjectHolder holder, int position) {
		holder.title.setText(context.getResources().getString(R.string.register, position));
		holder.value.setText(String.valueOf(registers.get(position, 0)));
	}

	@Override
	public int getItemCount() {
		return registers.keyAt(registers.size() - 1) + 1;
	}
}
