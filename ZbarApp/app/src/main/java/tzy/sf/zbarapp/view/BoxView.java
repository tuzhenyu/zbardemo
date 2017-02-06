package tzy.sf.zbarapp.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import tzy.sf.zbarapp.R;

public class BoxView extends View {

    private Context mContext;
    private int mCornnerColor = 0xee21ee12;
    private int mFrameColor = 0xaaffeeff;
    private Paint mCornnerPaint;
    private Paint mFramePaint;

    private float mCornnerLineLengthRate;
    private int mCornnerLineLength;
    private int mCornnerLineWidthPx;
    private int mFrameLineWidthPx;


    public BoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mContext = context;
        initAttrs(attrs);
        initPaint();
    }


    private void initAttrs(AttributeSet attrs) {
        TypedArray types = mContext.obtainStyledAttributes(attrs, R.styleable.BoxView);
        mCornnerColor = types.getColor(R.styleable.BoxView_cornnerColor, 0xee21ee12);
        mFrameColor = types.getColor(R.styleable.BoxView_frameColor, 0xaaffeeff);
        mCornnerLineLengthRate = types.getFloat(R.styleable.BoxView_cornnerLineLengthRate, 0.067f);
        mCornnerLineWidthPx = types.getDimensionPixelSize(R.styleable.BoxView_cornnerLineWidth, 5);
        mFrameLineWidthPx = types.getDimensionPixelSize(R.styleable.BoxView_frameLineWidth, 1);

        types.recycle();
    }

    private void initPaint() {
        mCornnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCornnerPaint.setColor(mCornnerColor);
        mCornnerPaint.setStrokeWidth(mCornnerLineWidthPx);

        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setColor(mFrameColor);
        mFramePaint.setStrokeWidth(mFrameLineWidthPx);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        int width = getMeasuredWidth();
        int height = width;

        mCornnerLineLength = (int) (mCornnerLineLengthRate * width);
        canvas.drawLines(new float[]{0, 0, width, 0f,
                width - 1, 0f, width - 1, height - 1,
                width - 1, height - 1, 0f, height - 1,
                0f, height - 1, 0f, 0f}, mFramePaint);
        canvas.drawLines(new float[]{0f, mCornnerLineLength, 0f, 0f, 0f, 0f, mCornnerLineLength, 0f,
                width - mCornnerLineLength, 0f, width, 0f, width, 0f, width, mCornnerLineLength,
                width, height - mCornnerLineLength, width, height, width, height, width - mCornnerLineLength, height,
                mCornnerLineLength, height, 0f, height, 0f, height, 0f, height - mCornnerLineLength}, mCornnerPaint);

    }


}