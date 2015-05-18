package com.cafe.view;

import android.R.integer;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

public class ZoomImageView extends ImageView implements OnGlobalLayoutListener,
OnScaleGestureListener ,OnTouchListener{

	private boolean once;
	private float mInitScale;//初始化时缩放的值
	private float mMidScale;//双击放大的值
	private float mMaxScale;//最大scale
	
	//移动缩放
	private Matrix mScaleMatrix;
	//捕获用户多点触控时缩放比例
	private ScaleGestureDetector mScaleGestureDetector;
	//......自由移动
	/**
	 * 记录上一次多点触控的数量
	 */
	private int mLastPointCount;

	private float mLastX,mLastY;

	private int mTouvhSlop;
	private boolean isCanDrag;

	private boolean isCheckLeftAndRight;
	private boolean isCheckTopAndButtom;
	public ZoomImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mScaleMatrix = new Matrix();
		super.setScaleType(ScaleType.MATRIX);
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		setOnTouchListener(this);
		mTouvhSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

	public ZoomImageView(Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}

	public ZoomImageView(Context context) {
		this(context,null);
	}


	@Override
	protected void onAttachedToWindow() {//当view Attach到window执行
		super.onAttachedToWindow();
		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}
	@SuppressWarnings("deprecation")
	@Override
	protected void onDetachedFromWindow() {//当view Detached执行
		super.onDetachedFromWindow();
		getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}
	@Override
	public void onGlobalLayout() {//全局布局方法调用完成后，会调用该方法
		//此接口需要在某地方注册和移除，onAttachedToWindow，onDetachedFromWindow

		if(!once){
			float w = getWidth()*1.0f;//控件宽高
			float h = getHeight()*1.0f;

			Drawable d = getDrawable();
			if(d==null)return;
			float dw = d.getIntrinsicWidth()*1.0f;//图片宽高
			float dh = d.getIntrinsicHeight()*1.0f;

			float scale = 1.0f;
			if(dw>w&&dh<h){
				scale=w*1.0f/dw;
			}else if(dh>h&&dw<w){
				scale=h*1.0f/dh;
			}else{
				scale=Math.min(w*1.0f/dw,h*1.0f/dh);
			}
			mInitScale = scale;
			mMaxScale = mInitScale*4;
			mMidScale = mInitScale*2;

			//将图片移动到当前控件的中心

			mScaleMatrix.postScale(mInitScale, mInitScale);
			checkBorderAndCneterWhenTranslate();
			setImageMatrix(mScaleMatrix);
			once = true;
		}
	}


	//获取当前图片的缩放值
	public float getScale(){
		float[] values = new float[9];
		mScaleMatrix.getValues(values);
		return values[Matrix.MSCALE_X];
	}

	//缩放区间mMaxScale。mMidScale
	@Override
	public boolean onScale(ScaleGestureDetector detector) {

		float scale = getScale();
		float scaleFactor = detector.getScaleFactor();

		if(getDrawable()==null){
			return true;
		}
		//缩放的范围

		if((scale <mMaxScale&&scaleFactor>1.0)
				||(scale>mInitScale&&scaleFactor<1.0)){

			if(scale*scaleFactor<mInitScale){
				scaleFactor = mInitScale/scale;
			}
			if(scale*scaleFactor>mMaxScale){
				scaleFactor = mMaxScale/scale;
			}

			mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
			checkBorderAndCneterWhenTranslate();
			setImageMatrix(mScaleMatrix);
		}
		return true;
	}

	/**
	 * 获得图片放大缩小以后的宽高以及各点坐标
	 * @return
	 */
	private RectF getmatrixRectF(){
		Matrix matrix = mScaleMatrix;
		RectF f = new RectF();
		Drawable d = getDrawable();

		if(d!=null){
			f.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
			matrix.mapRect(f);
		}
		return f;
	}
	/**
	 * 在缩放时进行边界控制和位置控制
	 */

	private void checkBorderAndCneterWhenTranslate() {
		RectF rect = getmatrixRectF();

		float deltaX = 0;
		float deltaY = 0;

		int width  = getWidth();
		int height = getHeight();

		if(rect.top>0&&isCheckTopAndButtom)deltaY = -rect.top;
		if(rect.bottom<height&&isCheckTopAndButtom)deltaY = height-rect.bottom;
		if(rect.left>0&&isCheckLeftAndRight)	deltaX = -rect.left;
		if(rect.right<width&&isCheckLeftAndRight)deltaX = width-rect.right;


		//如果宽度高度小于控件的宽高，居中
		if(rect.width()<width){
			deltaX = width/2-rect.right+rect.width()/2;
		}
		if(rect.height()<height){
			deltaY = height/2-rect.bottom+rect.height()/2;
		}
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {

		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {

	}

	@SuppressLint("ClickableViewAccessibility") @Override
	public boolean onTouch(View v, MotionEvent event) {
		mScaleGestureDetector.onTouchEvent(event);

		float x = 0;
		float y = 0;

		int pointerCount = event.getPointerCount();
		for (int i = 0; i < pointerCount; i++) {
			x+=event.getX(i);
			y+=event.getY(i);
		}
		x/=pointerCount;
		y/=pointerCount;

		if(mLastPointCount!=pointerCount){
			isCanDrag = false;
			mLastX = x;
			mLastY = y;
		}
		mLastPointCount = pointerCount;

		switch (event.getAction()) {
		case MotionEvent.ACTION_MOVE:

			float dx = x-mLastX;
			float dy = y-mLastY;

			if(!isCanDrag){
				isCanDrag = isMoveAction(dx,dy);
			}
			if(isCanDrag){
				isCheckLeftAndRight = isCheckTopAndButtom = true;
				RectF rectF = getmatrixRectF();
				if(getDrawable()!=null){//如果宽度小于控件宽度，不允许横向移动
					if(rectF.width()<=getWidth()){
						isCheckLeftAndRight = false;
						dx=0;
					}
					if(rectF.height()<=getHeight()){
						isCheckTopAndButtom = false;
						dy=0;
					}

					mScaleMatrix.postTranslate(dx, dy);
					checkBorderAndCneterWhenTranslate();
					setImageMatrix(mScaleMatrix);
				}
			}

			mLastX = x;
			mLastY = y;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mLastPointCount = 0;
			break;

		default:
			break;
		}

		return true;
	}



	private boolean isMoveAction(float dx, float dy) {
		return Math.sqrt(dx*dx+dy*dy)>mTouvhSlop;
	}
}
