package com.pointlessapps.raminterpreter.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import com.pointlessapps.raminterpreter.R;

import java.util.Locale;

public class LineNumberEditText extends AppCompatEditText {

	private int charWidth;
	private int minPadding;
	private String prevText;
	private Rect rect;
	private Paint paint;

	public LineNumberEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		rect = new Rect();
		paint = new Paint();
		paint.setTypeface(ResourcesCompat.getFont(context, R.font.josefin_sans));
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(getCurrentHintTextColor());
		paint.setTextSize(getTextSize());

		Rect bounds = new Rect();
		paint.getTextBounds("0", 0, "0".length(), bounds);
		minPadding = getPaddingLeft();
		charWidth = bounds.width();
	}

	private void recalculatePadding() {
		prevText = getText().toString();
		int padding = ((int)Math.log10(getLineCount()) + 2) * charWidth;
		setPadding(Math.max(padding, minPadding), getPaddingTop(), getPaddingRight(), getPaddingBottom());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int baseline = getBaseline();
		for(int i = 0; i < getLineCount(); i++) {
			canvas.drawText(String.format(Locale.getDefault(), "%d:", i + 1), rect.left + 5, baseline, paint);
			baseline += getLineHeight();
		}
		if(!getText().toString().equals(prevText))
			recalculatePadding();
		super.onDraw(canvas);
	}
}