package tzy.sf.zbarapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.InflateException;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tzy.sf.zbarapp.view.BoxView;
import tzy.sf.zbarapp.view.CameraPreview;
import tzy.sf.zbarapp.view.ShadeView;


@SuppressLint("NewApi")
public class ScannerActivity extends FragmentActivity {

    public static final String KEY_CODE = "key_code";

    public static final String TAG = "ScannerActivity";

    public static final String EXTRA_WEB_CONTRAL = "extra_web_contral";

    public static final float BOX_WIDTH_RATE = 0.92f;
    public static final float BOX_HEIGHT_RATE = 0.92f;

//    public static final float BOX_MIDDLE_OFFSET_RATE = 0.43f;
    public static final float BOX_TOP_OFFSET_RATE = 0.17F;

    public static final float BOX_WIDTH_TO_HEIGHT = 1.0f;

//    public static final int SPACE_BETWEEN_BOX_VIEW_AND_TEXT_VIEW_IN_DP = 30;
    public static final float BOTTOM_TXT_MARGIN_TOP_RATE = 0.05F;

    public static final int PREVIEW_CALLBACK_VALID_PERIOD = 300;
    public static final int FOCUS_PERIOD = 1000;
    public static final float DELTA_ACCEPTABLE = 0.1f;

    public static final int MSG_SCANED = 354;
    public static final int MSG_DATA_ILLEGAL = 355;
    public static final int MSG_WEBSITE = 356;
    public static final int MSG_DATA_UNIDENTIFIED = 357;

    public static final String EXTRA_CODE = "code";

    ImageScanner scanner;
    private Camera mCamera;
    private CameraPreview mPreview;

    private BoxView mBoxView;
//    private TextView mTextView;
    private LinearLayout llBottomText;
//    private AnimateView mAnimateView;
    private FrameLayout flAnimate;
    private ImageView ivAnimate;

    private ImageView mImageScanBar;


    private ShadeView mShaderTop;
    private ShadeView mShaderBottom;
    private ShadeView mShaderLeft;
    private ShadeView mShaderRight;

    private boolean isFocus = true;


    private int mBoxWidth;
    private int mBoxHeight;
    private int mScreenWidth;
    private int mScreenHeight;


    private int mBoxTopOffset;
//    private float mBoxTopOffsetRate;

    private MySize mBestPreviewSize;

    private float mScreenDensity;


    private Timer mTimer;

