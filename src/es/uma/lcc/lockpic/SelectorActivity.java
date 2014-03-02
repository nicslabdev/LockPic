package es.uma.lcc.lockpic;

import java.util.ArrayList;

import es.uma.lcc.nativejpegencoder.R;
import es.uma.lcc.utils.SelectorActivityBundle;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * Activity which allows the user to select regions on a picture before encryption,
 * marking which regions will be obscured and who will be granted permission to decrypt
 * them
 */

public class SelectorActivity extends FragmentActivity {

	DrawView mDrawView;
	public enum ViewMode { REGION_SELECTOR, PERMISSION_SELECTOR };
	
	String mPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	Button btnForward, btnBack, btnFaces;
        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundResource(R.drawable.background_gradient);
        
        mPath = getIntent().getStringExtra("path");
        
        SelectorActivityBundle bundle = (SelectorActivityBundle)getLastCustomNonConfigurationInstance();
        if(bundle != null)  {
	        mDrawView = new DrawView(this, mPath, false);
	        mDrawView.setRectangles(bundle.getRectangles());
	        mDrawView.setViewMode(bundle.getViewMode());
	        mDrawView.setAspectRate(bundle.getAspectRate());
        }  else  {
        	mDrawView = new DrawView(this, mPath, true);
        }
        mDrawView.setId(1);
        
        RelativeLayout.LayoutParams lpDrawView = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lpDrawView.addRule(RelativeLayout.CENTER_VERTICAL);
            mDrawView.setLayoutParams(lpDrawView);
            layout.addView(mDrawView, lpDrawView);
        
        btnForward = new Button(this);
        btnForward.setText(R.string.selectorForwardButton);
        btnForward.setId(2);
        btnForward.setOnClickListener(new forwardButtonListener());
        RelativeLayout.LayoutParams lpButton = new RelativeLayout.LayoutParams(
        		LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpButton.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lpButton.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layout.addView(btnForward, lpButton);
        
        btnBack = new Button(this);
        btnBack.setText(R.string.selectorBackButton);
        btnBack.setId(3);
        btnBack.setOnClickListener(new backButtonListener());
        lpButton = new RelativeLayout.LayoutParams(
        		LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpButton.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lpButton.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layout.addView(btnBack, lpButton);
        
        btnFaces = new Button(this);
        btnFaces.setText(R.string.facesButton);
        btnFaces.setId(4);
        btnFaces.setOnClickListener(new FacesButtonListener());
        if(!isViewSelectingRegions())
        	btnFaces.setVisibility(View.INVISIBLE);
        lpButton = new RelativeLayout.LayoutParams(
        		LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpButton.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lpButton.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layout.addView(btnFaces, lpButton);
        
        setContentView(layout);
    }
    
    private boolean isViewSelectingRegions()  {  return mDrawView.getViewMode() == ViewMode.REGION_SELECTOR;  }
    
    private class forwardButtonListener implements View.OnClickListener {
		public void onClick(View view) {
			if(isViewSelectingRegions())  {
				if(mDrawView.getRectangles().size() == 0)  {
					Toast.makeText(getApplicationContext(), R.string.noRegionsWarning, Toast.LENGTH_LONG).show();
				}  else  {
					mDrawView.switchToPermissionSelect();
			    	findViewById(4).setVisibility(View.INVISIBLE);
					Toast.makeText(getApplicationContext(), R.string.addPermissionsPopup, Toast.LENGTH_SHORT).show();
				}
			}  else  { // DrawView is selecting permissions => We intend to finish
				ArrayList<String> stringArray = mDrawView.getRectanglesString();
		    	if(stringArray.size() > 0)  {
			    	Intent intent = new Intent();
			    	intent.putStringArrayListExtra("rectangles", stringArray);
			    	intent.putExtra("path", mPath);
			    	intent.putExtra("sampleSize", mDrawView.getSampleSize());
			    	intent.putExtra("width", mDrawView.getImageWidth());
			    	intent.putExtra("height", mDrawView.getImageHeight());
			    	setResult(RESULT_OK, intent);
			    	finish();
		    	}  else  {
		    		Toast.makeText(getApplicationContext(), 
		    				R.string.noRegionsWarning, Toast.LENGTH_LONG).show();
		    	}
			}
		}
	}
    
    private class backButtonListener implements View.OnClickListener  {
    	public void onClick(View view) {
    		if(!isViewSelectingRegions())  {
		    	mDrawView.switchToRegionSelect();
		    	findViewById(4).setVisibility(View.VISIBLE);
    		}  else  {
    			finish();
    		}
		}
    }
    
    private class FacesButtonListener implements View.OnClickListener  {
    	public void onClick(View view) {
    		mDrawView.findFaces(mPath);
		}
    }
    
    /**
	 * Makes the Android back button work as the UI button (btnBack) set above
	 */
	@Override
	public void onBackPressed(){
		if(!isViewSelectingRegions())  {
	    	mDrawView.switchToRegionSelect();
	    	findViewById(4).setVisibility(View.VISIBLE);
		}  else  {
			finish();
		}
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
        	mDrawView.setRectanglePermissions(data.getStringArrayListExtra("accounts"), requestCode);
        }
    }
	 
	 @Override
	 public Object onRetainCustomNonConfigurationInstance ()  {
		 return new SelectorActivityBundle(mDrawView.getRectangles(), 
				 		mDrawView.getViewMode(), mDrawView.getAspectRate());
	 }
}
