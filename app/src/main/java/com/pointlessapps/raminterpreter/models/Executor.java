package com.pointlessapps.raminterpreter.models;

import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Executor {

	private final SparseIntArray registers;
	private final List<Command> commands;
	private final List<Integer> input;

	private Map<String, Integer> labelIndexes;
	private Output output;

	private boolean isExecuting;

	public Executor() {
		registers = new SparseIntArray();
		labelIndexes = new HashMap<>();
		commands = new ArrayList<>();
		input = new ArrayList<>();
		output = new Output();
		isExecuting = false;
	}

	public Map<String, Integer> getLabelIndexes() {
		Map<String, Integer> labelIndexes = new HashMap<>();
		for(int i = 0; i < commands.size(); i++) {
			String label = commands.get(i).getLabel();
			if(label != null && !label.isEmpty())
				labelIndexes.put(label, i);
		}
		return labelIndexes;
	}

	public void processInput(String text) {
		this.input.clear();
		if(!text.isEmpty()) {
			String[] numbers = text.replaceAll("[^\\d,-]", "").split(",");
			for(String number : numbers) {
				String input = number.replaceAll(",", "");
				if(!input.isEmpty()) this.input.add(Integer.parseInt(input));
			}
		}
	}

	public void prepare() {
		labelIndexes = getLabelIndexes();
		isExecuting = true;
		output.clear();
		input.clear();
		clearRegisters();
	}

	private void clearRegisters() {
		getRegisters().clear();
		getRegisters().put(0, 0);
	}

	public void cancel() {
		isExecuting = false;
	}

	public int executeCommandAtIndex(int index) {
		Command c = commands.get(index);
		String command = c.getCommand();
		if(command.equals(Command.COMMAND.HALT.toString())) {
			index = commands.size() - 1;
			isExecuting = false;
		} else {
			Integer labelIndex = labelIndexes.get(c.getAddress());
			if(command.equals(Command.COMMAND.JUMP.toString())) {
				if(labelIndex != null)
					index = labelIndex - 1;
			} else if(command.equals(Command.COMMAND.JGTZ.toString())) {
				if(registers.get(0) > 0 && labelIndex != null)
					index = labelIndex - 1;
			} else if(command.equals(Command.COMMAND.JZERO.toString())) {
				if(registers.get(0) == 0 && labelIndex != null)
					index = labelIndex - 1;
			} else if(command.equals(Command.COMMAND.LOAD.toString())) {
				int value = decodeValue(c.getAddress());
				registers.put(0, value);
			} else if(command.equals(Command.COMMAND.STORE.toString())) {
				int address = decodeAddress(c.getAddress());
				registers.put(address, registers.get(0));
			} else if(command.equals(Command.COMMAND.ADD.toString())) {
				int value = decodeValue(c.getAddress());
				registers.put(0, registers.get(0) + value);
			} else if(command.equals(Command.COMMAND.SUB.toString())) {
				int value = decodeValue(c.getAddress());
				registers.put(0, registers.get(0) - value);
			} else if(command.equals(Command.COMMAND.DIV.toString())) {
				int value = decodeValue(c.getAddress());
				registers.put(0, registers.get(0) / value);
			} else if(command.equals(Command.COMMAND.MULT.toString())) {
				int value = decodeValue(c.getAddress());
				registers.put(0, registers.get(0) * value);
			} else if(command.equals(Command.COMMAND.READ.toString())) {
				int address = decodeAddress(c.getAddress());
				if(input.size() > 0) registers.put(address, input.remove(0));
				else registers.put(address, (int)(Math.random() * 100));
			} else if(command.equals(Command.COMMAND.WRITE.toString())) {
				int value = decodeValue(c.getAddress());
				output.addValue(decodeAddress(c.getAddress()), value);
			}
		}

		return ++index;
	}

	private int decodeValue(String address) {
		if(address.contains("="))
			return Integer.parseInt(address.substring(1));
		else return registers.get(decodeAddress(address));
	}

	private int decodeAddress(String address) {
		if(address.contains("*"))
			return registers.get(Integer.parseInt(address.substring(1)));
		else return Integer.parseInt(address);
	}

	public List<Command> getCommands() {
		return commands;
	}

	public SparseIntArray getRegisters() {
		return registers;
	}

	public String getInput() {
		StringBuilder builder = new StringBuilder();
		for(Integer number : input)
			builder.append(number).append(", ");
		if(!input.isEmpty())
			builder.delete(builder.length() - 2, builder.length());
		return builder.toString();
	}

	public String getOutput() {
		return output.formatOutput(registers);
	}

	public String getRawOutput() {
		return output.getOutput();
	}

	public void setOutput(String output) {
		this.output.setOutput(output);
	}

	public boolean isExecuting() {
		return isExecuting;
	}
}
