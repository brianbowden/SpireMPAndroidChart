
package com.github.mikephil.charting.charts;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;

import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.FillFormatter;

import java.util.ArrayList;

/**
 * Chart that draws lines, surfaces, circles, ...
 * 
 * @author Philipp Jahoda
 */
public class LineChart extends BarLineChartBase<LineData> {

    /** the width of the highlighning line */
    protected float mHighlightWidth = 3f;

    /** paint for the outer circle of the value indicators */
    protected Paint mCirclePaintOuter;

    /** paint for the inner circle of the value indicators */
    protected Paint mCirclePaintInner;

    /** paint for the data point stems */
    protected Paint mStemPaint;

    private OnSelectedPointDrawnListener mSelectedPointDrawnListener;

    private FillFormatter mFillFormatter;

    private DataSet<Entry> mPreviousDataSet;
    private DataSet<Entry> mCurrentDataSet;
    private float[] mPreviousEntryCoords;
    private float[] mPreviousEntryNewValues;
    private boolean mPreviousEntryPositionsCreated;
    private boolean mUseMorph;

    public LineChart(Context context) {
        super(context);
    }

    public LineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mPreviousEntryCoords = new float[0];
        mPreviousEntryNewValues = new float[0];

        mFillFormatter = new DefaultFillFormatter();

        mCirclePaintOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaintOuter.setStyle(Paint.Style.STROKE);
        mCirclePaintOuter.setStrokeWidth(2f);
        mCirclePaintOuter.setColor(Color.WHITE);

        mCirclePaintInner = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaintInner.setStyle(Paint.Style.FILL);
        mCirclePaintInner.setColor(Color.GRAY);

        mStemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStemPaint.setStyle(Paint.Style.STROKE);
        mStemPaint.setStrokeWidth(1f);
        mStemPaint.setPathEffect(new DashPathEffect(new float[] { 3, 3 }, 0));
        mStemPaint.setColor(0xCCFFFFFF);

        mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHighlightPaint.setStyle(Paint.Style.STROKE);
        mHighlightPaint.setStrokeWidth(2f);
        mHighlightPaint.setColor(Color.rgb(255, 187, 115));        
    }

    @Override
    protected void calcMinMax(boolean fixedValues) {
        super.calcMinMax(fixedValues);

        // // if there is only one value in the chart
        // if (mOriginalData.getYValCount() == 1
        // || mOriginalData.getYValCount() <= mOriginalData.getDataSetCount()) {
        // mDeltaX = 1;
        // }

        if (mDeltaX == 0 && mData.getYValCount() > 0)
            mDeltaX = 1;
    }

    @Override
    protected void drawHighlights() {

        for (int i = 0; i < mIndicesToHightlight.length; i++) {

            LineDataSet set = mData.getDataSetByIndex(mIndicesToHightlight[i]
                    .getDataSetIndex());

            if (set == null)
                continue;

            mHighlightPaint.setColor(set.getHighLightColor());

            int xIndex = mIndicesToHightlight[i].getXIndex(); // get the
                                                              // x-position

            if (xIndex > mDeltaX * mPhaseX)
                continue;

            float y = set.getYValForXIndex(xIndex) * mPhaseY; // get the
                                                              // y-position

            float[] pts = new float[] {
                    xIndex, mYChartMax, xIndex, mYChartMin, 0, y, mDeltaX, y
            };

            mTrans.pointValuesToPixel(pts);

            // draw the highlight lines
            mDrawCanvas.drawLines(pts, mHighlightPaint);
        }
    }

    /**
     * Class needed for saving the points when drawing cubic-lines.
     * 
     * @author Philipp Jahoda
     */
    private class CPoint {

        public float x = 0f;
        public float y = 0f;

        /** x-axis distance */
        public float dx = 0f;

        /** y-axis distance */
        public float dy = 0f;

        public CPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * draws the given y values to the screen
     */
    @Override
    protected void drawData() {

        ArrayList<LineDataSet> dataSets = mData.getDataSets();
        mCurrentDataSet = dataSets.get(0);

        if (mPhaseY == 0.0f) {
            mPreviousEntryNewValues = mPreviousEntryCoords;
            mTrans.pixelsToValue(mPreviousEntryNewValues);
            mPreviousEntryPositionsCreated = true;
        }

        for (int i = 0; i < mData.getDataSetCount(); i++) {

            LineDataSet dataSet = dataSets.get(i);
            ArrayList<Entry> entries = dataSet.getYVals();

            if (entries.size() < 1)
                continue;

            mRenderPaint.setStrokeWidth(dataSet.getLineWidth());
            mRenderPaint.setPathEffect(dataSet.getDashPathEffect());

            // if drawing cubic lines is enabled
            if (dataSet.isDrawCubicEnabled()) {

                // get the color that is specified for this position from the
                // DataSet
                mRenderPaint.setColor(dataSet.getColor());

                float intensity = dataSet.getCubicIntensity();

                // the path for the cubic-spline
                Path spline = new Path();

                ArrayList<CPoint> points = new ArrayList<CPoint>();
                for (Entry e : entries)
                    points.add(new CPoint(e.getXIndex(), e.getVal()));

                if (points.size() > 1) {
                    for (int j = 0; j < points.size() * mPhaseX; j++) {

                        CPoint point = points.get(j);

                        if (j == 0) {
                            CPoint next = points.get(j + 1);
                            point.dx = ((next.x - point.x) * intensity);
                            point.dy = ((next.y - point.y) * intensity);
                        }
                        else if (j == points.size() - 1) {
                            CPoint prev = points.get(j - 1);
                            point.dx = ((point.x - prev.x) * intensity);
                            point.dy = ((point.y - prev.y) * intensity);
                        }
                        else {
                            CPoint next = points.get(j + 1);
                            CPoint prev = points.get(j - 1);
                            point.dx = ((next.x - prev.x) * intensity);
                            point.dy = ((next.y - prev.y) * intensity);
                        }

                        // create the cubic-spline path
                        if (j == 0) {
                            spline.moveTo(point.x, point.y * mPhaseY);
                        }
                        else {
                            CPoint prev = points.get(j - 1);
                            spline.cubicTo(prev.x + prev.dx, (prev.y + prev.dy) * mPhaseY, point.x
                                    - point.dx,
                                    (point.y - point.dy) * mPhaseY, point.x, point.y * mPhaseY);
                        }
                    }
                }

                // if filled is enabled, close the path
                if (dataSet.isDrawFilledEnabled()) {

                    float fillMin = mFillFormatter
                            .getFillLinePosition(dataSet, mData, mYChartMax, mYChartMin);

                    spline.lineTo((entries.size() - 1) * mPhaseX, fillMin);
                    spline.lineTo(0, fillMin);
                    spline.close();

                    mRenderPaint.setStyle(Paint.Style.FILL);
                } else {
                    mRenderPaint.setStyle(Paint.Style.STROKE);
                }

                mTrans.pathValueToPixel(spline);

                mDrawCanvas.drawPath(spline, mRenderPaint);

                // draw normal (straight) lines
            } else {

                mRenderPaint.setStyle(Paint.Style.STROKE);

                // more than 1 color
                if (dataSet.getColors() == null || dataSet.getColors().size() > 1) {

                    float[] valuePoints = mTrans.generateTransformedValuesLineScatter(entries, mPhaseY);

                    for (int j = 0; j < (valuePoints.length - 2) * mPhaseX; j += 2) {

                        if (isOffContentRight(valuePoints[j]))
                            break;

                        // make sure the lines don't do shitty things outside
                        // bounds
                        if (j != 0 && isOffContentLeft(valuePoints[j - 1])
                                && isOffContentTop(valuePoints[j + 1])
                                && isOffContentBottom(valuePoints[j + 1]))
                            continue;

                        // get the color that is set for this line-segment
                        mRenderPaint.setColor(dataSet.getColor(j / 2));

                        mDrawCanvas.drawLine(valuePoints[j], valuePoints[j + 1],
                                valuePoints[j + 2], valuePoints[j + 3], mRenderPaint);
                    }

                } else { // only one color per dataset

                    mRenderPaint.setColor(dataSet.getColor());

                    Path line = generateLinePath(entries);
                    mTrans.pathValueToPixel(line);

                    mDrawCanvas.drawPath(line, mRenderPaint);
                }

                mRenderPaint.setPathEffect(null);

                // if drawing filled is enabled
                if (dataSet.isDrawFilledEnabled() && entries.size() > 0) {
                    // mDrawCanvas.drawVertices(VertexMode.TRIANGLE_STRIP,
                    // valuePoints.length, valuePoints, 0,
                    // null, 0, null, 0, null, 0, 0, paint);

                    mRenderPaint.setStyle(Paint.Style.FILL);

                    mRenderPaint.setColor(dataSet.getFillColor());
                    // filled is drawn with less alpha
                    mRenderPaint.setAlpha(dataSet.getFillAlpha());

                    // mRenderPaint.setShader(dataSet.getShader());

                    Path filled = generateFilledPath(entries,
                            mFillFormatter.getFillLinePosition(dataSet, mData, mYChartMax,
                                    mYChartMin));

                    mTrans.pathValueToPixel(filled);

                    if (mUseMorph) {

                    }

                    mDrawCanvas.drawPath(filled, mRenderPaint);

                    // restore alpha
                    mRenderPaint.setAlpha(255);
                    // mRenderPaint.setShader(null);
                }
            }

            mRenderPaint.setPathEffect(null);
        }
    }
    
    /**
     * Generates the path that is used for filled drawing.
     * 
     * @param entries
     * @return
     */
    private Path generateFilledPath(ArrayList<Entry> entries, float fillMin) {

        Path filled = new Path();

        float yMultiplier = mUseMorph ? 1 : mPhaseY;
        float yStartValue = entries.get(0).getVal() * yMultiplier;
        if (mUseMorph) {
            if (mPreviousEntryNewValues.length > 0) {
                yStartValue = mPreviousEntryNewValues[1] + (yStartValue - mPreviousEntryNewValues[1]) * mPhaseY;
            } else {
                yStartValue = yStartValue * mPhaseY;
            }
        }

        filled.moveTo(entries.get(0).getXIndex(), yStartValue);

        // create a new path
        for (int x = 1; x < entries.size() * mPhaseX; x++) {
            Entry e = entries.get(x);
            float yValue = e.getVal() * yMultiplier;

            if (mUseMorph) {
                if (mPreviousEntryNewValues.length > (x * 2 + 1)) {
                    yValue = mPreviousEntryNewValues[x * 2 + 1] + (yValue - mPreviousEntryNewValues[x * 2 + 1]) * mPhaseY;
                } else {
                    yValue = yValue * mPhaseY;
                }
            }

            filled.lineTo(e.getXIndex(), yValue);
        }

        // close up
        filled.lineTo(entries.get((int) ((entries.size() - 1) * mPhaseX)).getXIndex(), fillMin);
        filled.lineTo(entries.get(0).getXIndex(), fillMin);
        filled.close();

        return filled;
    }

    /**
     * Generates the path that is used for drawing a single line.
     * 
     * @param entries
     * @return
     */
    private Path generateLinePath(ArrayList<Entry> entries) {

        Path line = new Path();

        float yMultiplier = mUseMorph ? 1 : mPhaseY;
        float yStartValue = entries.get(0).getVal() * yMultiplier;
        if (mUseMorph) {
            if (mPreviousEntryNewValues.length > 0) {
                yStartValue = mPreviousEntryNewValues[1] + (yStartValue - mPreviousEntryNewValues[1]) * mPhaseY;
            } else {
                yStartValue = yStartValue * mPhaseY;
            }
        }

        line.moveTo(entries.get(0).getXIndex(), yStartValue);

        // create a new path
        for (int x = 1; x < entries.size() * mPhaseX; x++) {
            Entry e = entries.get(x);
            float yValue = e.getVal() * yMultiplier;

            if (mUseMorph) {
                if (mPreviousEntryNewValues.length > (x * 2 + 1)) {
                    yValue = mPreviousEntryNewValues[x * 2 + 1] + (yValue - mPreviousEntryNewValues[x * 2 + 1]) * mPhaseY;
                } else {
                    yValue = yValue * mPhaseY;
                }
            }

            line.lineTo(e.getXIndex(), yValue);
        }

        return line;
    }

    @Override
    protected void drawValues() {

        // if values are drawn
        if (mDrawYValues && mData.getYValCount() < mMaxVisibleCount * mTrans.getScaleX()) {

            ArrayList<LineDataSet> dataSets = mData.getDataSets();

            for (int i = 0; i < mData.getDataSetCount(); i++) {

                LineDataSet dataSet = dataSets.get(i);

                // make sure the values do not interfear with the circles
                int valOffset = (int) (dataSet.getCircleSize() * 1.75f);

                if (!dataSet.isDrawCirclesEnabled())
                    valOffset = valOffset / 2;

                ArrayList<Entry> entries = dataSet.getYVals();

                float[] positions = mTrans.generateTransformedValuesLineScatter(entries, mPhaseY);

                for (int j = 0; j < positions.length * mPhaseX; j += 2) {

                    if (isOffContentRight(positions[j]))
                        break;

                    if (isOffContentLeft(positions[j]) || isOffContentTop(positions[j + 1])
                            || isOffContentBottom(positions[j + 1]))
                        continue;

                    float val = entries.get(j / 2).getVal();

                    if (mDrawUnitInChart) {

                        mDrawCanvas.drawText(mValueFormatter.getFormattedValue(val) + mUnit,
                                positions[j],
                                positions[j + 1]
                                        - valOffset, mValuePaint);
                    } else {

                        mDrawCanvas.drawText(mValueFormatter.getFormattedValue(val), positions[j],
                                positions[j + 1] - valOffset,
                                mValuePaint);
                    }
                }
            }
        }
    }

    /**
     * draws the circle value indicators
     */
    @Override
    protected void drawAdditional() {

        ArrayList<LineDataSet> dataSets = mData.getDataSets();

        for (int i = 0; i < mData.getDataSetCount(); i++) {

            LineDataSet dataSet = dataSets.get(i);

            // if drawing circles is enabled for this dataset
            if (dataSet.isDrawCirclesEnabled()) {

                ArrayList<Entry> entries = dataSet.getYVals();

                float[] positions = mTrans.generateTransformedValuesLineScatter(entries, mUseMorph ? 1.0f : mPhaseY);
                float[] prevPositions = new float[0];

                if (mUseMorph && mPreviousEntryNewValues.length > 0) {
                    prevPositions = mPreviousEntryNewValues.clone();
                    mTrans.pointValuesToPixel(prevPositions);
                }

                for (int j = 0; j < positions.length * mPhaseX; j += 2) {

                    // Set the color for the currently drawn value. If the index
                    // is
                    // out of bounds, reuse colors.
                    mCirclePaintOuter.setColor(dataSet.getCircleColor(j / 2));

                    int originalInnerColor = -1;
                    int originalStemColor = -1;
                    float originalStemWidth = -1;

                    float yValue = positions[j + 1];
                    if (mUseMorph && prevPositions.length > j + 1) {
                        yValue = prevPositions[j + 1] - ((prevPositions[j + 1] - yValue) * mPhaseY);

                    } else {
                        yValue = yValue + (getHeight() - mOffsetBottom - yValue) * (1.0f - mPhaseY);
                    }


                    if (mSelectedValueIndex == j / 2) {
                        originalInnerColor = mCirclePaintInner.getColor();
                        mCirclePaintInner.setColor(mCirclePaintOuter.getColor());
                        originalStemColor = mStemPaint.getColor();
                        mStemPaint.setColor(mCirclePaintOuter.getColor());
                        originalStemWidth = mStemPaint.getStrokeWidth();
                        mStemPaint.setStrokeWidth(originalStemWidth * 1.5f);

                        if (mSelectedPointDrawnListener != null) {
                            mSelectedPointDrawnListener.onPointDrawn(positions[j], yValue);
                        }
                    }

                    if (isOffContentRight(positions[j]))
                        break;

                    // make sure the circles don't do shitty things outside
                    // bounds
                    if (isOffContentLeft(positions[j]) ||
                            isOffContentTop(yValue)
                            || isOffContentBottom(yValue))
                        continue;

                    mDrawCanvas.drawLine(positions[j], getHeight() - mOffsetBottom,
                            positions[j], yValue, mStemPaint);

                    mDrawCanvas.drawCircle(positions[j], yValue,
                            dataSet.getCircleSize(), mCirclePaintInner);
                    mDrawCanvas.drawCircle(positions[j], yValue,
                            dataSet.getCircleSize(), mCirclePaintOuter);

                    if (originalInnerColor != -1) {
                        mCirclePaintInner.setColor(originalInnerColor);
                        mStemPaint.setColor(originalStemColor);
                        mStemPaint.setStrokeWidth(originalStemWidth);
                    }
                }
            } // else do nothing

        }

        if (mPhaseY == 1.0f) {
            mPreviousDataSet = mCurrentDataSet;
            mPreviousEntryPositionsCreated = false;
            mPreviousEntryCoords = mTrans.generateTransformedValuesLineScatter(mCurrentDataSet.getYVals(), 1.0f);
        }
    }

    public void setUseMorph(boolean useMorph) {
        mUseMorph = useMorph;
    }

    /**
     * set the width of the highlightning lines, default 3f
     * 
     * @param width
     */
    public void setHighlightLineWidth(float width) {
        mHighlightWidth = width;
    }

    /**
     * returns the width of the highlightning line, default 3f
     * 
     * @return
     */
    public float getHighlightLineWidth() {
        return mHighlightWidth;
    }

    @Override
    public void setPaint(Paint p, int which) {
        super.setPaint(p, which);

        switch (which) {
            case PAINT_CIRCLES_OUTER:
                mCirclePaintOuter = p;
                break;
            case PAINT_CIRCLES_INNER:
                mCirclePaintInner = p;
                break;
            case PAINT_STEM_LINE:
                mStemPaint = p;
                break;
        }
    }

    @Override
    public Paint getPaint(int which) {
        Paint p = super.getPaint(which);
        if (p != null)
            return p;

        switch (which) {
            case PAINT_CIRCLES_OUTER:
                return mCirclePaintOuter;
            case PAINT_CIRCLES_INNER:
                return mCirclePaintInner;
            case PAINT_STEM_LINE:
                return mStemPaint;
        }

        return null;
    }

    /**
     * Sets a custom FillFormatter to the chart that handles the position of the
     * filled-line for each DataSet. Set this to null to use the default logic.
     * 
     * @param formatter
     */
    public void setFillFormatter(FillFormatter formatter) {

        if (formatter == null)
            formatter = new DefaultFillFormatter();

        mFillFormatter = formatter;
    }

    /**
     * Default formatter that calculates the position of the filled line.
     * 
     * @author Philipp Jahoda
     */
    private class DefaultFillFormatter implements FillFormatter {

        @Override
        public float getFillLinePosition(LineDataSet dataSet, LineData data,
                float chartMaxY, float chartMinY) {

            float fillMin = 0f;

            if (dataSet.getYMax() > 0 && dataSet.getYMin() < 0) {
                fillMin = 0f;
            } else {

                if (!mStartAtZero) {

                    float max, min;

                    if (data.getYMax() > 0)
                        max = 0f;
                    else
                        max = chartMaxY;
                    if (data.getYMin() < 0)
                        min = 0f;
                    else
                        min = chartMinY;

                    fillMin = dataSet.getYMin() >= 0 ? min : max;
                } else {
                    fillMin = 0f;
                }

            }

            return fillMin;
        }
    }

    public void setOnSelectedPointDrawnListener(OnSelectedPointDrawnListener listener) {
        mSelectedPointDrawnListener = listener;
    }

    public interface OnSelectedPointDrawnListener {
        public void onPointDrawn(float x, float y);
    }
}
