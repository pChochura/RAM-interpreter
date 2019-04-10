package com.pointlessapps.raminterpreter.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.models.Command;

import java.util.Arrays;
import java.util.List;

public class CommandsListAdapter extends RecyclerView.Adapter<CommandsListAdapter.DataObjectHolder> {

	private final List<Command> commands;

	private OnClickListener onClickListener;
	private Context context;
	private int colors[];

	class DataObjectHolder extends RecyclerView.ViewHolder {

		final AppCompatTextView label, command, address, comment;
		final ViewGroup bg;

		DataObjectHolder(View itemView) {
			super(itemView);

			label = itemView.findViewById(R.id.label);
			command = itemView.findViewById(R.id.command);
			address = itemView.findViewById(R.id.address);
			comment = itemView.findViewById(R.id.comment);
			bg = itemView.findViewById(R.id.bg);

			bg.setOnClickListener(v -> onClickListener.onClick(getAdapterPosition()));

			colors = Arrays.copyOf(colors, colors.length + 2);
			colors[colors.length - 2] = context.getResources().getColor(R.color.colorAccent);
			colors[colors.length - 1] = context.getResources().getColor(R.color.colorBreakpoint);
		}
	}

	public CommandsListAdapter(List<Command> commands, int[] colors) {
		this.commands = commands;
		this.colors = colors;
		setHasStableIds(true);
	}

	public void setOnClickListener(OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
	}

	@Override public long getItemId(int position) {
		return commands.get(position).hashCode();
	}

	@NonNull
	@Override
	public DataObjectHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new DataObjectHolder(LayoutInflater.from(context = parent.getContext()).inflate(R.layout.commands_list_item_view, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull DataObjectHolder holder, int position) {
		if(commands.get(position).getLabel() != null)
			holder.label.setText(commands.get(position).getLabel());
		if(commands.get(position).getCommand() != null)
			holder.command.setText(commands.get(position).getCommand());
		if(commands.get(position).getAddress() != null)
			holder.address.setText(commands.get(position).getAddress());
		if(commands.get(position).getComment() != null)
			holder.comment.setText(commands.get(position).getComment());

		holder.bg.setBackgroundColor(getColor(position));
	}

	private int getColor(int position) {
		int n = position % 2;
		if(commands.get(position).isBreakpoint()) n = 3;
		if(commands.get(position).isSelected()) n = 2;
		return colors[n];
	}

	@Override
	public int getItemCount() {
		return commands.size();
	}

	public interface OnClickListener {
		void onClick(int position);
	}
}
