package com.pointlessapps.raminterpreter.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.adapters.AutocompletionListAdapter;
import com.pointlessapps.raminterpreter.models.Command;
import com.pointlessapps.raminterpreter.utils.OnTextChanged;

import java.util.ArrayList;
import java.util.List;

public class FragmentEditor extends Fragment {

	private ViewGroup rootView;
	private List<String> items;
	private List<Command> commands;
	private AutocompletionListAdapter autocompletionListAdapter;
	private String code;

	@Nullable @Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if(rootView == null) {
			rootView = (ViewGroup)inflater.inflate(R.layout.fragment_editor, container, false);

			init();
			setAutocompletion();
		}
		updateCode();
		return rootView;
	}

	private void init() {
		RecyclerView autocompletionList = rootView.findViewById(R.id.autocompletionList);
		autocompletionList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
		autocompletionList.setAdapter(autocompletionListAdapter = new AutocompletionListAdapter(items = new ArrayList<>()));
		autocompletionListAdapter.setOnClickListener(text -> {
			AppCompatEditText commandsEditor = rootView.findViewById(R.id.commandsEditor);
			int selection;
			if((selection = commandsEditor.getSelectionStart()) == commandsEditor.getSelectionEnd()) {
				Editable editorText = commandsEditor.getText();
				if(editorText != null) {
					int startWordIndex = getStartWordIndex(editorText, selection);
					commandsEditor.setText(editorText.replace(startWordIndex, commandsEditor.getSelectionEnd(), text.concat(" ")));
					commandsEditor.setSelection(startWordIndex + text.length() + 1);
				}
			}
		});
	}

	private void setAutocompletion() {
		AppCompatEditText commandsEditor = rootView.findViewById(R.id.commandsEditor);
		commandsEditor.addTextChangedListener(new OnTextChanged(s -> prepareAutocompletion(commandsEditor, s)));
		commandsEditor.setOnClickListener(v -> prepareAutocompletion(commandsEditor, commandsEditor.getText()));
	}

	private void prepareAutocompletion(AppCompatEditText commandsEditor, Editable s) {
		Editable editorText = commandsEditor.getText();
		if(editorText != null) {
			int selection;
			if((selection = commandsEditor.getSelectionStart()) == commandsEditor.getSelectionEnd()) {
				int startWordIndex = getStartWordIndex(editorText, selection);
				String typedText = s.toString().substring(startWordIndex, selection);
				List<String> matching = getMatching(typedText);
				if(matching.size() == 1 && matching.get(0).equals(typedText))
					showAutocompletion(null);
				else showAutocompletion(matching);
			}
		}
	}

	private int getStartWordIndex(Editable editorText, int selection) {
		int startWordIndex = 0;
		for(int i = selection - 1; i >= 0; i--)
			if(editorText.charAt(i) == '\n' || editorText.charAt(i) == ' ') {
				startWordIndex = i + 1;
				break;
			}
		return startWordIndex;
	}

	private void showAutocompletion(List<String> items) {
		this.items.clear();
		if(items != null)
			this.items.addAll(items);
		autocompletionListAdapter.notifyDataSetChanged();
		rootView.findViewById(R.id.autocompletionContainer).setVisibility(items == null || items.isEmpty() ? View.GONE : View.VISIBLE);
	}

	private List<String> getMatching(String text) {
		List<String> matching = new ArrayList<>();
		if(!text.isEmpty())
			for(String s : Command.keyWords) if(s.contains(text)) matching.add(s);
		return matching;
	}

	private void updateCode() {
		((AppCompatEditText) rootView.findViewById(R.id.commandsEditor)).setText(code != null ? code : Command.getStringList(commands));
	}

	public FragmentEditor setCommands(List<Command> commands) {
		this.commands = commands;
		this.code = null;
		return this;
	}

	public void setCode(String code) {
		this.code = code;
		updateCode();
	}

	public boolean saveCommands() {
		commands.clear();
		try {
			Editable text = ((AppCompatEditText)rootView.findViewById(R.id.commandsEditor)).getText();
			if(text != null)
				commands.addAll(Command.getCommandsList(text.toString(), getContext()));
		} catch(Exception e) {
			e.printStackTrace();
			Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}

	public String getCode() {
		return ((AppCompatEditText) rootView.findViewById(R.id.commandsEditor)).getText().toString();
	}
}