package com.pointlessapps.raminterpreter.fragments;

import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.adapters.AutocompletionListAdapter;
import com.pointlessapps.raminterpreter.models.AutocompletionDescription;
import com.pointlessapps.raminterpreter.models.AutocompletionItem;
import com.pointlessapps.raminterpreter.models.Command;
import com.pointlessapps.raminterpreter.models.Executor;
import com.pointlessapps.raminterpreter.models.Parser;
import com.pointlessapps.raminterpreter.utils.KeyboardHeightObserver;
import com.pointlessapps.raminterpreter.utils.KeyboardHeightProvider;
import com.pointlessapps.raminterpreter.views.LineNumberEditText;
import com.pointlessapps.raminterpreter.utils.OnTextChanged;
import com.pointlessapps.raminterpreter.utils.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FragmentEditor extends Fragment implements KeyboardHeightObserver {

	private AutocompletionListAdapter autocompletionListAdapter;
	private List<AutocompletionItem> items;
	private ViewGroup rootView;
	private Executor executor;
	private String code;
	private KeyboardHeightProvider keyboardHeightProvider;

	private boolean edited;
	public AutocompletionDescription descriptionItem;

	@Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if(rootView == null) {
			rootView = (ViewGroup)inflater.inflate(R.layout.fragment_editor, container, false);

			init();
			setAutocompletion();

			keyboardHeightProvider = new KeyboardHeightProvider(getActivity());
		}
		return rootView;
	}

	private void init() {
		edited = false;
		descriptionItem = new AutocompletionDescription();

		RecyclerView autocompletionList = rootView.findViewById(R.id.autocompletionList);
		autocompletionList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
		autocompletionList.setAdapter(autocompletionListAdapter = new AutocompletionListAdapter(items = new ArrayList<>()));
		autocompletionListAdapter.setOnClickListener(new AutocompletionListAdapter.OnClickListener() {
			@Override public void onClick(String text) {
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
			}

			@Override public void onInfoClick(AutocompletionItem item) {
				item.setExtended(!item.isExtended());
				if(!item.isExtended()) items.remove(descriptionItem);
				else {
					int index = items.indexOf(item) + 1, i;
					int descriptionID = getResources().getIdentifier("description_" + item.getText().toLowerCase(), "string", rootView.getContext().getPackageName());
					int exampleID = getResources().getIdentifier("example_" + item.getText().toLowerCase(), "string", rootView.getContext().getPackageName());
					if(descriptionID != 0) descriptionItem.set(getResources().getString(descriptionID), getResources().getString(exampleID));
					if((i = items.indexOf(descriptionItem)) != -1 && i - 1 >= 0) {
						items.get(i - 1).setExtended(false);
						Collections.swap(items, i, index - (index > i ? 1 : 0));
					} else items.add(index, descriptionItem);
				}
				autocompletionListAdapter.notifyDataSetChanged();
			}
		});

		rootView.findViewById(R.id.commandsEditor).getViewTreeObserver().addOnScrollChangedListener(this::updateAutocompletionPosition);
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

		updateAutocompletionPosition();
	}

	private void updateAutocompletionPosition() {
		Point pos = rootView.<LineNumberEditText>findViewById(R.id.commandsEditor).getCursorPosition();
		View container = rootView.findViewById(R.id.autocompletionContainer);
		int height = container.getHeight();
		int screenHeight = ((ViewGroup)container.getParent()).getHeight();
		container.setY(Math.max(Math.min(pos.y, screenHeight - height), 0));
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
		if(!text.isEmpty()) {
			for(String s : Command.keyWords) {
				if(s.toLowerCase().contains(text.toLowerCase())) {
					AutocompletionItem item = new AutocompletionItem();
					item.setHasDescription(true);
					item.setText(s);
					item.setMatching(text);
					item.setDescription(getResources().getString(R.string.command));
					matching.add(item);
				}
			}

			try {
				saveCommands(false);
				Map<String, Integer> labelIndexes = executor.getLabelIndexes();
				for(String label : labelIndexes.keySet()) {
					if(label.toLowerCase().contains(text.toLowerCase())) {
						AutocompletionItem item = new AutocompletionItem();
						item.setHasDescription(false);
						item.setText(label);
						item.setMatching(text);
						item.setDescription(getResources().getString(R.string.label));
						matching.add(item);
					}
				}
			} catch(ParseException ignored) { }
		}

		Collections.sort(matching, AutocompletionItem.ItemComparator);
		return matching;
	}

	public FragmentEditor setExecutor(Executor executor) {
		this.executor = executor;
		this.code = Command.getStringList(executor.getCommands());
		return this;
	}

	public void saveCommands(boolean throwable) throws ParseException {
		Editable text = ((LineNumberEditText)rootView.findViewById(R.id.commandsEditor)).getText();
		if(text != null) {
			List<Command> temp = Parser.formatAsList(getContext(), text.toString(), throwable);
			executor.getCommands().clear();
			executor.getCommands().addAll(temp);
		}
	}

	public void updateCode() {
		if(rootView != null)
			((LineNumberEditText)rootView.findViewById(R.id.commandsEditor)).setText(code);
	}

	@Nullable public String getCode() {
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

	@Override public void onKeyboardHeightChanged(int height, int orientation) {
		((LineNumberEditText)rootView.findViewById(R.id.commandsEditor)).adjustMinHeight(height);
	}

	@Override public void onStart() {
		super.onStart();
		if(executor != null) setCode(Command.getStringList(executor.getCommands()));
		keyboardHeightProvider.start();
	}

	@Override public void onPause() {
		super.onPause();
		keyboardHeightProvider.setKeyboardHeightObserver(null);
	}

	@Override public void onResume() {
		super.onResume();
		keyboardHeightProvider.setKeyboardHeightObserver(this);
	}

	@Override public void onDestroy() {
		super.onDestroy();
		keyboardHeightProvider.close();
	}
}