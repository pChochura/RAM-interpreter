package com.pointlessapps.raminterpreter.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.Toast;

import com.pointlessapps.raminterpreter.R;
import com.pointlessapps.raminterpreter.models.Parser;
import com.pointlessapps.raminterpreter.utils.OnTextChanged;
import com.pointlessapps.raminterpreter.utils.ParseException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineNumberEditText extends AppCompatEditText {

	private static final Pattern PATTERN_ADDRESS = Pattern.compile("(?<=[ ])([*=]?[-]?\\d+)(?![\\w#])");
	private static final Pattern PATTERN_LABELS = Pattern.compile("(\\w+:)");
	private static final Pattern PATTERN_KEYWORDS = Pattern.compile("\\b(READ|LOAD|WRITE|JUMP|JZERO|JGTZ|SUB|ADD|MULT|DIV|HALT|STORE)\\b");
	private static final Pattern PATTERN_COMMENTS = Pattern.compile("#.*");

	private final Handler updateHandler = new Handler();
	private final Runnable updateRunnable = () -> highlightWithoutChange(getText());

	private final int updateDelay = 100;
	private final int charWidth;
	private final int minPadding;
	private final Rect rect;
	private final Paint paint;
	private String prevText;

	private final int colorAddress;
	private final int colorLabel;
	private final int colorCommand;
	private final int colorComment;

	public LineNumberEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		rect = new Rect();
		paint = new Paint();
		paint.setTypeface(ResourcesCompat.getFont(context, R.font.josefin_sans));
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(getCurrentHintTextColor());
		paint.setTextSize(getTextSize());

		Rect bounds = new Rect();
		paint.getTextBounds("0", 0, 1, bounds);
		minPadding = getPaddingLeft();
		charWidth = bounds.width();

		colorAddress = getResources().getColor(R.color.colorAddress);
		colorLabel = getResources().getColor(R.color.colorLabel);
		colorCommand = getResources().getColor(R.color.colorCommand);
		colorComment = getResources().getColor(R.color.colorComment);

		addTextChangedListener(new OnTextChanged(e -> {
			cancelUpdate();
			updateHandler.postDelayed(updateRunnable, updateDelay);
		}));
	}

	@Override
	public boolean onTextContextMenuItem(int id) {
		boolean consumed = super.onTextContextMenuItem(id);
		if(id == android.R.id.paste) try {
			setText(Parser.formatCode(getContext(), Objects.requireNonNull(getText()).toString()));
		} catch(ParseException e) {
			Toast.makeText(getContext(), getResources().getString(R.string.not_formatted), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		return consumed;
	}

	private void clearSpans(Editable e, int length) {
		ForegroundColorSpan spans[] = e.getSpans(0, length, ForegroundColorSpan.class);
		for(int i = spans.length; i-- > 0; ) e.removeSpan(spans[i]);
	}

	private void highlightWithoutChange(Editable e) {
		highlight(e);
	}

	private void highlight(Editable e) {
		try {
			clearSpans(e, e.length());

			if(e.length() == 0) return;

			for(Matcher m = PATTERN_ADDRESS.matcher(e); m.find(); )
				e.setSpan(new ForegroundColorSpan(colorAddress), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			for(Matcher m = PATTERN_LABELS.matcher(e); m.find(); )
				e.setSpan(new ForegroundColorSpan(colorLabel), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			for(Matcher m = PATTERN_KEYWORDS.matcher(e); m.find(); )
				e.setSpan(new ForegroundColorSpan(colorCommand), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			for(Matcher m = PATTERN_COMMENTS.matcher(e); m.find(); )
				e.setSpan(new ForegroundColorSpan(colorComment), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		} catch(IllegalStateException ignored) { }
	}

	private void cancelUpdate() {
		updateHandler.removeCallbacks(updateRunnable);
	}

	private void recalculatePadding() {
		try {
			prevText = Objects.requireNonNull(getText()).toString();
		} catch(NullPointerException ignored) {}
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
		try {
			if(!Objects.requireNonNull(getText()).toString().equals(prevText))
				recalculatePadding();
		} catch(NullPointerException ignored) {}
		super.onDraw(canvas);
	}
}