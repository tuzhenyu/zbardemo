package tzy.sf.zbarapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by tzy on 2017/2/6.
 */

public class ResultActivity extends Activity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_result);
        textView = (TextView) findViewById(R.id.text);
        String code = getIntent().getStringExtra(ScannerActivity.EXTRA_CODE);
        textView.setText("扫描结果：\n" + code);
    }

}
