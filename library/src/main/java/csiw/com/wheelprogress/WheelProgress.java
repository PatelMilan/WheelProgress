package csiw.com.wheelprogress;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;


/*************************************
 ** Created by Milan on 25/05/2016. **
 *************************************/



public class WheelProgress extends View {
    private static final String TAG = WheelProgress.class.getSimpleName();
    private final int lengthOfBar = 16;
    private final int maxLengthOfBar = 270;
    private final long growingTimeOfPause = 200;
    /**
     * *********
     * DEFAULTS *
     * **********
     */
    //Sizes (with defaults in DP)
    private int radiusOfCircle = 28;
    private int widthOfBar = 4;
    private int widthOfRim = 4;
    private boolean radiusOfFill = false;
    private double startTimeOfGrowing = 0;
    private double timeOfSpinBarCycle = 460;
    private float extraLengthOfBar = 0;
    private boolean fromFrontGrowingBar = true;
    private long timeWithoutOfPauseGrowing = 0;
    //Colors (with defaults)
    private int colorOfBar = 0xAA000000;
    private int colorOfRim = 0x00FFFFFF;

    //Paints
    private Paint paintOfBar = new Paint();
    private Paint paintOfRim = new Paint();

    //Rectangles
    private RectF boundsOfCircle = new RectF();

    //Animation
    //The amount of degrees per second
    private float speedOfSpin = 230.0f;
    //private float speedOfSpin = 120.0f;
    // The last time the spinner was animated
    private long timeOfLastAnimation = 0;

    private boolean ProgressOfLinear;

    private float mProgress = 0.0f;
    private float mTargetProgress = 0.0f;
    private boolean isSpinning = false;

    private ProgressCallback callback;

    private boolean shouldAnimate;

    /**
     * The constructor for the WheelProgress
     */
    public WheelProgress(Context context, AttributeSet attrs) {
        super(context, attrs);

        parseAttributes(context.obtainStyledAttributes(attrs, R.styleable.WheelProgress));

        setAnimationEnabled();
    }

    /**
     * The constructor for the WheelProgress
     */
    public WheelProgress(Context context) {
        super(context);
        setAnimationEnabled();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1) private void setAnimationEnabled() {
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        float animationValue;
        if (currentApiVersion >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            animationValue = Settings.Global.getFloat(getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1);
        } else {
            animationValue = Settings.System.getFloat(getContext().getContentResolver(),
                    Settings.System.ANIMATOR_DURATION_SCALE, 1);
        }

        shouldAnimate = animationValue != 0;
    }

