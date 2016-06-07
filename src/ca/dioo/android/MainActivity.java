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
import android.widget.Toast;

public class MainActivity extends Activity {
	private ScrollView mResultScroll;
	private TextView mResultView;
	private EditText mInputView;
	private boolean mEmptyResults;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		mResultScroll = (ScrollView) findViewById(R.id.result_scroll);
		mResultView = (TextView) findViewById(R.id.result_view);
		mInputView = (EditText) findViewById(R.id.input_view);
		mEmptyResults = true;
	}


	public void displayResult(View v) {
		String expr = mInputView.getText().toString();
		Number result;
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
				mResultView.setText("");
				prefix = "";
			}
			mResultView.append(prefix + expr + " = " + result);
			mResultScroll.fullScroll(ScrollView.FOCUS_DOWN);
			//FIXME: Must request focus only after scrolling is done?
			//mInputView.requestFocus();
		} catch (MalformedExpressionException e) {
			Toast.makeText(this, "Invalid expression: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}
