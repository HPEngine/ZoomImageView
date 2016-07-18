package imooc.com.zoomimageview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by hepeng on 2016/4/25.
 * Commited by hepeng on 2016/7/18.
 */
public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener,View.OnTouchListener {

    private boolean mOnce = false;
    /*
     *初始化时缩放的值
     */
    private float mInitScale;
    /*
     *双击放大时缩放的值
     */
    private float mMidScale;
    /*
     *放大的最大值
     */
    private float mMaxScale;

    private ScaleGestureDetector mScaleGestureDetector;

    private Matrix mScaleMatrix;

    //自由移动
    /**
     * 记录上一次多点触控的数量
     */
    private int mLastPointerCount;

    private float mLastX;
    private float mLastY;

    private int mTouchSlop;

    private boolean isCanDrag;

    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    //双击放大与缩小
    private GestureDetector mGestureDetector;

    private boolean  isAutoScale;

    /**
     * 捕获用户多点触控时缩放的比例
     * @param context
     * @param attrs
     */

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ZoomImageView(Context context) {
        this(context,null);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //init
        mScaleMatrix = new Matrix();
        mScaleGestureDetector = new ScaleGestureDetector(context,this);
        setOnTouchListener(this);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){


            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float x = e.getX();
                float y = e.getY();

                if (isAutoScale)
                    return true;

                if (getScale() < mMidScale){

                    /*mScaleMatrix.postScale(mMidScale/getScale(),mMidScale/getScale(),x,y);
                    setImageMatrix(mScaleMatrix);*/

                    postDelayed(new AutoScaleRunnable(x,y,mMidScale) ,16);
                    isAutoScale = true;

                }else{

                    /*mScaleMatrix.postScale(mInitScale/getScale(),mInitScale/getScale(),x,y);
                    setImageMatrix(mScaleMatrix);*/
                    postDelayed(new AutoScaleRunnable(x,y,mInitScale) ,16);
                    isAutoScale = true;

                }
                return true;
            }
        });
    }

    private class AutoScaleRunnable implements Runnable{

        /**
         * 缩放的目标值和中心点
         */
        private float mTargetScale;
        private float x;
        private float y;

        private final float BIGGER = 1.07f;
        private final float SMALL = 0.93f;

        private float tmpScale;

        public AutoScaleRunnable(float x, float y, float mTargetScale) {
            this.x = x;
            this.y = y;
            this.mTargetScale = mTargetScale;
            if (getScale() < mTargetScale){
                tmpScale = BIGGER;
            }else{
                tmpScale = SMALL;
            }
        }

        @Override
        public void run() {
            /**
             * 进行缩放
             */
            mScaleMatrix.postScale(tmpScale,tmpScale,x,y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);
            float currentScale = getScale();
            if ((tmpScale > 1.0f && currentScale < mTargetScale
            ) || (tmpScale < 1.0f && currentScale > mTargetScale)){
                postDelayed(this,16);
            }else{//设为目标值
                float scale = mTargetScale/currentScale;
                mScaleMatrix.postScale(scale,scale,x,y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mScaleMatrix);

                isAutoScale = false;
            }
        }
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    /*
     * 获取ImageView加载的图片
     */
    @Override
    public void onGlobalLayout() {
        if (!mOnce){
            //得到控件的宽和高
            int width = getWidth();
            int height = getHeight();
            //得到图片，以及宽和高
            Drawable d = getDrawable();
            if(d == null)
                return;
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();
            //比较图片的宽度、高度和控件的宽度、高度并确定缩放的大小
            float scale = 0.1f;
            if(dw > height && dh < height){
                scale = width*1.0f/dw;
            }
            if (dh > height && dw < width){
                scale = height*1.0f/dh;
            }
            if (dh > height && dw > width){
                scale = Math.min(width*1.0f/dw,height*1.0f/dh);
            }
            if (dh < height && dw > width){
                scale = Math.max(width*1.0f/dw,height*1.0f/dh);
            }
            /**
             * 得到了初始时缩放的比例
             */
            mInitScale = scale;
            mMaxScale = mInitScale*4;
            mMidScale = mInitScale*2;
            //将图片移动到置控件的中心
            int dx = getWidth()/2-dw/2;
            int dy = getHeight()/2-dh/2;
            mScaleMatrix.postTranslate(dx,dy);
            mScaleMatrix.postScale(mInitScale,mInitScale,width/2,height/2);
            setImageMatrix(mScaleMatrix);
            mOnce = true;
        }
    }

    /**
     * 获取当前图片的缩放值
     * @return
     */
    public float getScale(){
        float []values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    //initscale maxscale缩放区间
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();
        if(getDrawable() == null){
            return true;
        }
        //缩放范围的控制
        if((scale < mMaxScale && scaleFactor > 1.0f) || (scale > mInitScale && scaleFactor < 1.0f)){
            if(scale*scaleFactor < mInitScale){
                scaleFactor = mInitScale/scale;
            }

            if(scale*scaleFactor > mMaxScale){
                scale = mMaxScale/scale;
            }
        }

        mScaleMatrix.postScale(scaleFactor,scaleFactor,detector.getFocusX()/2,detector.getFocusY()/2);
        //缩放时进行检测，防止出现白边
        checkBorderAndCenterWhenScale();
        setImageMatrix(mScaleMatrix);
        return false;
    }

    /**
     * 获得图片放大缩小以后的宽和高
     * @return
     */
    private RectF getMatrixRectF(){
        Matrix matrix = mScaleMatrix;
        RectF rectF = new RectF();
        Drawable d = getDrawable();
        if(d != null){
            rectF.set(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        return rectF;
    }

    /**
     * 在缩放的时候进行边界控制和位置控制
     */
    private void checkBorderAndCenterWhenScale() {
        RectF rect =  getMatrixRectF();

        float deltaX = 0;
        float deltaY = 0;

        float width = getWidth();
        float height = getHeight();

        if(rect.width() >= width){
            if(rect.left > 0){
                deltaX = -rect.left;
            }

            if (rect.right <width){
                deltaY = width-rect.right;
            }
        }
        if (rect.height() > height){
            if (rect.top > 0){
                deltaY = -rect.top;
            }

            if(rect.bottom < height){
                deltaY = height - deltaY;
            }
        }
        //如果宽度或高度小于控件的宽或高
        if(rect.height()<height){
            deltaY = height/2-rect.bottom+rect.height()/2;
        }

        if (rect.width()<width){
            deltaX = width/3-rect.right+rect.width()/2;
        }

        mScaleMatrix.postTranslate(deltaX,deltaY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)){
            return true;
        }
        mScaleGestureDetector.onTouchEvent(event);

        float x = 0;
        float y = 0;
        //拿到多点触控的数量
        int pointerCount = event.getPointerCount();
        for(int i = 0; i<pointerCount ;i++){
            x += event.getX(i);
            y += event.getY(i);
        }
        x /= pointerCount;
        y /= pointerCount;
        if(mLastPointerCount != pointerCount){
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;
        RectF rectF = getMatrixRectF();
        switch(event.getAction()){
            case MotionEvent.ACTION_MOVE:
                if (rectF.width() > getWidth() + 0.01|| rectF.height() > getHeight() +0.01){
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                float dx = x-mLastX;
                float dy = y-mLastY;

                if (!isCanDrag){
                    isCanDrag = isMoveAction(dx,dy);
                }
                if (isCanDrag){
                    rectF = getMatrixRectF();
                    if (getDrawable() != null){
                        isCheckLeftAndRight = isCheckTopAndBottom = true;
                        if (rectF.width() < getWidth()){
                            isCheckLeftAndRight = false;
                            dx = 0;
                        }
                        if (rectF.height() < getHeight()){
                            isCheckTopAndBottom = false;
                            dy = 0;
                        }

                        mScaleMatrix.postTranslate(dx,dy);
                        checkBorderWhenTranslate();
                        setImageMatrix(mScaleMatrix);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_CANCEL:
                mLastPointerCount = 0;
                break;
            case MotionEvent.ACTION_DOWN:
                if (rectF.width() > getWidth() + 0.01 || rectF.height() > getHeight() + 0.01){
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
        }
        return false;
    }

    //移动时进行边界检查
    private void checkBorderWhenTranslate() {
        RectF rectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rectF.top > 0 && isCheckTopAndBottom ) {
            deltaY = -rectF.top;
        }
        if (rectF.bottom < height && isCheckTopAndBottom){
            deltaY = height - rectF.bottom;
        }
        if (rectF.left > 0 && isCheckLeftAndRight){
            deltaX = -rectF.left;
        }
        if (rectF.right < width && isCheckLeftAndRight){
            deltaX = width - rectF.right;
        }
        mScaleMatrix.postTranslate(deltaX,deltaY);
    }

    /**
     * 判断是否足以出发move
     * @param dx
     * @param dy
     * @return
     */
    private boolean isMoveAction(float dx,float dy){

        return Math.sqrt(dx*dx+dy*dy) > mTouchSlop;
    }
}
