package es.uma.lcc.lockpic;

import java.util.ArrayList;
import es.uma.lcc.lockpic.SelectorActivity.ViewMode;
import es.uma.lcc.nativejpegencoder.R;
import es.uma.lcc.utils.Rectangle;
import es.uma.lcc.utils.LockPicIO;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Paint.Style;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * View (meant for use in SelectorActivity) which provides the interface for it
 */

public class DrawView extends View implements OnTouchListener {

	// Constants
	final int DEFAULT_RECTANGLE_SIZE = 200; // new squares will be this about this size.
    final int DEFAULT_SIZE_CTRL_WIDTH = 80; // bottom right size control dimensions
    final int REASONABLE_ACCIDENTAL_MOVING_LIMIT = 30;
    	/* Sometimes, touching and releasing is interpreted as a move action (see onTouch).
    	   Unless the movement is greater than this threshold, we interpret it as release.  */
	final int MAX_NUMBER_OF_FACES = 5;  // limit for automatic face detection
	final int MAX_REGIONS_ALLOWED = 15; // no more than this number of squares
	final int ROUNDING_FACTOR = 1; // jpeg MCU is 16x16px, we want to work with multiples
	// Set to 1 for smooth look, since anyway it isn't WYSIWYG set to 16
		
	ViewMode mMode; 
	/* This view works in 2 modes: selecting rectangles and granting permissions on them.
	 * This variable can be checked to know which mode the view is currently on.
	 * Class ViewMode is declared in SelectorActivity  */
	
	// Class variables
    ArrayList<Rectangle> mRects = new ArrayList<Rectangle>(); // regions marked on the picture
    Bitmap mBmp; // the image which is being displayed

	boolean mFirstTime;
	/* Set to true if the activity is created by the first time. Certain operations, like
	 * face detection, need not be done on subsequent creations (ie on screen orientation change) */
    
    // Auxiliary class variables
    Paint mPaintBorder = new Paint(); // border for selected regions
    Paint mPaintFill = new Paint(); 
    // paint for selected regions with permissions added (used only in PERMISSION_SELECTOR mode)
    Paint mSizeCtrlPaint = new Paint(); // paint for the bottom right size control
    int mSampleSize = 1; // sampling factor - currently always 1
	int mOriginalWidth, mOriginalHeight; // original dimensions of the picture
    int mMaxWidth, mMaxHeight; // dimensions of the picture after resizing to fit on screen
    float mAspectRateFactor = 1; /* factor by which height and width will be multiplied to fit
    								onscreen keeping the aspect ratio */
    int mStartingUpCorner = -1, mStartingLeftCorner = -1; // initial coordinates in a movement
    boolean mWantsToDelete = false; // set to true if user may want to remove a rectangle
    int mCurrentSelectedResizingRect = -1; // if a rectangle is being resized, sets to its index
    int mCurrentSelectedMovingRect = -1; // if a rectangle is being moved, sets to its index
    int mHorizontalPadding = 0, mVerticalPadding = 0;
     /* Padding, respectively needed if the picture is less wide or less high than the screen.
      * Used to determine the borders of the legal selection area.  */
    
	
	/**
	 * Constructor called from MainActivity. Initialises some variables
	 * @param context: Parent context, mandatory for activities
	 * @param path: path of the image file
	 */
    public DrawView(Context context, String path, boolean firstTime)  {
    	super(context);
    	mMode = ViewMode.REGION_SELECTOR;
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inSampleSize = mSampleSize;
    	mBmp = BitmapFactory.decodeFile(path, options);
    	mOriginalWidth = options.outWidth;
		mOriginalHeight = options.outHeight;
		mFirstTime = firstTime;
    }
    
