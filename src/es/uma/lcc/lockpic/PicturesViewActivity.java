package es.uma.lcc.lockpic;

import java.util.ArrayList;

import es.uma.lcc.nativejpegencoder.R;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/** 
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * Activity which holds a list with all pictures uploaded by the user
 */

public class PicturesViewActivity extends ListActivity {
	Cursor mCursor;
	ArrayAdapter<String> mAdapter;
	ArrayList<String> mPictures;
	ArrayList<String> mIds = null;
	ArrayList<String> mDates;
	
	 @Override
     protected void onCreate(Bundle savedInstanceState)  {
         super.onCreate(savedInstanceState);
         this.setContentView(R.layout.mypicsview);
         Intent intent = getIntent();
    	 getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    	 
         if(intent != null && intent.getStringArrayListExtra("pictures") != null)  {
        	 mPictures = intent.getStringArrayListExtra("pictures");
        	 mIds = intent.getStringArrayListExtra("ids");
        	 mDates = intent.getStringArrayListExtra("dates");
         }  else  {
        	 mPictures = new ArrayList<String>();
         }
    	 mAdapter = new TwolineAdapter(this, mPictures, mDates);
    	 setListAdapter(mAdapter);
     }
	 
	 @Override
	  public void onListItemClick(ListView l, View v, int position, long id) {
		String picId;
		if(mIds != null && mIds.size() > position)
			picId = mIds.get(position);
		else
			picId = null;
		Intent intent = new Intent();
		intent.putExtra("picId", picId);
		setResult(RESULT_OK, intent);
		finish();
	  }
	 
	 private class TwolineAdapter extends ArrayAdapter<String> {

		    private ArrayList<String> _mPictures;
		    private ArrayList<String> _mDates;

		    public TwolineAdapter(Context context, ArrayList<String> pictures, ArrayList<String> dates) {
		        super(context, android.R.layout.simple_list_item_2, android.R.id.text1, pictures);
		        _mPictures = pictures;
		        _mDates = dates;
		    }

		    @Override
		    public View getView(int position, View convertView, ViewGroup parent) {
		    	View view = super.getView(position, convertView, parent);
		        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
		        TextView text2 = (TextView) view.findViewById(android.R.id.text2);
		        text1.setText(_mPictures.get(position));
		        text2.setText(getResources().getString(R.string.dateCreated) + _mDates.get(position));
		        return view;
		    }
		}
		
}