    //----------------------------------
    //Setting up stuff
    //----------------------------------

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int viewWidth = radiusOfCircle + this.getPaddingLeft() + this.getPaddingRight();
        int viewHeight = radiusOfCircle + this.getPaddingTop() + this.getPaddingBottom();

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(viewWidth, widthSize);
        } else {
            //Be whatever you want
            width = viewWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(viewHeight, heightSize);
        } else {
            //Be whatever you want
            height = viewHeight;
        }

        setMeasuredDimension(width, height);
    }

    /**
     * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
     * because this method is called after measuring the dimensions of MATCH_PARENT & WRAP_CONTENT.
     * Use this dimensions to setup the bounds and paints.
     */
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        setupBounds(w, h);
        setupPaints();
        invalidate();
    }

    /**
     * Set the properties of the paints we're using to
     * draw the progress wheel
     */
    private void setupPaints() {
        paintOfBar.setColor(colorOfBar);
        paintOfBar.setAntiAlias(true);
        paintOfBar.setStyle(Style.STROKE);
        paintOfBar.setStrokeWidth(widthOfBar);

        paintOfRim.setColor(colorOfRim);
        paintOfRim.setAntiAlias(true);
        paintOfRim.setStyle(Style.STROKE);
        paintOfRim.setStrokeWidth(widthOfRim);
    }

    /**
     * Set the bounds of the component
     */
    private void setupBounds(int layout_width, int layout_height) {
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();

        if (!radiusOfFill) {
            // Width should equal to Height, find the min value to setup the circle
            int minValue = Math.min(layout_width - paddingLeft - paddingRight,
                    layout_height - paddingBottom - paddingTop);

            int circleDiameter = Math.min(minValue, radiusOfCircle * 2 - widthOfBar * 2);

            // Calc the Offset if needed for centering the wheel in the available space
            int xOffset = (layout_width - paddingLeft - paddingRight - circleDiameter) / 2 + paddingLeft;
            int yOffset = (layout_height - paddingTop - paddingBottom - circleDiameter) / 2 + paddingTop;

            boundsOfCircle =
                    new RectF(xOffset + widthOfBar, yOffset + widthOfBar, xOffset + circleDiameter - widthOfBar,
                            yOffset + circleDiameter - widthOfBar);
        } else {
            boundsOfCircle = new RectF(paddingLeft + widthOfBar, paddingTop + widthOfBar,
                    layout_width - paddingRight - widthOfBar, layout_height - paddingBottom - widthOfBar);
        }
    }

    /**
     * Parse the attributes passed to the view from the XML
     *
     * @param a the attributes to parse
     */
    private void parseAttributes(TypedArray a) {
        // We transform the default values from DIP to pixels
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        widthOfBar = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthOfBar, metrics);
        widthOfRim = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthOfRim, metrics);
        radiusOfCircle =
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radiusOfCircle, metrics);

        radiusOfCircle =
                (int) a.getDimension(R.styleable.WheelProgress_radiusOfCircle, radiusOfCircle);

        radiusOfFill = a.getBoolean(R.styleable.WheelProgress_radiusOfFill, false);

        widthOfBar = (int) a.getDimension(R.styleable.WheelProgress_widthOfBar, widthOfBar);

        widthOfRim = (int) a.getDimension(R.styleable.WheelProgress_widthOfRim, widthOfRim);

        float basespeedOfSpin =
                a.getFloat(R.styleable.WheelProgress_speedOfSpin, speedOfSpin / 360.0f);
        speedOfSpin = basespeedOfSpin * 360;

        timeOfSpinBarCycle =
                a.getInt(R.styleable.WheelProgress_timeOfSpinBarCycle, (int) timeOfSpinBarCycle);

        colorOfBar = a.getColor(R.styleable.WheelProgress_colorOfBar, colorOfBar);

        colorOfRim = a.getColor(R.styleable.WheelProgress_colorOfRim, colorOfRim);

        ProgressOfLinear = a.getBoolean(R.styleable.WheelProgress_progressOfLinear, false);

        if (a.getBoolean(R.styleable.WheelProgress_progressIndeterminate, false)) {
            spin();
        }

        // Recycle
        a.recycle();
    }

    public void setCallback(ProgressCallback progressCallback) {
        callback = progressCallback;

        if (!isSpinning) {
            runCallback();
        }
    }

    //----------------------------------
    //Animation stuff
    //----------------------------------

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawArc(boundsOfCircle, 360, 360, false, paintOfRim);

        boolean mustInvalidate = false;

        if (!shouldAnimate) {
            return;
        }

        if (isSpinning) {
            //Draw the spinning bar
            mustInvalidate = true;

            long deltaTime = (SystemClock.uptimeMillis() - timeOfLastAnimation);
            float deltaNormalized = deltaTime * speedOfSpin / 1000.0f;

            updateLengthOfBar(deltaTime);

            mProgress += deltaNormalized;
            if (mProgress > 360) {
                mProgress -= 360f;

                // A full turn has been completed
                // we run the callback with -1 in case we want to
                // do something, like changing the color
                runCallback(-1.0f);
            }
            timeOfLastAnimation = SystemClock.uptimeMillis();

            float from = mProgress - 90;
            float length = lengthOfBar + extraLengthOfBar;

            if (isInEditMode()) {
                from = 0;
                length = 135;
            }

            canvas.drawArc(boundsOfCircle, from, length, false, paintOfBar);
        } else {
            float oldProgress = mProgress;

            if (mProgress != mTargetProgress) {
                //We smoothly increase the progress bar
                mustInvalidate = true;

                float deltaTime = (float) (SystemClock.uptimeMillis() - timeOfLastAnimation) / 1000;
                float deltaNormalized = deltaTime * speedOfSpin;

                mProgress = Math.min(mProgress + deltaNormalized, mTargetProgress);
                timeOfLastAnimation = SystemClock.uptimeMillis();
            }

            if (oldProgress != mProgress) {
                runCallback();
            }

            float offset = 0.0f;
            float progress = mProgress;
            if (!ProgressOfLinear) {
                float factor = 2.0f;
                offset = (float) (1.0f - Math.pow(1.0f - mProgress / 360.0f, 2.0f * factor)) * 360.0f;
                progress = (float) (1.0f - Math.pow(1.0f - mProgress / 360.0f, factor)) * 360.0f;
            }

            if (isInEditMode()) {
                progress = 360;
            }

            canvas.drawArc(boundsOfCircle, offset - 90, progress, false, paintOfBar);
        }

        if (mustInvalidate) {
            invalidate();
        }
    }

    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility == VISIBLE) {
            timeOfLastAnimation = SystemClock.uptimeMillis();
        }
    }

    private void updateLengthOfBar(long deltaTimeInMilliSeconds) {
        if (timeWithoutOfPauseGrowing >= growingTimeOfPause) {
            startTimeOfGrowing += deltaTimeInMilliSeconds;

            if (startTimeOfGrowing > timeOfSpinBarCycle) {
                // We completed a size change cycle
                // (growing or shrinking)
                startTimeOfGrowing -= timeOfSpinBarCycle;
                //if(fromFrontGrowingBar) {
                timeWithoutOfPauseGrowing = 0;
                //}
                fromFrontGrowingBar = !fromFrontGrowingBar;
            }

            float distance =
                    (float) Math.cos((startTimeOfGrowing / timeOfSpinBarCycle + 1) * Math.PI) / 2 + 0.5f;
            float destLength = (maxLengthOfBar - lengthOfBar);

            if (fromFrontGrowingBar) {
                extraLengthOfBar = distance * destLength;
            } else {
                float newLength = destLength * (1 - distance);
                mProgress += (extraLengthOfBar - newLength);
                extraLengthOfBar = newLength;
            }
        } else {
            timeWithoutOfPauseGrowing += deltaTimeInMilliSeconds;
        }
    }

    /**
     * Check if the wheel is currently spinning
     */

    public boolean isSpinning() {
        return isSpinning;
    }

    /**
     * Reset the count (in increment mode)
     */
    public void resetCount() {
        mProgress = 0.0f;
        mTargetProgress = 0.0f;
        invalidate();
    }

    /**
     * Turn off spin mode
     */
    public void stopSpinning() {
        isSpinning = false;
        mProgress = 0.0f;
        mTargetProgress = 0.0f;
        invalidate();
    }

    /**
     * Puts the view on spin mode
     */
    public void spin() {
        timeOfLastAnimation = SystemClock.uptimeMillis();
        isSpinning = true;
        invalidate();
    }

    private void runCallback(float value) {
        if (callback != null) {
            callback.onProgressUpdate(value);
        }
    }

    private void runCallback() {
        if (callback != null) {
            float normalizedProgress = (float) Math.round(mProgress * 100 / 360.0f) / 100;
            callback.onProgressUpdate(normalizedProgress);
        }
    }

    /**
     * Set the progress to a specific value,
     * the bar will be set instantly to that value
     *
     * @param progress the progress between 0 and 1
     */
    public void setInstantProgress(float progress) {
        if (isSpinning) {
            mProgress = 0.0f;
            isSpinning = false;
        }

        if (progress > 1.0f) {
            progress -= 1.0f;
        } else if (progress < 0) {
            progress = 0;
        }

        if (progress == mTargetProgress) {
            return;
        }

        mTargetProgress = Math.min(progress * 360.0f, 360.0f);
        mProgress = mTargetProgress;
        timeOfLastAnimation = SystemClock.uptimeMillis();
        invalidate();
    }

    // Great way to save a view's state http://stackoverflow.com/a/7089687/1991053
    @Override public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        WheelSavedState ss = new WheelSavedState(superState);

        // We save everything that can be changed at runtime
        ss.mProgress = this.mProgress;
        ss.mTargetProgress = this.mTargetProgress;
        ss.isSpinning = this.isSpinning;
        ss.speedOfSpin = this.speedOfSpin;
        ss.widthOfBar = this.widthOfBar;
        ss.colorOfBar = this.colorOfBar;
        ss.widthOfRim = this.widthOfRim;
        ss.colorOfRim = this.colorOfRim;
        ss.radiusOfCircle = this.radiusOfCircle;
        ss.ProgressOfLinear = this.ProgressOfLinear;
        ss.radiusOfFill = this.radiusOfFill;

        return ss;
    }

    @Override public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof WheelSavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        WheelSavedState ss = (WheelSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        this.mProgress = ss.mProgress;
        this.mTargetProgress = ss.mTargetProgress;
        this.isSpinning = ss.isSpinning;
        this.speedOfSpin = ss.speedOfSpin;
        this.widthOfBar = ss.widthOfBar;
        this.colorOfBar = ss.colorOfBar;
        this.widthOfRim = ss.widthOfRim;
        this.colorOfRim = ss.colorOfRim;
        this.radiusOfCircle = ss.radiusOfCircle;
        this.ProgressOfLinear = ss.ProgressOfLinear;
        this.radiusOfFill = ss.radiusOfFill;

        this.timeOfLastAnimation = SystemClock.uptimeMillis();
    }

    /**
     * @return the current progress between 0.0 and 1.0,
     * if the wheel is indeterminate, then the result is -1
     */
    public float getProgress() {
        return isSpinning ? -1 : mProgress / 360.0f;
    }

    //----------------------------------
    //Getters + setters
    //----------------------------------

    /**
     * Set the progress to a specific value,
     * the bar will smoothly animate until that value
     *
     * @param progress the progress between 0 and 1
     */
    public void setProgress(float progress) {
        if (isSpinning) {
            mProgress = 0.0f;
            isSpinning = false;

            runCallback();
        }

        if (progress > 1.0f) {
            progress -= 1.0f;
        } else if (progress < 0) {
            progress = 0;
        }

        if (progress == mTargetProgress) {
            return;
        }

        // If we are currently in the right position
        // we set again the last time animated so the
        // animation starts smooth from here
        if (mProgress == mTargetProgress) {
            timeOfLastAnimation = SystemClock.uptimeMillis();
        }

        mTargetProgress = Math.min(progress * 360.0f, 360.0f);

        invalidate();
    }

    /**
     * Sets the determinate progress mode
     *
     * @param isLinear if the progress should increase linearly
     */
    public void setProgressOfLinear(boolean isLinear) {
        ProgressOfLinear = isLinear;
        if (!isSpinning) {
            invalidate();
        }
    }

    /**
     * @return the radius of the wheel in pixels
     */
    public int getRadiusOfCircle() {
        return radiusOfCircle;
    }

    /**
     * Sets the radius of the wheel
     *
     * @param radiusOfCircle the expected radius, in pixels
     */
    public void setCircleRadius(int radiusOfCircle) {
        this.radiusOfCircle = radiusOfCircle;
        if (!isSpinning) {
            invalidate();
        }
    }

    /**
     * @return the width of the spinning bar
     */
    public int getWidthOfBar() {
        return widthOfBar;
    }

    /**
     * Sets the width of the spinning bar
     *
     * @param barWidth the spinning bar width in pixels
     */
    public void setBarWidth(int barWidth) {
        this.widthOfBar = barWidth;
        if (!isSpinning) {
            invalidate();
        }
    }

    /**
     * @return the color of the spinning bar
     */
    public int getColorOfBar() {
        return colorOfBar;
    }

    /**
     * Sets the color of the spinning bar
     *
     * @param colorOfBar The spinning bar color
     */
    public void setColorOfBar(int colorOfBar) {
        this.colorOfBar = colorOfBar;
        setupPaints();
        if (!isSpinning) {
            invalidate();
        }
    }

    /**
     * @return the color of the wheel's contour
     */
    public int getColorOfRim() {
        return colorOfRim;
    }

    /**
     * Sets the color of the wheel's contour
     *
     * @param colorOfRim the color for the wheel
     */
    public void setColorOfRim(int colorOfRim) {
        this.colorOfRim = colorOfRim;
        setupPaints();
        if (!isSpinning) {
            invalidate();
        }
    }

    /**
     * @return the base spinning speed, in full circle turns per second
     * (1.0 equals on full turn in one second), this value also is applied for
     * the smoothness when setting a progress
     */
    public float getSpeedOfSpin() {
        return speedOfSpin / 360.0f;
    }

    /**
     * Sets the base spinning speed, in full circle turns per second
     * (1.0 equals on full turn in one second), this value also is applied for
     * the smoothness when setting a progress
     *
     * @param speedOfSpin the desired base speed in full turns per second
     */
    public void setSpeedOfSpin(float speedOfSpin) {
        this.speedOfSpin = speedOfSpin * 360.0f;
    }

    /**
     * @return the width of the wheel's contour in pixels
     */
    public int getWidthOfRim() {
        return widthOfRim;
    }

    /**
     * Sets the width of the wheel's contour
     *
     * @param rimWidth the width in pixels
     */
    public void setRimWidth(int rimWidth) {
        this.widthOfRim = rimWidth;
        if (!isSpinning) {
            invalidate();
        }
    }

    public interface ProgressCallback {
        /**
         * Method to call when the progress reaches a value
         * in order to avoid float precision issues, the progress
         * is rounded to a float with two decimals.
         *
         * In indeterminate mode, the callback is called each time
         * the wheel completes an animation cycle, with, the progress value is -1.0f
         *
         * @param progress a double value between 0.00 and 1.00 both included
         */
        public void onProgressUpdate(float progress);
    }

    static class WheelSavedState extends BaseSavedState {
        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<WheelSavedState> CREATOR =
                new Parcelable.Creator<WheelSavedState>() {
                    public WheelSavedState createFromParcel(Parcel in) {
                        return new WheelSavedState(in);
                    }

                    public WheelSavedState[] newArray(int size) {
                        return new WheelSavedState[size];
                    }
                };
        float mProgress;
        float mTargetProgress;
        boolean isSpinning;
        float speedOfSpin;
        int widthOfBar;
        int colorOfBar;
        int widthOfRim;
        int colorOfRim;
        int radiusOfCircle;
        boolean ProgressOfLinear;
        boolean radiusOfFill;

        WheelSavedState(Parcelable superState) {
            super(superState);
        }

        private WheelSavedState(Parcel in) {
            super(in);
            this.mProgress = in.readFloat();
            this.mTargetProgress = in.readFloat();
            this.isSpinning = in.readByte() != 0;
            this.speedOfSpin = in.readFloat();
            this.widthOfBar = in.readInt();
            this.colorOfBar = in.readInt();
            this.widthOfRim = in.readInt();
            this.colorOfRim = in.readInt();
            this.radiusOfCircle = in.readInt();
            this.ProgressOfLinear = in.readByte() != 0;
            this.radiusOfFill = in.readByte() != 0;
        }

        @Override public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(this.mProgress);
            out.writeFloat(this.mTargetProgress);
            out.writeByte((byte) (isSpinning ? 1 : 0));
            out.writeFloat(this.speedOfSpin);
            out.writeInt(this.widthOfBar);
            out.writeInt(this.colorOfBar);
            out.writeInt(this.widthOfRim);
            out.writeInt(this.colorOfRim);
            out.writeInt(this.radiusOfCircle);
            out.writeByte((byte) (ProgressOfLinear ? 1 : 0));
            out.writeByte((byte) (radiusOfFill ? 1 : 0));
        }
    }
}
