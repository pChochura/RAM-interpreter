package com.pointlessapps.raminterpreter.activities;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ihhira.android.filechooser.FileChooser;
import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.adapters.RegistersListAdapter;
import com.pointlessapps.raminterpreter.fragments.FragmentCommandsList;
import com.pointlessapps.raminterpreter.fragments.FragmentEditor;
import com.pointlessapps.raminterpreter.models.Command;
import com.pointlessapps.raminterpreter.models.Executor;
import com.pointlessapps.raminterpreter.utils.ParseException;
import com.pointlessapps.raminterpreter.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

	private Executor executor;
	private int SAVE_FILE_REQUEST_CODE = 1;
	private int OPEN_FILE_REQUEST_CODE = 2;

	private int currentLine;
	private Fragment currentFragment;
	private FragmentEditor fragmentEditor;
	private FragmentCommandsList fragmentCommandsList;
	private RegistersListAdapter registersListAdapter;

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		init();
		switchFragment(fragmentCommandsList);
	}

	private void init() {
		executor = new Executor();
		RecyclerView registersList = findViewById(R.id.registersList);
		registersList.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
		registersList.setAdapter(registersListAdapter = new RegistersListAdapter(executor.getRegisters()));
		fragmentCommandsList = new FragmentCommandsList().setCommands(executor.getCommands());
		fragmentEditor = new FragmentEditor().setCommands(executor.getCommands());
	}

	private void switchFragment(Fragment fragment) {
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
		if(currentFragment != null)
			fragmentTransaction.replace(R.id.fragmentsContainer, currentFragment = fragment).commit();
		else fragmentTransaction.add(R.id.fragmentsContainer, currentFragment = fragment).commit();
	}

	public void clickEdit(View view) {
		if(currentFragment.getId() == fragmentEditor.getId()) {
			try {
				fragmentEditor.saveCommands();
				showCommandsList();
			} catch(ParseException e) {
				showDialog(getResources().getString(R.string.parsing_error), e.getLocalizedMessage(), () -> fragmentEditor.setAtLine(e.getLineIndex()), null);
			}
		} else {
			showEditor();
			fragmentEditor.setCode(Command.getStringList(executor.getCommands()));
		}

		TransitionManager.beginDelayedTransition(findViewById(R.id.bg), new AutoTransition());
	}

	public void clickPlay(View view) {
		if(!executor.getCommands().isEmpty()) {
			currentLine = 0;
			executor.prepare();
			setOutput();
			setInput();
			registersListAdapter.notifyDataSetChanged();
			fragmentCommandsList.setCurrentLine(currentLine);

			prepareExecuting();
		} else showDialog(getResources().getString(R.string.error), getResources().getString(R.string.commands_list_empty), null, null);
	}

	public void clickNextLine(View view) {
		executor.processInput(((AppCompatEditText)findViewById(R.id.input)).getText().toString());
		currentLine = executor.executeCommandAtIndex(currentLine);
		fragmentCommandsList.setCurrentLine(currentLine);
		registersListAdapter.notifyDataSetChanged();
		setInput();
		setOutput();

		((RecyclerView)findViewById(R.id.commandsList)).scrollToPosition(currentLine);

		if(!executor.isExecuting()) prepareExecuting();
	}

	public void clickSave(View view) {
		if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED)
			showSaveFileDialog();
		else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, SAVE_FILE_REQUEST_CODE);
	}

	public void clickOpen(View view) {
		if(!fragmentEditor.getCode().isEmpty() && fragmentEditor.isEdited()) {
			showDialog(getResources().getString(R.string.caution), getResources().getString(R.string.discard_changes), this::showOpenFileDialog, () -> {});
		} else if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED)
			showOpenFileDialog();
		else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, OPEN_FILE_REQUEST_CODE);
	}

	private void prepareExecuting() {
		findViewById(R.id.buttonPlay).setVisibility(executor.isExecuting() ? View.GONE : View.VISIBLE);
		findViewById(R.id.buttonEdit).setVisibility(executor.isExecuting() ? View.GONE : View.VISIBLE);
		findViewById(R.id.navigatorContainer).setVisibility(executor.isExecuting() ? View.VISIBLE : View.GONE);

		TransitionManager.beginDelayedTransition(findViewById(R.id.bg), new AutoTransition());
	}

	private void setOutput() {
		((AppCompatTextView)findViewById(R.id.output)).setText(executor.getOutput());
	}

	private void setInput() {
		((AppCompatEditText)findViewById(R.id.input)).setText(executor.getInput());
	}

	private void showEditor() {
		findViewById(R.id.buttonPlay).setVisibility(View.GONE);
		findViewById(R.id.buttonSave).setVisibility(View.VISIBLE);
		findViewById(R.id.buttonOpen).setVisibility(View.VISIBLE);
		((AppCompatImageView)findViewById(R.id.buttonEdit)).setImageResource(R.drawable.ic_done);

		switchFragment(fragmentEditor);
	}

	private void showCommandsList() {
		findViewById(R.id.buttonPlay).setVisibility(View.VISIBLE);
		findViewById(R.id.buttonSave).setVisibility(View.GONE);
		findViewById(R.id.buttonOpen).setVisibility(View.GONE);
		((AppCompatImageView)findViewById(R.id.buttonEdit)).setImageResource(R.drawable.ic_edit);

		switchFragment(fragmentCommandsList);
	}

	private void showSaveFileDialog() {
		File startingFile = Environment.getExternalStorageDirectory();
		if(!startingFile.exists())
			startingFile = Environment.getDataDirectory();
		FileChooser fileChooser = new FileChooser(this, getResources().getString(R.string.choose_a_file), FileChooser.DialogType.SAVE_AS, startingFile);
		fileChooser.setFilelistFilter("txt,c,cpp", true);
		fileChooser.show(file -> {
			try {
				OutputStream fo = new FileOutputStream(file);
				fo.write(fragmentEditor.getCode().getBytes());
				fo.close();
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.file_saved), Toast.LENGTH_SHORT).show();
				fragmentEditor.setEdited(false);
			} catch(IOException exception) {
				exception.printStackTrace();
			}
		});
	}

	private void showOpenFileDialog() {
		File startingFile = Environment.getExternalStorageDirectory();
		if(!startingFile.exists())
			startingFile = Environment.getDataDirectory();
		FileChooser fileChooser = new FileChooser(this, getResources().getString(R.string.choose_a_file), FileChooser.DialogType.SELECT_FILE, startingFile);
		fileChooser.setFilelistFilter("txt,c,cpp", true);
		fileChooser.show(file -> {
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));

				StringBuilder builder = new StringBuilder();
				String line;
				while((line = br.readLine()) != null) builder.append(line).append("\n");
				fragmentEditor.setCode(builder.toString());

				br.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		});
	}

	private void showExplanationDialog() {
		showDialog(getResources().getString(R.string.caution), getResources().getString(R.string.permission_explanation), null, null);
	}

	private void showDialog(String title, String content, @Nullable Runnable okCallback, @Nullable Runnable cancelCallback) {
		Utils.makeDialog(this, R.layout.dialog_simple, dialog -> {
			((AppCompatTextView)dialog.findViewById(R.id.title)).setText(title);
			((AppCompatTextView)dialog.findViewById(R.id.content)).setText(content);

			dialog.findViewById(R.id.buttonOKContainer).setOnClickListener(v -> {
				if(okCallback != null) okCallback.run();
				dialog.dismiss();
			});
			if(cancelCallback == null) dialog.findViewById(R.id.buttonCancelContainer).setVisibility(View.GONE);
			else dialog.findViewById(R.id.buttonCancelContainer).setOnClickListener(v -> {
				cancelCallback.run();
				dialog.dismiss();
			});
		}, Utils.UNDEFINED_WINDOW_SIZE, ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	@Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if(grantResults[0] == PERMISSION_GRANTED) {
			if(requestCode == SAVE_FILE_REQUEST_CODE) showSaveFileDialog();
			else if(requestCode == OPEN_FILE_REQUEST_CODE) showOpenFileDialog();
		} else showExplanationDialog();
	}

	@Override public void onBackPressed() {
		if(executor.isExecuting()) {
			currentLine = -1;
			executor.cancel();
			prepareExecuting();
			fragmentCommandsList.setCurrentLine(currentLine);
		} else if(currentFragment.getId() == fragmentEditor.getId()) {
			if(!fragmentEditor.getCode().isEmpty() && fragmentEditor.isEdited())
				showDialog(getResources().getString(R.string.caution), getResources().getString(R.string.discard_changes), this::showCommandsList, () -> {});
			else showCommandsList();
		} else if(!fragmentEditor.getCode().isEmpty() && fragmentEditor.isEdited())
			showDialog(getResources().getString(R.string.caution), getResources().getString(R.string.leave_unsaved), this::showCommandsList, super::onBackPressed);
	}
}