package es.uma.lcc.lockpic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import es.uma.lcc.nativejpegencoder.R;
import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * Activity which shows who has permissions on a picture, and on which regions.
 * 
 * Copyright (C) 2014  Carlos Parés: carlosparespulido (at) gmail (dot) com
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class PictureDetailsActivity extends ExpandableListActivity {
	
	final String STATE_OPERATIONS = "operations",
				 STATE_PARENT = "parentList",
				 STATE_CHILD = "childList";
	final char SEPARATOR = '|';
	final int ACTIVITY_PICK_CONTACTS = 101;
	
	// needs to be at least Constants.MAX_REGIONS_ALLOWED elements long
	final static int[] helperColors = {Color.GREEN, Color.RED, Color.CYAN, Color.WHITE, 
		Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.BLUE, Color.LTGRAY,
		0xFFFF8800 /*orange*/, 0xFFCA00FC /*purple*/, 0xFFD45E15 /*brown*/, 0xFF0CEBB7 /*aqua*/,
		0xFFFA5C96 /*maroon*/, 0xFF6B2104 /*dark brown*/};
	
    String mPicId; // identifier of the picture
    String mUserMail; // mail of the current logged in account
    int mHeight, mWidth; // dimensions of the image
    
    SimpleExpandableListAdapter mExpListAdapter;
    ArrayList<Map<String, String>> mParentList = new ArrayList<Map<String, String>>();
    ArrayList<ArrayList<Map<String, String>>> mChildList = new ArrayList<ArrayList<Map<String,String>>>();
    ArrayList<String> mOperations = new ArrayList<String>();
    
	@Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        
        ArrayList<String> users;
        ArrayList<String> coords = new ArrayList<String>();
        ArrayList<String> ids;
        
        this.setContentView(R.layout.picturedetailsview);
        
        ArrayList<String> operations = null;
        if(savedInstanceState != null)
        	operations = savedInstanceState.getStringArrayList(STATE_OPERATIONS);
        
        Intent intent = getIntent();
        if(intent == null 
        		|| intent.getStringArrayListExtra("users") == null
        		|| intent.getStringArrayListExtra("coordinates") == null
        		|| intent.getStringArrayListExtra("ids") == null)  {
        	finish(); // should never happen in normal usage
        }  else  {
        	 mPicId = intent.getStringExtra("picId");
	       	 mUserMail = intent.getStringExtra("username");
	       	 mWidth = intent.getIntExtra("width", 2000);
	       	 mHeight = intent.getIntExtra("height", 2000);
	       	 
	       	 if(operations == null)  { // first time created
	       		 users = intent.getStringArrayListExtra("users");
		       	 coords = intent.getStringArrayListExtra("coordinates");
		       	 ids = intent.getStringArrayListExtra("ids");
	       		 buildLists(users,coords,ids); 
	       	 }  else  {  // already had information in the Bundle
	       		mOperations = operations;
	        	ArrayList<String> parent = savedInstanceState.getStringArrayList(STATE_PARENT);
	        	for(String coord : parent)  {
	        		HashMap<String, String> newMap = new HashMap<String,String>();
	        		newMap.put("coordinates", coord);
	        		coords.add(coord);
	        		mParentList.add(newMap);
	        	}
	        	
	        	for(int i=0; i < parent.size(); i++)  {
	        		ArrayList<Map<String, String>> childList = new ArrayList<Map<String, String>>();
		        	ArrayList<String> child = savedInstanceState.getStringArrayList(STATE_CHILD + i);
		        	for(String str : child)  {
		        		int separator = str.lastIndexOf(SEPARATOR);
		        		String id = str.substring(0, separator);
		        		String username = str.substring(separator+1);
		        		HashMap<String, String> newMap = new HashMap<String, String>();
		        		newMap.put("id", id);
		        		newMap.put("username", username);
		        		childList.add(newMap);
		        	}
		        	mChildList.add(childList);
	        	}
	       	 }
        }
        mExpListAdapter =
	            new CustomExpandableListAdapter(
	                    this,
	                    mParentList,                    // Creating group List.
	                    R.layout.group_row,             // Group item layout XML.
	                    new String[] { "coordinates" }, // the key of group item.
	                    new int[] { R.id.row_name },    // ID of each group item.-Data under the key goes into this TextView.
	                    mChildList,                     // childData describes second-level entries.
	                    R.layout.child_row,             // Layout for sub-level entries(second level).
	                    new String[] {"username"},      // Keys in childData maps to display.
	                    new int[] { R.id.grp_child}     // Data under the keys above go into these TextViews.
	                );
            setListAdapter( mExpListAdapter );       // setting the adapter in the list.
            
            for(int i=0; i < mExpListAdapter.getGroupCount(); i++)
            	this.getExpandableListView().expandGroup(i);
            
	       	 drawHelper(coords);
    }
	
	private void drawHelper(ArrayList<String> coordinates)  {
		try  {
			Bitmap helper;
			double factor = 1;
			if(mWidth >= 1024 || mHeight >= 1024)  {
				factor = (mWidth > mHeight ? 1024.0/mWidth : 1024.0/mHeight);
				helper = Bitmap.createBitmap((int)(factor*mWidth), (int)(factor*mHeight), Bitmap.Config.valueOf("RGB_565"));
			}  else  {
				helper = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.valueOf("RGB_565"));
			}
	      	 
	      	 int x0, xEnd, y0, yEnd;
	      	 String coord;
	      	 ArrayList<String> filteredCoordinates = filter(coordinates);
	      	 for(int n = 0; n < filteredCoordinates.size(); n++) {
	      		 coord = filteredCoordinates.get(n);
	      		 StringTokenizer st = new StringTokenizer(coord, 
	      				 "(,) " + getResources().getString(R.string.coordSeparator));
	      		 x0 = Integer.parseInt(st.nextToken());
	      		 x0 = (int)(x0*factor);
	      		 y0 = Integer.parseInt(st.nextToken());
	      		 y0 = (int)(y0*factor);
	      		 xEnd = Integer.parseInt(st.nextToken());
	      		 xEnd = (int)(xEnd*factor);
	      		 yEnd = Integer.parseInt(st.nextToken());
	      		 yEnd = (int)(yEnd*factor);
	      		 
	      		 for(int i = x0; i < xEnd; i++)
	      			 for(int j = y0; j < yEnd; j++)
	      				 helper.setPixel(i, j, helperColors[n]);
	      	 }
	      	 ((ImageView)findViewById(R.id.helperImageView)).setImageBitmap(helper);
		} catch (IllegalArgumentException illargex)  {
			/* Trying to set a pixel out of the image range. Maybe caused by image dimensions
			* being recorded as 0 (should never happen anyway).
			* Hide the helper and work normally. */
			(findViewById(R.id.helperLayout)).setVisibility(View.GONE);
			illargex.printStackTrace();
		}
	}

	/**
	 * removes duplicates from a list, maintaining order
	 */
    private ArrayList<String> filter(ArrayList<String> strings) {
		ArrayList<String> result = new ArrayList<String>();
		for(String str : strings)
			if(!result.contains(str))
				result.add(str);
		return result;
	}

	private void buildLists(ArrayList<String> users, ArrayList<String> coords, 
			ArrayList<String> ids)  {
    	mParentList = new ArrayList<Map<String, String>>();
    	mChildList = new ArrayList<ArrayList<Map<String,String>>>();

    	ArrayList<String> auxList = new ArrayList<String>();
    	
    	for(int i = 0; i < coords.size(); i++)  {    		
    		if(auxList.contains(coords.get(i)))  {
    			if(!users.get(i).equalsIgnoreCase(mUserMail))  { //owner is never shown
	    			HashMap<String, String> childMap = new HashMap<String,String>();
	    			childMap.put("username", users.get(i));
	    			childMap.put("id", ids.get(i));
	    			mChildList.get(auxList.indexOf(coords.get(i))).add(childMap);
    			}
    		}  else  {
    			auxList.add(coords.get(i));
    			HashMap<String,String> parentMap = new HashMap<String,String>();
    			parentMap.put("coordinates", coords.get(i));
    			mParentList.add(parentMap);
    			
    			if(!users.get(i).equalsIgnoreCase(mUserMail))  {
	    			HashMap<String, String> childMap = new HashMap<String,String>();
	    			childMap.put("username", users.get(i));
	    			childMap.put("id", ids.get(i));
	    			ArrayList<Map<String, String>> intermediateList = new ArrayList<Map<String, String>>();
	    			intermediateList.add(childMap);
	    			mChildList.add(intermediateList);
    			}  else  {
    				mChildList.add(new ArrayList<Map<String, String>>());
    			}
    		}
    	}    	
    }
    
    /* This function is called on each child click */
    public boolean onChildClick( ExpandableListView parent, View v, int groupPosition,
    		int childPosition,long id) {
    	final int grpPos = groupPosition;
    	final int chldPos = childPosition;
    	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
    	    @Override
    	    public void onClick(DialogInterface dialog, int which) {
    	        switch (which){
    	        case DialogInterface.BUTTON_POSITIVE:
    	            //"Yes" button clicked
    	        	if(mChildList.get(grpPos).get(chldPos).get("id") != null)
    	        		mOperations.add("delete " + mChildList.get(grpPos).get(chldPos).get("id"));
    	        	else
    	        		mOperations.remove(mOperations.lastIndexOf(
    	        				"add " + asSeparatedNumbers(mParentList.get(grpPos).get("coordinates")) 
    	        				+ " " + mChildList.get(grpPos).get(chldPos).get("username")));
    	        	
    	        	mChildList.get(grpPos).remove(chldPos);
    	        	mExpListAdapter.notifyDataSetChanged();
    	            break;

    	        case DialogInterface.BUTTON_NEGATIVE:
    	            //"No" button clicked
    	            break;
    	        }
    	    }
    	};

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(R.string.confirmRevokePermissionMessage)
    				.setPositiveButton(R.string.revokePermissionPositiveButton, dialogClickListener)
    				.setNegativeButton(R.string.revokePermissionNegativeButton, dialogClickListener).show();
        return true;
    }
	
	private class CustomExpandableListAdapter extends SimpleExpandableListAdapter {

		Context _mContext;
		
		public CustomExpandableListAdapter(Context context,
				List<? extends Map<String, ?>> groupData, int groupLayout,
				String[] groupFrom, int[] groupTo,
				List<? extends List<? extends Map<String, ?>>> childData,
				int childLayout, String[] childFrom, int[] childTo) {
			super(context, groupData, groupLayout, groupFrom, groupTo, childData,
					childLayout, childFrom, childTo);
			_mContext = context;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) { 
		    
		    if (convertView == null) {
		        convertView = View.inflate(_mContext, R.layout.group_row, null);
		        Button addButton = (Button)convertView.findViewById(R.id.group_button);
		        addButton.setOnClickListener(new OnClickListener() {
		            @Override
		            public void onClick(View view) {
		            	Intent intent = new Intent(getApplicationContext(), ContactPickActivity.class);
		            	intent.putExtra("groupPosition", (Integer)view.getTag());
		            	startActivityForResult(intent, ACTIVITY_PICK_CONTACTS);
		            }
		        });
		        addButton.setFocusable(false);
		    }        
		    
		    TextView textView = (TextView)convertView.findViewById(R.id.row_name);
		    textView.setText(((HashMap<String, String>)getGroup(groupPosition)).get("coordinates").toString());
		    textView.setTextColor(helperColors[groupPosition]);
		    Button addButton = (Button)convertView.findViewById(R.id.group_button);
	    	addButton.setTag(Integer.valueOf(groupPosition));
	        
		    return convertView; 
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data)   {
		if (requestCode == ACTIVITY_PICK_CONTACTS && resultCode == RESULT_OK)  {
			int group = data.getIntExtra("groupPosition", -1);
			if(group != -1)  {
				ArrayList<String> accounts = data.getStringArrayListExtra("accounts");
				ArrayList<Map<String,String>> intermediateList = mChildList.get(group);
				String coords;
				for(String account : accounts)  {
					account = account.toLowerCase(Locale.ENGLISH);
					boolean existedAlready = false;
					for(Map<String, String> map : intermediateList)  {
						if( ((String)map.get("username")).equalsIgnoreCase(account) )
							existedAlready = true;
					}
					if(!existedAlready && !mUserMail.equalsIgnoreCase(account))  {
						HashMap<String, String> newChild = new HashMap<String, String>();
						newChild.put("username", account);
						intermediateList.add(newChild);
						coords = mParentList.get(group).get("coordinates");
						mOperations.add("add " + asSeparatedNumbers(coords) + " " + account);
					}
				}
				mExpListAdapter.notifyDataSetChanged();
				if(accounts.size() > 0)
					getExpandableListView().expandGroup(group);
			}
		}
	}
	
	private String asSeparatedNumbers(String coords)  {
		StringTokenizer st = new StringTokenizer(coords, "(), " + getResources().getString(R.string.coordSeparator));
		return st.nextToken() + " " + st.nextToken() + " " + st.nextToken() + " " + st.nextToken();
	}
	
	public void buttonCancelClick(View view)  {
		this.finish();
	}
	
	public void buttonConfirmClick(View view)  {
		Intent intent = new Intent();
	 	intent.putStringArrayListExtra("operations", mOperations);
	 	intent.putExtra("picId", mPicId);
	 	setResult(RESULT_OK, intent);
	 	finish();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {	    
	    savedInstanceState.putStringArrayList(STATE_OPERATIONS, mOperations);
	    ArrayList<String> parentList = new ArrayList<String>();
	    for(Map<String, String> map : mParentList)  {
	    	parentList.add(map.get("coordinates"));
	    }
	    savedInstanceState.putStringArrayList(STATE_PARENT, parentList);

	    ArrayList<String> newList;
	    int index = 0;
	    for(ArrayList<Map<String,String>> list : mChildList)  {
	    	newList = new ArrayList<String>();
	    	for(Map<String, String> map : list)  {
	    		newList.add(  (String)map.get("id") + SEPARATOR + (String)map.get("username"));
	    	}
	    	savedInstanceState.putStringArrayList(STATE_CHILD + index, newList);
	    	index++;
	    }
	    super.onSaveInstanceState(savedInstanceState);
	}
}
