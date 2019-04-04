package com.pointlessapps.raminterpreter.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.support.annotation.LayoutRes;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class Utils {

	public static final int UNDEFINED_WINDOW_SIZE = Integer.MAX_VALUE;

	public static void makeDialog(Context context, @LayoutRes int layout, DataCallback<Dialog> callback, int... windowSize) {
		Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window window = dialog.getWindow();
		if(window != null) {
			window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
			layoutParams.dimAmount = 0.5f;
			dialog.getWindow().setAttributes(layoutParams);
		}
		Point size = getScreenSize(context);
		int width = windowSize != null && windowSize.length > 0 && windowSize[0] != UNDEFINED_WINDOW_SIZE ? windowSize[0] : Math.min(dpToPx(context, 350), size.x - 150);
		int height = windowSize != null && windowSize.length > 1 && windowSize[1] != UNDEFINED_WINDOW_SIZE ? windowSize[1] : Math.min(dpToPx(context, 500), size.y - 150);
		ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(width, height);
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(layout, null);
		dialog.setContentView(view, layoutParams);
		callback.run(dialog);
		if(!dialog.isShowing())
			dialog.show();
	}

	private static Point getScreenSize(Context context) {
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		return new Point(displayMetrics.widthPixels, displayMetrics.heightPixels);
	}

	private static int dpToPx(Context context, int dp) {
		if(context != null)
			return (int) (context.getResources().getDisplayMetrics().density * dp);
		else return 0;
	}
}