    private Thread mScaner;
    private boolean mFlag;
    private Object mLock = new Object();
    private byte[] mYuvData;
    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            System.gc();
            if (!mScaner.isAlive()) {
                mCamera.setPreviewCallback(null);
                return;
            }
            synchronized (mLock) {
                mYuvData = data;
                mLock.notify();
            }
            mCamera.setPreviewCallback(null);
        }
    };
    private boolean mIsScaned;

    static {
        System.loadLibrary("iconv");
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {

                case MSG_DATA_ILLEGAL:
                    if (!mIsScaned) {
                        mIsScaned = true;
                    }
                    Toast.makeText(ScannerActivity.this,"不合法",Toast.LENGTH_SHORT).show();
                    break;
                case MSG_WEBSITE://扫描出网址，进入该网页

                case MSG_DATA_UNIDENTIFIED://未标识的内容，可能是文本或者无法识别的内容
                    break;
                case MSG_SCANED:
                    if (!mIsScaned) {
                        String code = (String) msg.obj;
                        mIsScaned = true;
                        Intent intent = new Intent(ScannerActivity.this,ResultActivity.class);
                        intent.putExtra(EXTRA_CODE,code);
                        ScannerActivity.this.startActivity(intent);
                    }
                    break;
            }
        }

    };


    private boolean dlgShown;

    private Animation scanAnim = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 1f);

    @Override
    protected void onCreate(Bundle saveinstance) {
        super.onCreate(saveinstance);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try {
            setContentView(R.layout.activity_bar_code_login);
        } catch (InflateException e) {
            Toast.makeText(this,"内存不足",Toast.LENGTH_SHORT).show();
            Log.i(TAG, Log.getStackTraceString(e));
            finish();
            return;
        }
        getIntentData();

        try {
            init();
        } catch (RuntimeException e) {
            dlgShown = true;
            showCameraDlg();
//            showActionPrompt2(R.string.camera_error);
            Log.i(TAG, Log.getStackTraceString(e));
        }

    }

    private void showCameraDlg(){
        Toast.makeText(this,"无法打开摄像头，摄像头可能被占用",Toast.LENGTH_SHORT).show();
    }

    private void getIntentData() {
        Intent intent = getIntent();
    }

    private void startAnalysing() {
        mScaner = new Thread() {
            /**
             * 解析信息：
             * 判断code是否url，如果是，再判断是否登录信息；
             * 如果是登录信息，则进入登录界面；
             * 否则进入浏览器界面；
             * 否则，直接显示该文本。
             *
             * @return 如果是登录信息，返回true，否则返回false
             */
            private boolean paraseLoginMsg(String data,Message msg){
               /* Pattern pattern = Pattern.compile(LinkUtils.REGEX_URL);
                Matcher matcher = pattern.matcher(data);
                if(matcher.matches()){//URL
                    int i = data.lastIndexOf("/");//判断是否登录信息
                    if(i != -1){
                        String validatePart = data.substring(0, i);
                        if((URL_TEMPLATE + '/' + NetServices.SERVER_TDC).equals(validatePart)){ //登录逻辑，进入登录界面
                            String code = data.substring(i + 1);
                            msg.obj = code;
                            mHandler.sendMessage(msg);
                            return true;
                        }
                    }
                    //非登录信息，只是一个普通网址，进入浏览器界面
                    msg.what = MSG_WEBSITE;
                    msg.obj = data;
                    mHandler.sendMessage(msg);
                }else{
                    msg.what = MSG_DATA_UNIDENTIFIED;
                    msg.obj = data;
                    mHandler.sendMessage(msg);
                }*/

                msg.what = MSG_DATA_UNIDENTIFIED;
                msg.obj = data;
                mHandler.sendMessage(msg);

                return false;
            }

            /**
             * 判断是否"SJIS"编码，如果是，则中文字符串可能解析成乱码，
             * 所以要先转成正确的字节数组，再用“utf-8”编码转成字符串
             *
             * @param source 可能是 “SJIS”编码的字符串
             * @return 转为“utf-8”编码的字符串
             * */
            public String makeSJISEncodingRight(String source){

                if(null == source) return "";

                String code = source;

                try {
                    //检查是否满足日文编码
                    boolean canSJISCharsetEncoding = java.nio.charset.Charset.
                            forName("SJIS").newEncoder().canEncode(source);

                    // 以下条件可能成立的情景：
                    // 1.source是日文；2.source是中文被SJIS解码后的乱码
                   if( canSJISCharsetEncoding ){//尝试转为UTF-8
                        code = new String(source.getBytes("SJIS"),"utf-8");
                        // 检查转换后是否满足GBK编码格式，
                        // 若不满足条件(说明source不是中文，转编码无意义)，则撤销
                            if( !java.nio.charset.Charset.
                                    forName("GBK").newEncoder().canEncode(code)){
                                code = source;
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    code = source;
                    Log.i(TAG,"解析二维码扫描文本错误：" + e.getMessage());
                }catch (Exception e){
                    code = source;
                    Log.i(TAG,"解析二维码扫描文本错误：" + e.getMessage());
                }

                if(code == null){return "";}
                return code;
            }

            @Override
            public void run() {

                while (true) {
                    synchronized (mLock) {
                        try {
                            if (mFlag) {
                                break;
                            }
                            mLock.wait();
                            if (mFlag) {
                                break;
                            }

                            Image barcode = new Image(mBestPreviewSize.width,
                                    mBestPreviewSize.height, "Y800");
                            barcode.setData(mYuvData);

                            int result = scanner.scanImage(barcode);

                            if (result != 0) {
                                isFocus = true;//
                                SymbolSet syms = scanner.getResults();
                                for (Symbol sym : syms)  {

                                    Log.i(TAG,"SCAN SUCCESS!");
                                    Message msg = new Message();
                                    msg.what = MSG_SCANED;

                                    String  code = makeSJISEncodingRight(sym.getData());

                                    msg.obj = code;
                                    mHandler.sendMessage(msg);
                                }
                            }
                        } catch (InterruptedException e) {
                            Log.i(TAG, Log.getStackTraceString(e));
                        }
                    }
                }
            }
        };
        mScaner.start();
    }

    private void initAnim(){
        scanAnim.setDuration(2000);
        scanAnim.setRepeatCount(Animation.INFINITE);
//        scanAnim.setRepeatMode(Animation.);
    }

    private void init() {
        initAnim();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        mScreenDensity = displayMetrics.density;
        mBoxWidth = (int) (BOX_WIDTH_RATE * mScreenWidth);
        mBoxHeight = mBoxWidth;
        mBoxTopOffset = /*(int) (mScreenHeight * BOX_MIDDLE_OFFSET_RATE - mBoxHeight / 2);*/(int) (BOX_TOP_OFFSET_RATE * mScreenHeight);
//        mBoxTopOffsetRate = mBoxTopOffset / (float) mScreenHeight;



        mBoxView = (BoxView) findViewById(R.id.box_view);
        llBottomText = (LinearLayout) findViewById(R.id.ll_bottom_text);
//        mTextView = (TextView) findViewById(R.id.text_view);
//        mAnimateView = (AnimateView) findViewById(R.id.grid_animate_view);
        flAnimate = (FrameLayout) findViewById(R.id.fl_animate);
        ivAnimate = (ImageView) findViewById(R.id.iv_animate);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(mBoxWidth, mBoxHeight, Gravity.CENTER_HORIZONTAL);
        layoutParams.setMargins((mScreenWidth - mBoxWidth) / 2, mBoxTopOffset, (mScreenWidth - mBoxWidth) / 2, mScreenHeight - mBoxTopOffset - mBoxHeight);
        mBoxView.setLayoutParams(layoutParams);

        FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(mBoxWidth, mBoxHeight, Gravity.CENTER_HORIZONTAL);
        layoutParams3.setMargins((mScreenWidth - mBoxWidth) / 2, mBoxTopOffset, (mScreenWidth - mBoxWidth) / 2, mScreenHeight - mBoxTopOffset - mBoxHeight);
//        mAnimateView.setLayoutParams(layoutParams3);
        flAnimate.setLayoutParams(layoutParams3);

        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(mBoxWidth, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);
        layoutParams2.setMargins((mScreenWidth - mBoxWidth) / 2,
                (int) (mBoxTopOffset + mBoxHeight + mScreenHeight * BOTTOM_TXT_MARGIN_TOP_RATE),
                (mScreenWidth - mBoxWidth) / 2, 0);
//        mTextView.setLayoutParams(layoutParams2);
        llBottomText.setLayoutParams(layoutParams2);

        mShaderBottom = (ShadeView) findViewById(R.id.shade_bottom);
        mShaderLeft = (ShadeView) findViewById(R.id.shade_left);
        mShaderRight = (ShadeView) findViewById(R.id.shade_right);
        mShaderTop = (ShadeView) findViewById(R.id.shade_top);

        initShadeView();

        mCamera = getCameraInstance();
        if(mCamera == null){

            throw new RuntimeException("Camera is null");
        }

        /* Instance barcode scanner */
        scanner = new ImageScanner();

        //设置采样密度，数字越大密度越小，最小值为1
        scanner.setConfig(0, Config.X_DENSITY, 2);
        scanner.setConfig(0, Config.Y_DENSITY, 2);

        initBestPreviewSize();
        setCameraPreviewSize();
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
    }

    private void initBestPreviewSize() {
        if (mCamera == null) {
//            showToast(R.string.camera_is_unvailable);
//            finish();
            return;
        }
        Parameters parameters = mCamera.getParameters();
        List<Size> supportedPictureSizes = parameters.getSupportedPreviewSizes();
        mBestPreviewSize = selectBestSize(new MySize(mScreenHeight, mScreenWidth), supportedPictureSizes);
    }

    private void initShadeView() {
        FrameLayout.LayoutParams flpTop = new FrameLayout.LayoutParams(mScreenWidth, mBoxTopOffset, Gravity.TOP);
        FrameLayout.LayoutParams flpBottom = new FrameLayout.LayoutParams(mScreenWidth, mScreenHeight - mBoxTopOffset - mBoxHeight, Gravity.BOTTOM);
        FrameLayout.LayoutParams flpLeft = new FrameLayout.LayoutParams((mScreenWidth - mBoxWidth) / 2, mBoxHeight, Gravity.LEFT);
        FrameLayout.LayoutParams flpRight = new FrameLayout.LayoutParams((mScreenWidth - mBoxWidth) / 2, mBoxHeight, Gravity.RIGHT);
        flpLeft.setMargins(0, mBoxTopOffset, 0, mScreenHeight - mBoxTopOffset - mBoxHeight);
        flpRight.setMargins(0, mBoxTopOffset, 0, mScreenHeight - mBoxTopOffset - mBoxHeight);

        mShaderBottom.setLayoutParams(flpBottom);
        mShaderLeft.setLayoutParams(flpLeft);
        mShaderRight.setLayoutParams(flpRight);
        mShaderTop.setLayoutParams(flpTop);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            mFlag = false;
            mIsScaned = false;
            if (mCamera == null) {
                mCamera = getCameraInstance();

                if (mCamera == null) {
                    throw new RuntimeException("Camera is null");
                } else {
                    mPreview.setCamera(mCamera);
                    setCameraPreviewSize();
//                    mAnimateView.startAnimate();
                    ivAnimate.startAnimation(scanAnim);
                    startPreview();
                }
                mPreview.reSetHolder();
                startAnalysing();
            } else {
                mPreview.setCamera(mCamera);
                setCameraPreviewSize();
//                mAnimateView.startAnimate();
                ivAnimate.startAnimation(scanAnim);
                startPreview();
                startAnalysing();
            }
            isFocus = true;
        }catch (Exception e){
            if(!dlgShown) {
                showCameraDlg();
            }
//            showToast(R.string.camera_is_unvailable);
            Log.i(TAG, Log.getStackTraceString(e));
            return;
        }
    }

    private void setCameraPreviewSize() {
        if (mCamera == null) {
            //LogCore
            return;
        }
        if (mBestPreviewSize == null) {
            return;
        } else {
//            try {
                Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(mBestPreviewSize.width, mBestPreviewSize.height);
                mCamera.setParameters(parameters);
            /*} catch (RuntimeException e) {
                showActionPrompt2(R.string.camera_error);
                LogCore.i(TAG, Log.getStackTraceString(e));
                finish();
            }*/
        }
    }

    private void startPreview() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {

            @Override
            public void run() {

                if (mCamera == null) {

                    return;
                }
                mCamera.setPreviewCallback(previewCb);

            }
        }, 10, ScannerActivity.PREVIEW_CALLBACK_VALID_PERIOD);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFlag = true;
        /*if(mAnimateView != null) {
            mAnimateView.stopAnimate();
        }*/

        if(mTimer != null) {
            mTimer.cancel();
        }

        if(mCamera != null) {
            if(mPreview != null) {
                mPreview.setCamera(null);
            }
            releaseCamera();
            synchronized (mLock) {
                mFlag = true;
                mLock.notify();
            }
        }

        isFocus = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();

        } catch (Exception e) {
            Log.i(TAG, Log.getStackTraceString(e));
        }
        return c;
    }

    private void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }catch (Exception ignore){

        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (isFocus && mCamera != null) {
                mCamera.autoFocus(autoFocusCB);
            }

        }
    };

    private MySize selectBestSize(MySize expected, List<Size> sizes) {
        float expectedRate = expected.expectedRate;
        List<MySize> mySizes = new ArrayList<MySize>();
        for (Size s : sizes) {
            MySize mySize = new MySize(s.width, s.height, expectedRate);
            mySizes.add(mySize);
        }
        Collections.sort(mySizes);
        MySize remained = mySizes.get(0);
        for (int i = 0; i < mySizes.size(); i++) {
            if (isAcceptable(expected, mySizes.get(i))) {
                return mySizes.get(i);
            }
        }

        return remained;
    }

    private boolean isAcceptable(MySize expected, MySize actual) {
        if (actual.width > expected.width || actual.height > expected.height) {
            return false;
        }
         /*if(Math.abs(expected.width - actual.width) - expected.width < DELTA_ACCEPTABLE && Math.abs(expected.height - actual.height) - expected.height < DELTA_ACCEPTABLE){
             return true;
    	 }*/
//    	 return false;
        return true;
    }    // Mimic continuous auto-focusing

    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            mHandler.postDelayed(doAutoFocus, FOCUS_PERIOD);
        }
    };

    private class MySize implements Comparable<MySize> {
        public int width;
        public int height;

        public float expectedRate;

        public MySize(int width, int height, float expectedRate) {
            this.width = width;
            this.height = height;
            this.expectedRate = expectedRate;
        }

        public MySize(int width, int height) {
            this.width = width;
            this.height = height;
            this.expectedRate = width / (float) height;
        }

        @Override
        public int compareTo(MySize arg0) {
            float delta = Math.abs(this.width / (float) this.height - this.expectedRate) - Math.abs(arg0.width / (float) arg0.height - arg0.expectedRate);
            return (int) (delta * 10000000);
        }
    }


}
