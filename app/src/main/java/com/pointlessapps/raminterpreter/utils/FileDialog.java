package com.pointlessapps.raminterpreter.utils;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.adapters.FilesListAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileDialog {

	public enum FileDialogType {
		SELECT_DIRECTORY,
		SELECT_FILE
	}

	private final String[] extensions = {".RAM", ".txt"};

	private FilesListAdapter filesListAdapter;
	private Spinner extensionsList;
	private Activity activity;
	private String title;

	private File currentDirectory;
	private File selectedFile;

	private FileDialogType fileDialogType;

	private boolean withNegativeButton;
	private String positiveButton;
	private String negativeButton;
	private DataCallbackPair<Dialog, File> positiveButtonCallback;
	private DataCallback<Dialog> negativeButtonCallback;

	public FileDialog(Activity activity, String title, FileDialogType fileDialogType, File startingDirectory) {
		this.activity = activity;
		this.title = title;
		this.fileDialogType = fileDialogType;
		this.currentDirectory = startingDirectory;
	}

	public FileDialog withPositiveButton(String positiveButton, DataCallbackPair<Dialog, File> positiveButtonCallback) {
		this.positiveButton = positiveButton;
		this.positiveButtonCallback = positiveButtonCallback;
		return this;
	}

	public FileDialog withNegativeButton(String negativeButton, DataCallback<Dialog> negativeButtonCallback) {
		this.negativeButton = negativeButton;
		this.negativeButtonCallback = negativeButtonCallback;
		this.withNegativeButton = true;
		return this;
	}

	public FileDialog setSelectedFile(File selectedFile) {
		this.selectedFile = selectedFile;
		return this;
	}

	public void show() {
		Utils.makeDialog(activity, fileDialogType == FileDialogType.SELECT_DIRECTORY ? R.layout.dialog_list_input : R.layout.dialog_list, dialog -> {
			((AppCompatTextView) dialog.findViewById(R.id.title)).setText(title);

			dialog.findViewById(R.id.buttonCancelContainer).setVisibility(withNegativeButton ? View.VISIBLE : View.INVISIBLE);

			((AppCompatTextView) dialog.findViewById(R.id.buttonOK)).setText(positiveButton);
			((AppCompatTextView) dialog.findViewById(R.id.buttonCancel)).setText(negativeButton);

			dialog.findViewById(R.id.buttonOKContainer).setOnClickListener(v -> {
				if(fileDialogType == FileDialogType.SELECT_DIRECTORY) {
					AppCompatEditText input = dialog.findViewById(R.id.input);
					Editable text = input.getText();
					if(text != null) selectedFile = new File(currentDirectory, text.toString().concat(extensions[extensionsList.getSelectedItemPosition()]));
				}
				if(fileDialogType == FileDialogType.SELECT_DIRECTORY && selectedFile.exists())
					showOverrideDialog(() -> {if(positiveButtonCallback != null) positiveButtonCallback.run(dialog, selectedFile);});
				else if(positiveButtonCallback != null) positiveButtonCallback.run(dialog, selectedFile);
			});

			dialog.findViewById(R.id.buttonCancelContainer).setOnClickListener(v -> {
				if(negativeButtonCallback != null) negativeButtonCallback.run(dialog);
			});

			if(fileDialogType == FileDialogType.SELECT_DIRECTORY)
				((AppCompatEditText) dialog.findViewById(R.id.input)).addTextChangedListener(new OnTextChanged(editable -> {
					selectedFile = null;
					updateInput(dialog);
				}));

			if(fileDialogType == FileDialogType.SELECT_DIRECTORY)
				setExtensionsList(dialog);

			setFilesList(dialog);
		}, Utils.UNDEFINED_WINDOW_SIZE, ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	private void showOverrideDialog(Runnable callback) {
		Utils.makeDialog(activity, R.layout.dialog_simple, dialog -> {
			((AppCompatTextView)dialog.findViewById(R.id.title)).setText(activity.getResources().getString(R.string.caution));
			((AppCompatTextView)dialog.findViewById(R.id.content)).setText(activity.getResources().getString(R.string.override_warning, selectedFile.getName()));

			dialog.findViewById(R.id.buttonOKContainer).setOnClickListener(v -> {
				callback.run();
				dialog.dismiss();
			});
			dialog.findViewById(R.id.buttonCancelContainer).setOnClickListener(v -> dialog.dismiss());
		}, Utils.UNDEFINED_WINDOW_SIZE, ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	private void setFilesList(Dialog dialog) {
		List<File> files = getFiles();

		RecyclerView filesList = dialog.findViewById(R.id.list);
		filesList.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
		filesListAdapter = new FilesListAdapter(currentDirectory, files);
		filesListAdapter.setOnClickListener(file -> {
			if(file == null || file.isDirectory()) {
				if(file == null) currentDirectory = currentDirectory.getParentFile();
				else currentDirectory = file;
				files.clear();
				files.addAll(getFiles());
				filesList.scrollToPosition(0);
				filesListAdapter.setCurrentDirectory(currentDirectory);
			} else selectedFile = file;
			updateInput(dialog);
		});
		filesList.setAdapter(filesListAdapter);

		updateInput(dialog);
	}

	private void setExtensionsList(Dialog dialog) {
		extensionsList = dialog.findViewById(R.id.selection);
		extensionsList.setAdapter(new ArrayAdapter<>(activity, R.layout.extensions_list_item_view, extensions));
	}

	private void updateInput(Dialog dialog) {
		int index = indexOf(extensions, getExtension(selectedFile));
		if(index != -1 && fileDialogType == FileDialogType.SELECT_DIRECTORY) extensionsList.setSelection(index);

		if(selectedFile != null && fileDialogType == FileDialogType.SELECT_DIRECTORY)
			((AppCompatEditText) dialog.findViewById(R.id.input)).setText(getSimpleName(selectedFile));
		filesListAdapter.setSelectedFile(selectedFile);
		filesListAdapter.notifyDataSetChanged();
	}

	private ArrayList<File> getFiles() {
		if(currentDirectory == null) return new ArrayList<>();
		File[] files = currentDirectory.listFiles(file -> !file.isHidden() && (file.isDirectory() || contains(extensions, getExtension(file))));
		if(files == null) return new ArrayList<>();
		ArrayList<File> list = new ArrayList<>(Arrays.asList(files));
		Collections.sort(list, (file1, file2) -> {
			if(file1.isDirectory() && file2.isFile()) return -1;
			else return file2.isDirectory() && file1.isFile() ? 1 : file1.getName().compareToIgnoreCase(file2.getName());
		});
		return list;
	}

	private boolean contains(String[] array, String item) {
		return indexOf(array, item) != -1;
	}

	private int indexOf(String[] array, String item) {
		for(int i = 0; i < array.length; i++) if(array[i].equals(item)) return i;
		return -1;
	}

	private String getExtension(File file) {
		if(file == null) return "";
		String name = file.getName();
		int dotIndex = name.indexOf(".");
		return dotIndex >= 0 ? name.substring(dotIndex) : "";
	}

	private String getSimpleName(File file) {
		if(file == null) return "";
		String name = file.getName();
		int dotIndex = name.indexOf(".");
		return dotIndex >= 0 ? name.substring(0, dotIndex) : name;
	}
}
