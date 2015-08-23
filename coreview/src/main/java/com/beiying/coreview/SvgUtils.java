package com.beiying.coreview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;

import com.caverock.androidsvg.PreserveAspectRatio;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by beiying on 15/8/20.
 */
public class SvgUtils {
    /**
     * It is for logging purposes.
     */
    private static final String LOG_TAG = "SVGUtils";
    /**
     * All the paths with their attributes from the svg.
     */
    private final List<SvgPath> mPaths = new ArrayList<SvgPath>();
    /**
     * The paint provided from the view.
     */
    private final Paint mSourcePaint;

    public SvgUtils(final Paint sourcePaint) {
        mSourcePaint = sourcePaint;
    }

    private SVG mSvg;
    public void load(Context context, int svgRes) {
        if (mSvg != null) return;
        try {
            mSvg = SVG.getFromResource(context, svgRes);
            mSvg.setDocumentPreserveAspectRatio(PreserveAspectRatio.UNSCALED);
        } catch (SVGParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Draw the svg to the canvas
     * @param canvas
     * @param width
     * @param height
     */
    public void drawSvgAfter(final Canvas canvas, final int width, final int height) {
        final float strokeWidth = mSourcePaint.getStrokeWidth();
        rescaleCanvas(width,height,strokeWidth,canvas);
    }

    /**
     * Render the svg to canvas and catch all the path while rendering
     * @param width
     * @param height
     * @return
     */
    public List<SvgPath> getPathsForViewport(final int width, final int height) {
        final float strokeWidth = mSourcePaint.getStrokeWidth();
        Canvas canvas = new Canvas() {
            private final Matrix mMatrix = new Matrix();

            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public void drawPath(Path path, Paint paint) {
                Path dst = new Path();
                getMatrix(mMatrix);
                path.transform(mMatrix, dst);
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(strokeWidth);
                mPaths.add(new SvgPath(dst, paint));
            }
        };
        rescaleCanvas(width, height, strokeWidth, canvas);
        return mPaths;
    }

    /**
     * Rescale the canvas width specific width and height;
     * @param width
     * @param height
     * @param strokeWidth
     * @param canvas
     */
    private void rescaleCanvas(int width, int height, float strokeWidth, Canvas canvas) {
        final RectF viewBox = mSvg.getDocumentViewBox();
        final float scale = Math.min(width / (viewBox.width() + strokeWidth), height / (viewBox.height() + strokeWidth));

        canvas.translate(width - viewBox.width() * scale / 2.0f, (height - viewBox.height() * scale) /2.0f);
        canvas.scale(scale, scale);
        mSvg.renderToCanvas(canvas);
    }

    public static class SvgPath {
        /**
         * Region of path
         */
        private static final Region mRegion = new Region();
        /**
         * This is done for clipping the bounds of the path
         */
        private static final Region MAX_CLIP = new Region(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

        final Path mPath;
        final Paint mPaint;
        final float mLength;
        final Rect mBounds;
        final PathMeasure mMeasure;

        public SvgPath(Path path, Paint paint) {
            this.mPath = path;
            this.mPaint = paint;

            mMeasure = new PathMeasure(path, false);
            this.mLength = mMeasure.getLength();

            mRegion.setPath(path, MAX_CLIP);
            mBounds = mRegion.getBounds();
        }


    }
}
