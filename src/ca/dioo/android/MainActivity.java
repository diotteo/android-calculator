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

public class MainActivity extends Activity {
	TextView mResultView;
	EditText mInputView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		mResultView = (TextView) findViewById(R.id.result_view);
		mInputView = (EditText) findViewById(R.id.input_view);
	}


	public void displayResult(View v) {
		String expr = mInputView.getText().toString();
		mResultView.append("\n" + expr + " = " + ExpressionTree.getResultFromExpr(expr));
	}
}
