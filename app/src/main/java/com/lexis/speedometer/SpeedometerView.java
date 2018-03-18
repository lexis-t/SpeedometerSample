package com.lexis.speedometer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class SpeedometerView extends View {

    private static final double START_DEGREES = Math.toRadians(225);
    private static final double BOUND_DEGREES = Math.toRadians(270);

    private int[] marks;
    private int maxValue;

    private int scale;

    private String label;
    private int textColor;
    private float textSize;
    private int color;
    private float drawWidth;

    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;
    private int contentWidth;
    private int contentHeight;

    private Rect bounds;
    private int radius;
    private int radius1;
    private int Cx;
    private int Cy;
    private List<PointF> markPoints;
    private List<PointF> markPoints1;
    private List<PointF> markTextPoints;
    private List<String> markStrings;
    private PointF labelPoint;

    private Rect textMeasureRect;

    private Paint textPaint;
    private Paint paint;
    private Paint arrowPaint;
    private Paint realPaint;

    private int channel;
    private float value;
    private float realValue;

    public SpeedometerView(Context context) {
        super(context);
        init(null, 0);
    }

    public SpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public SpeedometerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.com_lexis_speedometer_SpeedometerView, defStyle, 0);

        maxValue = a.getInteger(R.styleable.com_lexis_speedometer_SpeedometerView_maximum, 1);
        int marksResId = a.getResourceId(R.styleable.com_lexis_speedometer_SpeedometerView_marks, -1);
        marks = (marksResId != -1) ? getContext().getResources().getIntArray(marksResId) : new int[] {};
        scale = a.getInteger(R.styleable.com_lexis_speedometer_SpeedometerView_scale, 1);
        label = a.getString(R.styleable.com_lexis_speedometer_SpeedometerView_label);
        textColor = a.getColor(R.styleable.com_lexis_speedometer_SpeedometerView_text_color, getContext().getResources().getColor(android.R.color.white, null));
        textSize = a.getDimension(R.styleable.com_lexis_speedometer_SpeedometerView_text_size, 50f);
        color = a.getColor(R.styleable.com_lexis_speedometer_SpeedometerView_color, getContext().getResources().getColor(android.R.color.white, null));
        drawWidth = a.getDimension(R.styleable.com_lexis_speedometer_SpeedometerView_draw_width, 2f);
        channel = a.getInteger(R.styleable.com_lexis_speedometer_SpeedometerView_channel, 0);

        bounds = new Rect();

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(textSize);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(drawWidth);

        realPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        realPaint.setColor(getContext().getResources().getColor(android.R.color.holo_red_light, null));
        realPaint.setStyle(Paint.Style.STROKE);
        realPaint.setStrokeWidth(drawWidth);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(color);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setColor(getContext().getResources().getColor(android.R.color.white, null));
        arrowPaint.setStrokeWidth(drawWidth/3);

        a.recycle();

        markPoints = new ArrayList<>(marks.length);
        markPoints1 = new ArrayList<>(marks.length);
        markTextPoints = new ArrayList<>(marks.length);
        markStrings = new ArrayList<>(marks.length);
        for (int m : marks ) {
            if (m > maxValue) {
                break;
            }

            markPoints.add(new PointF());
            markPoints1.add(new PointF());
            markTextPoints.add(new PointF());
            markStrings.add(String.valueOf(m));
        }
        labelPoint = new PointF();

        textMeasureRect = new Rect();

        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        paddingRight = getPaddingRight();
        paddingBottom = getPaddingBottom();
    }

    public float getMaxValue() {
        return maxValue;
    }

    public void setRealValue(float value) {
        this.realValue = value;
    }

    public void setValue(float value) {
        this.value = value;
        invalidate();
    }

    public float getValue() {
        return value;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        measure();
    }

    private void measure() {
        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        if (contentHeight > 0 && contentWidth > 0 && (contentWidth != this.contentWidth || contentHeight != this.contentHeight)) {
            this.contentWidth = contentWidth;
            this.contentHeight = contentHeight;

            bounds.set(paddingLeft, paddingTop, paddingLeft + contentWidth, paddingTop + contentHeight);
            radius = Math.min(contentHeight, contentWidth) * 2 / 6;
            radius1 = radius * 9 / 10;
            Cx = getWidth() / 2;
            Cy = getHeight() / 2;

            textPaint.getTextBounds(label, 0, label.length(), textMeasureRect);
            labelPoint.set(Cx - textMeasureRect.centerX(), Cy + radius / 2 + textMeasureRect.height());

            int i = 0;
            for (int mark : marks) {
                int m = mark * scale;
                if (m > maxValue) {
                    break;
                }

                PointF p = markPoints.get(i);
                PointF p1 = markPoints1.get(i);
                PointF pt = markTextPoints.get(i);
                String s = markStrings.get(i);
                ++i;

                double rad = START_DEGREES - BOUND_DEGREES * (double) m / maxValue;

                double cos = Math.cos(rad);
                double sin = Math.sin(rad);

                float x = Cx + (float) (cos * radius);
                float y = Cy - (float) (sin * radius);
                p.set(x, y);

                float x1 = Cx + (float) (cos * radius1);
                float y1 = Cy - (float) (sin * radius1);
                p1.set(x1, y1);


                textPaint.getTextBounds(s, 0, s.length(), textMeasureRect);
                double Rt = (double) radius * 1.1d;

                float Tx = Cx + (float) ((cos * Rt) + (cos - 1) * textMeasureRect.width() / 2);
                float Ty = Cy - (float) ((sin * Rt) + (sin - 1) * textMeasureRect.height() / 2);

                pt.set(Tx, Ty);
            }
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        measure(); // Work around ViewPager behavior

        getBackground().draw(canvas);

        canvas.clipRect(bounds);

        canvas.drawRect(bounds, paint);
        canvas.drawCircle(Cx, Cy, radius, paint);
        canvas.drawCircle(Cx, Cy, radius * 3 / 100, paint);

        for (int i = 0; i < markPoints.size(); ++i) {
            PointF p = markPoints.get(i);
            PointF p1 = markPoints1.get(i);
            PointF pt = markTextPoints.get(i);

            canvas.drawLine(p.x, p.y, p1.x, p1.y, paint);
            canvas.drawText(markStrings.get(i), pt.x, pt.y, textPaint);
        }

        canvas.drawText(label, labelPoint.x, labelPoint.y, textPaint);

        double rad = START_DEGREES - BOUND_DEGREES * value / maxValue;
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        float x = Cx + (float) (cos * radius1);
        float y = Cy - (float) (sin * radius1);

        canvas.drawLine(Cx, Cy, x, y, paint);
        canvas.drawLine(Cx, Cy, x, y, arrowPaint);

//        double rad1 = START_DEGREES - BOUND_DEGREES * realValue / maxValue;
//        double cos1 = Math.cos(rad1);
//        double sin1 = Math.sin(rad1);
//        float x1 = Cx + (float) (cos1 * radius);
//        float y1 = Cy - (float) (sin1 * radius);
//        canvas.drawCircle(x1, y1, radius * 2 / 100, realPaint);

    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }
}
