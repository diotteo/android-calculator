package ca.dioo.android.dioo_calc;

import android.app.Activity;
import android.os.Bundle;
import java.io.*;
import java.net.*;
import android.view.View;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.text.Editable;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.view.View.OnFocusChangeListener;

import android.widget.ImageButton;
import android.view.LayoutInflater;


public class MainActivity extends Activity {
	private ScrollView mResultScroll;
	private LinearLayout mResultView;
	private EditText mInputView;
	private boolean mEmptyResults;
	private KeyboardView mKbdView;


	private static class ExpressionWidget extends LinearLayout {
		private View v;
		private ImageButton copyBtn;
		private TextView exprTxt;
		private LinearLayout exprLayout;
		private TextView resultTxt;

		public ExpressionWidget(Context ctxt) {
			super(ctxt);
		}
	}


	public void hideCustomKeyboard() {
		mKbdView.setVisibility(View.GONE);
		mKbdView.setEnabled(false);
	}

	public void showCustomKeyboard(View v) {
		mKbdView.setVisibility(View.VISIBLE);
		mKbdView.setEnabled(true);
		if (v != null) {
			((InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
	}

	public boolean isCustomKeyboardVisible() {
		return mKbdView.getVisibility() == View.VISIBLE;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		mResultScroll = (ScrollView) findViewById(R.id.result_scroll);
		mResultView = (LinearLayout) findViewById(R.id.result_view);
		mInputView = (EditText) findViewById(R.id.input_view);
		mEmptyResults = true;

		Keyboard mKbd = new Keyboard(this, R.xml.keyboard);
		mKbdView = (KeyboardView) findViewById(R.id.keyboardview);
		mKbdView.setKeyboard(mKbd);
		mKbdView.setPreviewEnabled(false);

		mKbdView.setOnKeyboardActionListener(new KeyboardView.OnKeyboardActionListener() {
				private static final int KEYCODE_CLEAR = -100;
				private static final int KEYCODE_ENTER = -101;
				private static final int KEYCODE_CANCEL = Keyboard.KEYCODE_CANCEL;
				private static final int KEYCODE_DELETE = Keyboard.KEYCODE_DELETE;

				@Override
				public void onKey(int primaryCode, int[] keyCodes) {
					View focusCurrent = MainActivity.this.getWindow().getCurrentFocus();
					if (focusCurrent == null || focusCurrent.getClass() != EditText.class) {
						return;
					}

					EditText edittext = (EditText) focusCurrent;
					int start = edittext.getSelectionStart();
					Editable editable = edittext.getText();

					if (primaryCode == KEYCODE_CANCEL) {
						hideCustomKeyboard();
					} else if (primaryCode == KEYCODE_DELETE) {
						if (editable != null && start > 0) {
							editable.delete(start - 1, start);
						}
					} else if (primaryCode == KEYCODE_ENTER) {
						displayResult(focusCurrent);
					} else if (primaryCode == KEYCODE_CLEAR) {
						clearInput(focusCurrent);
					} else {
						editable.insert(start, Character.toString((char) primaryCode));
					}
				}

				@Override public void onPress(int arg0) {
				}

				@Override public void onRelease(int primaryCode) {
				}

				@Override public void onText(CharSequence text) {
				}

				@Override public void swipeDown() {
				}

				@Override public void swipeLeft() {
				}

				@Override public void swipeRight() {
				}

				@Override public void swipeUp() {
				}
				});

		mInputView.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						showCustomKeyboard(v);
					} else {
						hideCustomKeyboard();
					}
				}
				});
		mInputView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showCustomKeyboard(v);
				}
				});
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}


	public void clearInput(View v) {
			mInputView.setText("");
	}


	public void displayResult(View v) {
		String expr = mInputView.getText().toString();
		Number result;
		LayoutInflater inflater;

		try {
			result = ExpressionTree.getResultFromExpr(expr);

			if (result == null) {
				Toast.makeText(this, "Invalid expression", Toast.LENGTH_SHORT).show();
				return;
			}
			clearInput(null);

			String prefix = "\n";
			if (mEmptyResults) {
				mEmptyResults = false;
				prefix = "";
			}
			inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View expr_v = inflater.inflate(R.layout.expression_fragment, null);
			ImageButton expr_btn = (ImageButton) expr_v.findViewById(R.id.copy_whole_btn);
			expr_btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					View parent = (View) v.getParent();
					CharSequence expr_chrsq = ((TextView) parent.findViewById(R.id.expr_txt)).getText().toString();
					mInputView.setText(expr_chrsq);
				}
			});
			TextView expr_txt = (TextView) expr_v.findViewById(R.id.expr_txt);
			TextView result_txt = (TextView) expr_v.findViewById(R.id.result_txt);
			expr_txt.setText(expr);
			result_txt.setText(result.toString());
			result_txt.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					CharSequence expr_chrsq = ((TextView) v).getText().toString();
					mInputView.setText(expr_chrsq);
				}
			});

			mResultView.addView(expr_v);
			mResultScroll.fullScroll(ScrollView.FOCUS_DOWN);
			//FIXME: Must request focus only after scrolling is done?
			//mInputView.requestFocus();
		} catch (MalformedExpressionException e) {
			Toast.makeText(this, "Invalid expression: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}
