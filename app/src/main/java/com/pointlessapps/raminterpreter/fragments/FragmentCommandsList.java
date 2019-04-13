package com.pointlessapps.raminterpreter.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.adapters.CommandsListAdapter;
import com.pointlessapps.raminterpreter.models.Command;
import com.pointlessapps.raminterpreter.utils.BottomPaddingItem;
import com.pointlessapps.raminterpreter.utils.Utils;

import java.util.List;

public class FragmentCommandsList extends Fragment {

	private ViewGroup rootView;
	private List<Command> commands;
	private CommandsListAdapter commandsListAdapter;

	private boolean isExecuting;

	@Nullable @Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if(rootView == null) {
			rootView = (ViewGroup)inflater.inflate(R.layout.fragment_commands_list, container, false);

			setCommandsList();
		}

		commandsListAdapter.notifyDataSetChanged();

		return rootView;
	}

	private void setCommandsList() {
		RecyclerView commandsList = rootView.findViewById(R.id.commandsList);
		commandsList.addItemDecoration(new BottomPaddingItem(Utils.dpToPx(getContext(), 100)));
		commandsList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
		commandsList.setAdapter(commandsListAdapter = new CommandsListAdapter(commands,
				new int[]{getResources().getColor(R.color.colorDark), getResources().getColor(R.color.colorLight)}));
		commandsListAdapter.setOnClickListener(position -> {
			if(isExecuting) {
				Command command = commands.get(position);
				command.setBreakpoint(!command.isBreakpoint());
				commandsListAdapter.notifyDataSetChanged();
			}
		});
	}

	public FragmentCommandsList setCommands(List<Command> commands) {
		this.commands = commands;
		return this;
	}

	public void setCurrentLine(int currentLine) {
		for(int i = 0; i < commands.size(); i++) commands.get(i).setSelected(i == currentLine);
		commandsListAdapter.notifyDataSetChanged();
	}

	public void setExecuting(boolean isExecuting) {
		this.isExecuting = isExecuting;

		if(!isExecuting) for(Command c : commands)
			c.setBreakpoint(false);
	}
}
