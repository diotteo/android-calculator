package ca.dioo.android.dioo_calc;

import android.app.Activity;
import android.os.Bundle;
import java.io.*;
import java.net.*;
import android.view.View;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.View.OnClickListener;

import android.widget.ImageButton;
import android.view.LayoutInflater;


public class MainActivity extends Activity {
	private ScrollView mResultScroll;
	private LinearLayout mResultView;
	private EditText mInputView;
	private boolean mEmptyResults;


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


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		mResultScroll = (ScrollView) findViewById(R.id.result_scroll);
		mResultView = (LinearLayout) findViewById(R.id.result_view);
		mInputView = (EditText) findViewById(R.id.input_view);
		mEmptyResults = true;
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
			mInputView.setText("");

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
