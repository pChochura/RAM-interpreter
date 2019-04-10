package com.pointlessapps.raminterpreter;

import android.os.AsyncTask;

public class ExecuteAsync extends AsyncTask<String, Void, Void> {

	private AsyncListener asyncListener;

	public ExecuteAsync(AsyncListener asyncListener) {
		this.asyncListener = asyncListener;
	}

	@Override protected void onPreExecute() {
		super.onPreExecute();
		asyncListener.onPreExecute();
	}

	@Override protected Void doInBackground(String... strings) {
		String input = strings[0];
		do {
			input = asyncListener.onExecuteLine(input);
		} while(asyncListener.onConditionCheck());
		return null;
	}

	@Override protected void onPostExecute(Void aVoid) {
		super.onPostExecute(aVoid);
		asyncListener.onPostExecute();
	}

	public interface AsyncListener {
		void onPreExecute();
		void onPostExecute();
		boolean onConditionCheck();
		String onExecuteLine(String input);
	}
}
