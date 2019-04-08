package com.pointlessapps.raminterpreter.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.adapters.AutocompletionListAdapter;
import com.pointlessapps.raminterpreter.models.AutocompletionItem;
import com.pointlessapps.raminterpreter.models.Command;
import com.pointlessapps.raminterpreter.views.LineNumberEditText;
import com.pointlessapps.raminterpreter.utils.OnTextChanged;
import com.pointlessapps.raminterpreter.utils.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FragmentEditor extends Fragment {

	private ViewGroup rootView;
	private List<AutocompletionItem> items;
	private List<Command> commands;
	private AutocompletionListAdapter autocompletionListAdapter;
	private String code;

	private boolean edited;

	@Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if(rootView == null) {
			rootView = (ViewGroup)inflater.inflate(R.layout.fragment_editor, container, false);

			init();
			setAutocompletion();
		}
		return rootView;
	}

	@Override public void onStart() {
		super.onStart();
		if(commands != null) setCode(Command.getStringList(commands));
	}

	private void init() {
		edited = false;

		RecyclerView autocompletionList = rootView.findViewById(R.id.autocompletionList);
		autocompletionList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
		autocompletionList.setAdapter(autocompletionListAdapter = new AutocompletionListAdapter(items = new ArrayList<>()));
		autocompletionListAdapter.setOnClickListener(text -> {
			LineNumberEditText commandsEditor = rootView.findViewById(R.id.commandsEditor);
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
		LineNumberEditText commandsEditor = rootView.findViewById(R.id.commandsEditor);
		commandsEditor.addTextChangedListener(new OnTextChanged(s -> {
			edited = true;
			prepareAutocompletion(commandsEditor, s);
		}));
		commandsEditor.setOnClickListener(v -> prepareAutocompletion(commandsEditor, commandsEditor.getText()));
	}

	private void prepareAutocompletion(LineNumberEditText commandsEditor, Editable s) {
		Editable editorText = commandsEditor.getText();
		if(editorText != null) {
			int selection;
			if((selection = commandsEditor.getSelectionStart()) == commandsEditor.getSelectionEnd() && selection != -1) {
				int startWordIndex = getStartWordIndex(editorText, selection);
				String typedText = s.toString().substring(startWordIndex, selection);
				List<AutocompletionItem> matching = getMatching(typedText);
				if(matching.size() == 1 && matching.get(0).getText().equals(typedText))
					showAutocompletion(null);
				else showAutocompletion(matching);
			}
		}
	}

	private void showAutocompletion(List<AutocompletionItem> items) {
		this.items.clear();
		if(items != null)
			this.items.addAll(items);
		autocompletionListAdapter.notifyDataSetChanged();
		rootView.findViewById(R.id.autocompletionContainer).setVisibility(items == null || items.isEmpty() ? View.GONE : View.VISIBLE);
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

	private List<AutocompletionItem> getMatching(String text) {
		List<AutocompletionItem> matching = new ArrayList<>();
		if(!text.isEmpty())
			for(String s : Command.keyWords)
				if(s.toLowerCase().contains(text.toLowerCase())) {
					AutocompletionItem item = new AutocompletionItem();
					item.setText(s);
					item.setMatching(text.toLowerCase());
					matching.add(item);
				}
		return matching;
	}

	public FragmentEditor setCommands(List<Command> commands) {
		this.commands = commands;
		this.code = Command.getStringList(commands);
		return this;
	}

	public void saveCommands() throws ParseException {
		Editable text = ((LineNumberEditText)rootView.findViewById(R.id.commandsEditor)).getText();
		if(text != null) {
			List<Command> temp = Command.getCommandsList(text.toString(), getContext());
			commands.clear();
			commands.addAll(temp);
		}
	}

	public void updateCode() {
		if(rootView != null)
			((LineNumberEditText)rootView.findViewById(R.id.commandsEditor)).setText(code);
	}

	public String getCode() {
		try {
			return Objects.requireNonNull(((LineNumberEditText)rootView.findViewById(R.id.commandsEditor)).getText()).toString();
		} catch(NullPointerException ex) {
			return null;
		}
	}

	public void setCode(String code) {
		this.code = code;
		updateCode();
		setEdited(false);
	}

	public boolean isEdited() {
		return edited;
	}

	public void setEdited(boolean edited) {
		this.edited = edited;
	}

	public void setAtLine(int lineIndex) {
		if(lineIndex != -1) {
			LineNumberEditText editor = rootView.findViewById(R.id.commandsEditor);
			try {
				String text = Objects.requireNonNull(editor.getText()).toString();
				int charIndex = 0;
				for(int i = 0; i < lineIndex - 1; i++)
					charIndex = text.indexOf('\n', charIndex) + 1;
				editor.setSelection(charIndex);
			} catch(NullPointerException ignored) {}
		}
	}
}