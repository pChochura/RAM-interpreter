package com.pointlessapps.raminterpreter;

import android.Manifest;
import android.os.Bundle;
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
import com.obsez.android.lib.filechooser.internals.FileUtil;
import com.pointlessapps.raminterpreter.adapters.RegistersListAdapter;
import com.pointlessapps.raminterpreter.fragments.FragmentCommandsList;
import com.pointlessapps.raminterpreter.fragments.FragmentEditor;
import com.pointlessapps.raminterpreter.models.Command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

	private static final int MAX_SIZE = 100;

	private String output;
	private int[] registers;
	private List<Integer> input;
	private List<Command> commands;
	private Map<String, Integer> labelIndexes;

	private boolean isExecuting;
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

	@Override public void onBackPressed() {
		if(isExecuting) {
			currentLine = -1;
			isExecuting = false;
			prepareExecuting();
			fragmentCommandsList.setCurrentLine(currentLine);
		} else if(currentFragment.getId() == fragmentEditor.getId()) clickEdit(null);
		else super.onBackPressed();
	}

	private void init() {
		commands = new ArrayList<>();
		registers = new int[MAX_SIZE];
		input = new ArrayList<>();
		RecyclerView registersList = findViewById(R.id.registersList);
		registersList.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
		registersList.setAdapter(registersListAdapter = new RegistersListAdapter(registers));
		fragmentCommandsList = new FragmentCommandsList().setCommands(commands);
		fragmentEditor = new FragmentEditor().setCommands(commands);
	}

	private void resetState() {
		labelIndexes = getLabelIndexes();
		currentLine = 0;
		isExecuting = true;
		output = "";
		setOutput();
		input.clear();
		clearRegisters();
		fragmentCommandsList.setCurrentLine(currentLine);
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
			if(fragmentEditor.saveCommands()) {
				findViewById(R.id.buttonPlay).setVisibility(View.VISIBLE);
				findViewById(R.id.buttonSave).setVisibility(View.GONE);
				findViewById(R.id.buttonOpen).setVisibility(View.GONE);
				((AppCompatImageView)findViewById(R.id.buttonEdit)).setImageResource(R.drawable.ic_edit);
				switchFragment(fragmentCommandsList);
			}
		} else {
			findViewById(R.id.buttonPlay).setVisibility(View.GONE);
			findViewById(R.id.buttonSave).setVisibility(View.VISIBLE);
			findViewById(R.id.buttonOpen).setVisibility(View.VISIBLE);
			((AppCompatImageView)findViewById(R.id.buttonEdit)).setImageResource(R.drawable.ic_done);

			switchFragment(fragmentEditor);
		}

		TransitionManager.beginDelayedTransition(findViewById(R.id.bg), new AutoTransition());
	}

	public void clickPlay(View view) {
		if(!commands.isEmpty()) {
			resetState();

			prepareExecuting();
		} else Toast.makeText(getApplicationContext(), getString(R.string.commands_list_empty), Toast.LENGTH_SHORT).show();
	}

	private void prepareExecuting() {
		findViewById(R.id.buttonPlay).setVisibility(isExecuting ? View.GONE : View.VISIBLE);
		findViewById(R.id.buttonEdit).setVisibility(isExecuting ? View.GONE : View.VISIBLE);
		findViewById(R.id.navigatorContainer).setVisibility(isExecuting ? View.VISIBLE : View.GONE);

		TransitionManager.beginDelayedTransition((ViewGroup)findViewById(R.id.bg), new AutoTransition());
	}

	public void clickNextLine(View view) {
		currentLine = executeCommandAtIndex(currentLine);
		fragmentCommandsList.setCurrentLine(currentLine);
		registersListAdapter.notifyDataSetChanged();

		((RecyclerView)findViewById(R.id.commandsList)).scrollToPosition(currentLine);

		if(currentLine >= commands.size()) {
			isExecuting = false;
			prepareExecuting();
		}
	}

	private int executeCommandAtIndex(int index) {
		Command c = commands.get(index);
		String command = c.getCommand();
		if(command.equals(Command.COMMAND.HALT.toString())) {
			index = commands.size() - 1;
		} else if(command.equals(Command.COMMAND.JUMP.toString())) {
			if(labelIndexes.get(c.getAddress()) != null)
				index = labelIndexes.get(c.getAddress()) - 1;
		} else if(command.equals(Command.COMMAND.JGTZ.toString())) {
			if(registers[0] != 0 && labelIndexes.get(c.getAddress()) != null)
				index = labelIndexes.get(c.getAddress()) - 1;
		} else if(command.equals(Command.COMMAND.JZERO.toString())) {
			if(registers[0] == 0 && labelIndexes.get(c.getAddress()) != null)
				index = labelIndexes.get(c.getAddress()) - 1;
		} else if(command.equals(Command.COMMAND.LOAD.toString())) {
			int value = decodeValue(c.getAddress());
			registers[0] = value;
		} else if(command.equals(Command.COMMAND.STORE.toString())) {
			int address = decodeAddress(c.getAddress());
			registers[address] = registers[0];
		} else if(command.equals(Command.COMMAND.ADD.toString())) {
			int value = decodeValue(c.getAddress());
			registers[0] += value;
		} else if(command.equals(Command.COMMAND.SUB.toString())) {
			int value = decodeValue(c.getAddress());
			registers[0] -= value;
		} else if(command.equals(Command.COMMAND.DIV.toString())) {
			int value = decodeValue(c.getAddress());
			registers[0] /= value;
		} else if(command.equals(Command.COMMAND.MULT.toString())) {
			int value = decodeValue(c.getAddress());
			registers[0] *= value;
		} else if(command.equals(Command.COMMAND.READ.toString())) {
			int address = decodeAddress(c.getAddress());
			getInput();
			if(input != null && input.size() > 0) {
				registers[address] = input.remove(0);
				setInput();
			} else registers[address] = (int)(Math.random() * MAX_SIZE);
		} else if(command.equals(Command.COMMAND.WRITE.toString())) {
			int value = decodeValue(c.getAddress());
			output += String.format(Locale.getDefault(), "%d, ", value);
			setOutput();
		}

		return ++index;
	}

	private void setOutput() {
		((AppCompatTextView)findViewById(R.id.output)).setText(output);
	}

	private void getInput() {
		this.input.clear();
		String text = ((AppCompatEditText)findViewById(R.id.input)).getText().toString();
		if(!text.isEmpty()) {
			String[] numbers = text.replaceAll("\\s+", "").split(",");
			for(String number : numbers) {
				String input = number.replaceAll(",", "");
				if(!input.isEmpty()) this.input.add(Integer.parseInt(input));
			}
		}
	}

	private void setInput() {
		StringBuilder builder = new StringBuilder();
		for(Integer number : input)
			builder.append(number).append(", ");
		if(!input.isEmpty())
			builder.delete(builder.length() - 2, builder.length());
		((AppCompatEditText)findViewById(R.id.input)).setText(builder.toString());
	}

	private void clearRegisters() {
		for(int i = 0; i < registers.length; i++)
			registers[i] = 0;
	}

	private int decodeValue(String address) {
		if(address.contains("="))
			return Integer.parseInt(address.substring(1));
		else return registers[decodeAddress(address)];
	}

	private int decodeAddress(String address) {
		if(address.contains("*"))
			return registers[Integer.parseInt(address.substring(1))];
		else return Integer.parseInt(address);
	}

	private Map<String, Integer> getLabelIndexes() {
		Map<String, Integer> labelIndexes = new HashMap<>();
		for(int i = 0; i < commands.size(); i++) {
			String label = commands.get(i).getLabel();
			if(label != null && !label.isEmpty())
				labelIndexes.put(label, i);
		}
		return labelIndexes;
	}

	public void clickSave(View view) {
		if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
			FileChooser fileChooser = new FileChooser(this, getString(R.string.choose_file), FileChooser.DialogType.SAVE_AS, new File(FileUtil.getStoragePath(getApplicationContext(), false)));
			fileChooser.setFilelistFilter("txt,c,cpp", true);
			fileChooser.show(file -> {
				try {
					OutputStream fo = new FileOutputStream(file);
					fo.write(fragmentEditor.getCode().getBytes());
					fo.close();
					Toast.makeText(getApplicationContext(), getString(R.string.file_saved), Toast.LENGTH_SHORT).show();
				} catch(IOException exception) {
					exception.printStackTrace();
				}
			});
		}
	}

	public void clickOpen(View view) {
		if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
			FileChooser fileChooser = new FileChooser(this, "Choose a file", FileChooser.DialogType.SELECT_FILE, new File(FileUtil.getStoragePath(getApplicationContext(), false)));
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
	}
}