    /**
     * Called by the Android OS when the view has been set, and before it is shown.
     * This is the moment to perform all actions which require knowledge of display
     * dimensions, which are unavailable before.
     */
	@SuppressLint("DrawAllocation")
	@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)  {
        super.onLayout(changed, left, top, right, bottom);
        int h = getHeight();
        int w = getWidth();
        
        float oldAspectRate = mAspectRateFactor;
        mAspectRateFactor = LockPicIO.getBestFitKeepingAspectRatio(
				w, h, mOriginalWidth, mOriginalHeight);
		 mBmp = Bitmap.createScaledBitmap(mBmp,
				 	(int)(mOriginalWidth*mAspectRateFactor),
					(int)(mOriginalHeight*mAspectRateFactor), false);
		 
		 int heightSpare = h - (int)(mOriginalHeight*mAspectRateFactor);
		 int widthSpare = w - (int)(mOriginalWidth*mAspectRateFactor);
		 /* Excess dimensions of the screen respect to the resized image.
		  * At least one of them will be close to zero. */
		 if(heightSpare < widthSpare)  {
			 mHorizontalPadding = widthSpare/2;
			 mVerticalPadding = 0;
		 }  else  {
			 mHorizontalPadding = 0;
			 mVerticalPadding = heightSpare/2;
		 }
		 
		 if(mFirstTime)  { // face detection is only performed on first load
//			 getFaceCoord(mPath);
			 mFirstTime = false;
		 }  else  { // we probably come from an orientation change: we adapt the coordinates
			 for(Rectangle rect : mRects)
				 rect.resizeBy(mAspectRateFactor/oldAspectRate);
		 }
		 
		 initDrawingSurface();
    }
    
    /** additional constructors, needed by the Android OS **/
    public DrawView(Context context) {
        super(context);
        initDrawingSurface();
    }
    
    public DrawView(Context context, AttributeSet attrs) {
	  super(context, attrs);
	  initDrawingSurface();
	}
	public DrawView(Context context, AttributeSet attrs, int defStyle) {
	  super(context, attrs, defStyle);
	  initDrawingSurface();
	}
	
	private void initDrawingSurface()
	{
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.setOnTouchListener(this);
        setBackgroundColor(Color.BLACK);
        
        mPaintBorder.setStyle(Style.STROKE);
		mPaintBorder.setStrokeWidth(3);
		
		mPaintFill.setColor(Color.GREEN);
		mPaintFill.setAlpha(128);
		mPaintFill.setStyle(Paint.Style.FILL_AND_STROKE);
		
		mSizeCtrlPaint.setColor(Color.GRAY);
		mSizeCtrlPaint.setStyle(Style.FILL);
		mSizeCtrlPaint.setAlpha(128);
		
		// we could re-use the ones calculated in onLayout, but these are not
		// expensive operations, and we save potential problems if the scaled
		// bitmap falls a few pixels short of the screen width/height due to
		// rounding
		// Also, when the image size is not divisible by 16, problems may arise 
		// when using jpeg encryption. This line uses rounding to keep away from the 
		// potentially problematic borders, avoiding those obstacles.
		mMaxWidth = (mBmp.getWidth()/16)*16; 
		mMaxHeight = (mBmp.getHeight()/16)*16;  
	}
	
	/**
	 * Called every time the view is being drawn onscreen.
	 */
    @Override
    public void onDraw(Canvas canvas) {
    	canvas.drawBitmap(mBmp, mHorizontalPadding, mVerticalPadding, null);
    	if(mMode == ViewMode.REGION_SELECTOR)  {
    		mPaintBorder.setColor(Color.GREEN);
	        for (Rectangle rect: mRects) {
	        	// main rectangle
	            canvas.drawRect(mHorizontalPadding + rect.x0, 
            				mVerticalPadding + rect.y0, 
            				mHorizontalPadding + rect.xEnd, 
            				mVerticalPadding + rect.yEnd, mPaintBorder);
	            // size control
	        	canvas.drawRect(mHorizontalPadding + rect.xEnd - DEFAULT_SIZE_CTRL_WIDTH, 
	        				mVerticalPadding + rect.yEnd - DEFAULT_SIZE_CTRL_WIDTH, 
	        				mHorizontalPadding + rect.xEnd, 
	        				mVerticalPadding + rect.yEnd, mSizeCtrlPaint);
	        }
    	}  else  {
    		mPaintBorder.setColor(Color.RED);
    		for(Rectangle rect : mRects) {
    			if(rect.hasPermissions())
    				canvas.drawRect(mHorizontalPadding + rect.x0, 
            				mVerticalPadding + rect.y0, 
            				mHorizontalPadding + rect.xEnd, 
            				mVerticalPadding + rect.yEnd, mPaintFill);
    			else
    				canvas.drawRect(mHorizontalPadding + rect.x0, 
            				mVerticalPadding + rect.y0, 
            				mHorizontalPadding + rect.xEnd, 
            				mVerticalPadding + rect.yEnd, mPaintBorder);
    		}
    	}
        
    }
    
    /**
     * This function rounds an integer to the closest, lower multiple 
     * of ROUNDING_FACTOR. E.g., if the factor is 16,
     * factorRound(16) = factorRound(17) = ... = factorRound(31) = 16,
     * but factorRound(32) = ... = factorRound(47) = 32.
     * 
     * It is meant to be used on every coordinate calculated, so as to
     * ensure the blocks selected by the user are precisely the areas
     * encoded, without an uncertain margin. The encoding is being done
     * on 16x16 pixel squares - selecting a 17x16 would imply encrypting
     * 2 small squares, that is, a 32x16 region, almost twice the expected
     * size.
     * 
     * Its use in onTouch ensures what you see is what you get, but at
     * the price of looking a bit "choppy". Setting ROUNDING_FACTOR to
     * 1 would get rid of the choppy look, but may result in areas larger
     * than expected being encrypted, noticeable in small images.
     */
    private int factorRound(int value)  {
    	return (value/ROUNDING_FACTOR)*ROUNDING_FACTOR;
    }

    /**
     * Called every time the screen registers a touch event. Those relevant to this
     * activity are ACTION_DOWN (touch screen), ACTION_UP (lift finger from screen),
     * and ACTION_MOVE (drag finger).
     */
    public boolean onTouch(View view, MotionEvent event) {

	   	 int x = (int)event.getX() - mHorizontalPadding;
	   	 int y = (int)event.getY() - mVerticalPadding;
	   	 
	   	 if(mMode == ViewMode.REGION_SELECTOR)  {
	         if(event.getAction() == MotionEvent.ACTION_DOWN)  {
	        	 int index = isInAnyRectangle(x,y);
	        	 if(index == -1) //not in any rectangle: try to place a new one
	        	 {
	        		 if(mRects.size() >= MAX_REGIONS_ALLOWED)  {
	        			 Toast.makeText(getContext(), getResources().getString(R.string.maxRegionsWarningBegin) 
	        					 					+ MAX_REGIONS_ALLOWED
	        					 					+ getResources().getString(R.string.maxRegionsWarningEnd)
	        					 					, Toast.LENGTH_SHORT).show();
	        		 }
	        		 else if( (x <= mMaxWidth) && (y <= mMaxHeight))
	        		 { // the rectangle is probably legal to place
				        Rectangle rect = new Rectangle();
				        rect.x0 = factorRound((int)Math.max(0,x-(DEFAULT_RECTANGLE_SIZE/2)));
				        rect.y0 = factorRound((int)Math.max(0,y-(DEFAULT_RECTANGLE_SIZE/2)));
				        rect.xEnd = factorRound((int)Math.min(mMaxWidth, x+(DEFAULT_RECTANGLE_SIZE/2)));
				        rect.yEnd = factorRound((int)Math.min(mMaxHeight,y+(DEFAULT_RECTANGLE_SIZE/2)));
				        // final checks: it can't intersect with any other rectangle
				        // and needs to have positive area (zero-area rectangles do not
				        // make sense, there would be nothing to encrypt)
				        if(rect.hasIntersectionList(mRects) == -1 &&
				        		rect.area() > 0)
				        	mRects.add(rect);
				        else
				        	Toast.makeText(getContext(), R.string.noOverlapWarning, Toast.LENGTH_SHORT).show();
	        		 }
	        	 }  else  { // is in some rectangle: might be resize, move or remove
	        		 Rectangle r = mRects.get(index);
	        		 // if lower right small rectangle (the resizing control)
	        		 if( new Rectangle(r.xEnd - DEFAULT_SIZE_CTRL_WIDTH,
	        				 		   r.yEnd - DEFAULT_SIZE_CTRL_WIDTH,
	        				 		   r.xEnd, r.yEnd).isInRectangle(x, y) )  {
	        			 mCurrentSelectedResizingRect = index;
	        		 }  else  { // interior of the rectangle, we intend to move or remove it
	        			 mCurrentSelectedMovingRect = index;
	        			 mStartingUpCorner = r.y0;
	        			 mStartingLeftCorner = r.x0;
	        			 mWantsToDelete = true; 
	        			 // The user *may* want to delete the rectangle (or maybe move it). 
	        			 // It will be determined by next action (move or up)
	        		 }
	        	 }
	         }  else if(event.getAction() == MotionEvent.ACTION_UP)  {
	        	 mCurrentSelectedResizingRect = -1;
	        	 
	        	 if(mWantsToDelete)
	        		 mRects.remove(mCurrentSelectedMovingRect);
	
	        	 mCurrentSelectedMovingRect = -1;
	        	 mWantsToDelete = false;
	         }
	         else if(event.getAction() == MotionEvent.ACTION_MOVE)
	         {
	        	 if(mCurrentSelectedResizingRect != -1)  { // a rectangle is being resized
	        		 Rectangle oldRect = mRects.get(mCurrentSelectedResizingRect);
	        		 Rectangle newRect = new Rectangle(oldRect.x0, oldRect.y0,
     				 		factorRound(Math.min(Math.max(oldRect.x0 + DEFAULT_SIZE_CTRL_WIDTH, x),mMaxWidth)),
     				 		factorRound(Math.min(Math.max(oldRect.y0 + DEFAULT_SIZE_CTRL_WIDTH, y),mMaxHeight)));
	        		 if(newRect.hasIntersectionListExcept(mRects, mCurrentSelectedResizingRect) == -1 
	        				 && newRect.area() > 0)  {
	        			 mRects.remove(oldRect);
	        			 newRect.setPermissions(oldRect.getPermissionsArrayList());
	        			 mRects.add(mCurrentSelectedResizingRect, newRect);
	        		 }
	        	 }  else if(mCurrentSelectedMovingRect != -1)  { // a rectangle is being moved
	        		 Rectangle oldRect = mRects.get(mCurrentSelectedMovingRect);
	        		 Rectangle newRect = new Rectangle(
	        				 factorRound(Math.min(Math.max(x - (oldRect.getWidth()/2), 0), mMaxWidth - DEFAULT_SIZE_CTRL_WIDTH)),
	        				 factorRound(Math.min(Math.max(y - (oldRect.getHeight()/2), 0), mMaxHeight - DEFAULT_SIZE_CTRL_WIDTH)),
	        				 factorRound(Math.min(Math.max(x + (oldRect.getWidth()/2),DEFAULT_SIZE_CTRL_WIDTH), mMaxWidth)),
	        				 factorRound(Math.min(Math.max(y + (oldRect.getHeight()/2),DEFAULT_SIZE_CTRL_WIDTH),mMaxHeight)));
	        		 if(newRect.hasIntersectionListExcept(mRects, mCurrentSelectedMovingRect) == -1 
	        				 && newRect.area() > 0)  {
	        			 mRects.remove(oldRect);
	        			 newRect.setPermissions(oldRect.getPermissionsArrayList());
	        			 mRects.add(mCurrentSelectedMovingRect, newRect);
	        		 }
	        		 
	
	        		 // if the square has been moved far enough from its starting
	        		 // position, we assume we don't want to delete it but move it
	        		 // (even if eventually it is left near its original position)
	        		 if( mWantsToDelete && 
	        				 (Math.abs(x - mStartingLeftCorner) 
	        				 + Math.abs(y - mStartingUpCorner) 
	        				 > REASONABLE_ACCIDENTAL_MOVING_LIMIT))
	        			 mWantsToDelete = false;
	        	 }
	         }
         }  else if ((mMode == ViewMode.PERMISSION_SELECTOR) && (event.getAction() == MotionEvent.ACTION_DOWN))  {
        	 int index = isInAnyRectangle(x,y);
        	 if(index > -1)  {
        		 Intent intent = new Intent(getContext(), ContactPickActivity.class);
        		 intent.putExtra("permissions", mRects.get(index).getPermissions());
    			 ((Activity)getContext()).startActivityForResult(intent, index);
        	 }
         }
    	invalidate(); // redraw
        return true;
    }
    

    // switching between modes
    
    /** Change this view to region selection mode */
	public void switchToRegionSelect()  {
		mMode = ViewMode.REGION_SELECTOR;
		mPaintBorder.setColor(Color.GREEN);
		invalidate(); //redraw
	}
	/** Change this view to permission selection mode */
	public void switchToPermissionSelect()  {
		mMode = ViewMode.PERMISSION_SELECTOR;
		mPaintBorder.setColor(Color.RED);
		invalidate(); //redraw
	}
    
    /** Returns -1 if the point (x,y) is not in any of the stored rectangles,
     * and otherwise returns the index (in _rects) of the rectangle which contains it.
     */
    private int isInAnyRectangle(int x, int y)
    {
    	int ret = -1;
    	int i = mRects.size() - 1;
    	// search in the list backwards, so that if a point belongs to more
    	// than 1 rectangle, this function returns the most recent, which will
    	// most likely be the one meant. 
    	//Rectangles must not overlap, so it should not matter anymore.
    	while(i >= 0 && ret == -1) {
    		if(mRects.get(i).isInRectangle(x, y))
    			ret = i;
			i--;
    	}
    	return ret;
    }
    
    /**
     * Returns the list of rectangles as an ArrayList of Strings, which
     * can be put as an extra in an Intent.
     */
    public ArrayList<String> getRectanglesString()
    {
    	ArrayList<String> rects_string = new ArrayList<String>();
    	for(Rectangle r : mRects)
    		rects_string.add(r.toString(mSampleSize/mAspectRateFactor));
    	return rects_string;
    }
    
    public ArrayList<Rectangle> getRectangles()  {
    	return mRects;
    }
    
    public void setRectangles(ArrayList<Rectangle> list)  {
    	if(list != null)
    		mRects = list;
    	else
    		mRects = new ArrayList<Rectangle>();
    }
    
    protected void clearRectangles() { mRects = new ArrayList<Rectangle>(); }
    
    public ViewMode getViewMode()   { return mMode; }
    public void setViewMode(ViewMode newMode)  { mMode = newMode; }
    
    public float getAspectRate()  { return mAspectRateFactor; }
    public void setAspectRate(float oldAspectRate)   { mAspectRateFactor = oldAspectRate; }
    
    public int getImageWidth() { return mOriginalWidth; }
    public int getImageHeight() { return mOriginalHeight; }
    
    public int getSampleSize()  {  return mSampleSize;  }
	
	/** sets the permissions of rectangle with number @param rect as @param usernames */
	public void setRectanglePermissions(ArrayList<String> usernames, int rect)  {
		mRects.get(rect).setPermissions(usernames);
		invalidate();
	}
	
	public void findFaces(String path)  {
		(new FaceFinderTask(path)).execute();
	}
	
	private class FaceFinderTask extends AsyncTask<Void,Void,Integer>  {

		private ProgressDialog _mProgressDialog;
		private String _mPath;
		
		public FaceFinderTask(String path)  {
			_mPath = path;
		}
		
		@Override
		public void onPreExecute()  {
			 _mProgressDialog = new ProgressDialog(getContext(), ProgressDialog.THEME_HOLO_DARK);
	         _mProgressDialog.setTitle(R.string.searchingDialog);
	         _mProgressDialog.setMessage(getContext().getString(R.string.pleaseWaitDialog));
	         _mProgressDialog.setCancelable(false);
	         _mProgressDialog.setIndeterminate(true);
	         _mProgressDialog.show();
		}
		
		@Override
		protected Integer doInBackground(Void... args) {
			return getFaceCoord(_mPath);
		}
		@Override
		public void onPostExecute(Integer numFaces)  {
			_mProgressDialog.dismiss();
			invalidate();
			if(numFaces == 0)
				Toast.makeText(getContext(), R.string.noFacesFound, Toast.LENGTH_SHORT).show();
		}

	    /** get face coordinates, using FaceDetector
	     * @param path: path of the image file
	     */
		protected Integer getFaceCoord(String path)
		{
			int sampleSizeForFaceDetection = 1;

			/* We need to load the image again, since FaceDetector only works in RGB-565.
			 * If it's big, we sample it at a broader resolution, so as to speed the process.
			 */
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true; // get dimensions without reloading the image
			BitmapFactory.decodeFile(path, options);
			int width = options.outWidth;
			int height = options.outHeight;
			
			sampleSizeForFaceDetection = Math.max(width/512, height/512);
			
			options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			options.inSampleSize = sampleSizeForFaceDetection;
			Bitmap bmp = BitmapFactory.decodeFile(path, options);
			width = options.outWidth;
			height = options.outHeight;
			
			Face[] detectedFaces= new FaceDetector.Face[MAX_NUMBER_OF_FACES];
			FaceDetector faceDetector=new FaceDetector(width,height,MAX_NUMBER_OF_FACES);
			int numberOfFacesFound = faceDetector.findFaces(bmp, detectedFaces);
			if(numberOfFacesFound > 0)
			{
				clearRectangles();
				for(int i=0; i<numberOfFacesFound; i++)
				{
					Face face = detectedFaces[i];
					PointF midPoint = new PointF();
					face.getMidPoint(midPoint);
					Rectangle newRect = new Rectangle(
							factorRound((int)(sampleSizeForFaceDetection*Math.round(midPoint.x - Math.max(face.eyesDistance(), DEFAULT_SIZE_CTRL_WIDTH/2))*mAspectRateFactor)),
							factorRound((int)(sampleSizeForFaceDetection*Math.round(midPoint.y - Math.max(face.eyesDistance(), DEFAULT_SIZE_CTRL_WIDTH/2))*mAspectRateFactor)),
							factorRound((int)(sampleSizeForFaceDetection*Math.round(midPoint.x + Math.max(face.eyesDistance(), DEFAULT_SIZE_CTRL_WIDTH/2))*mAspectRateFactor)),
							factorRound((int)(sampleSizeForFaceDetection*Math.round(midPoint.y + Math.max(3*face.eyesDistance()/2, DEFAULT_SIZE_CTRL_WIDTH/2))*mAspectRateFactor)));
					if(newRect.area() > 0)  {
						if(newRect.hasIntersectionList(mRects) == -1)  {
							mRects.add(newRect);
						}
					}
				}
			}
			return numberOfFacesFound;
		}
	}
}
