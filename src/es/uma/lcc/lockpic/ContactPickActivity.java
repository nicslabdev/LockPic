package es.uma.lcc.lockpic;

import static es.uma.lcc.utils.Constants.CONTACTS_FILENAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.uma.lcc.nativejpegencoder.R;
import es.uma.lcc.utils.Rectangle;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * This class is just an activity (in Dialog style, please see 
 * res/layout/listview.xml) which displays a list of contact emails,
 * and lets the user check which of those will be given permission
 * on a specific square.
 * 
 * It returns on the result intent the list of emails selected, 
 * as an ArrayList<String>.
 * 
 * It is meant to be used as an auxiliary activity for SelectorActivity and
 * PictureDetailsActivity.
 * 
 * Copyright (C) 2014  Carlos Parés: carlosparespulido (at) gmail (dot) com
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class ContactPickActivity extends ListActivity {
	Cursor mCursor;
	ArrayAdapter<String> mAdapter;
	ArrayList<String> mContacts;
	ArrayList<String> mSelectedContacts;
	Set<String> mStoredContacts; // historic of hand-written contacts, stored in the settings file 
	Integer mGroupId; // when called from PictureDetails, we need to store from which group
	
	private void getContacts(){
        try{
        	String[] projection = new String[] {
            		ContactsContract.Data.DATA1
            };
        	Cursor cursor = getContentResolver().query(
        			ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection, "DATA1 IS NOT NULL AND DATA1 <> \"\"" ,null, null);
        	while (cursor.moveToNext()) {
        		mContacts.add(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)));
        	}
        	cursor.close();
        	
        	SharedPreferences settings = getSharedPreferences(CONTACTS_FILENAME, MODE_PRIVATE);
        	mStoredContacts = settings.getStringSet("stored_contacts", new HashSet<String>());
        	
        	boolean updated = false;
        	for(String str: mStoredContacts)  {
        		if(!mContacts.contains(str))  {
        			mContacts.add(str);
        		}  else  {
        			mStoredContacts.remove(str);
        			updated = true;
        		}
        	}
        	if(updated)  {
        		Editor edit = getSharedPreferences(CONTACTS_FILENAME, MODE_PRIVATE).edit();
        		edit.putStringSet("stored_contacts", mStoredContacts);
        		edit.commit();
        	}
        	
        	Collections.sort(mContacts);
      }
        catch(Exception e){
            e.printStackTrace();
        }
    }
	
	 @Override
     protected void onCreate(Bundle savedInstanceState)  {
         super.onCreate(savedInstanceState);
         this.setContentView(R.layout.emailslist);
         Intent intent = getIntent();
         if(intent == null || intent.getStringExtra("permissions") == null)
        	 mSelectedContacts = new ArrayList<String>();
         else
        	 mSelectedContacts = Rectangle.buildPermissionsFromString(intent.getStringExtra("permissions"));
    	 
	     mGroupId = intent.getIntExtra("groupPosition", -1);
    	 
         getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
         mContacts = new ArrayList<String>();
         mStoredContacts = new HashSet<String>();
         getContacts();
         for(String str : mSelectedContacts)  {
        	 if(! mContacts.contains(str))
        		 mContacts.add(str);
         }
         mAdapter = new CustomArrayAdapter<String>(this/*, android.R.layout.simple_list_item_activated_1*/, mContacts);
         setListAdapter(mAdapter);
     }

	 @Override
	public void onWindowFocusChanged(boolean b)  {
		 super.onWindowFocusChanged(b);
		 ListView listView = getListView();
		 int index;
		 for(String str : mSelectedContacts)  {
			 index = mContacts.indexOf(str);
			 if(index != -1)  {
				 listView.setItemChecked(index, true);
			 }
		 }
	 }
	 
	 @Override
	  public void onListItemClick(ListView l, View v, int position, long id) {
		String mail = mContacts.get(position);
		if(mSelectedContacts.contains(mail))  {
			mSelectedContacts.remove(mail);
			l.setItemChecked(position, false);
		}  else  {
			mSelectedContacts.add(mail);
		    l.setItemChecked(position, true);
		}
	  }
	 
	 /** Returns the list of selected accounts */
	 public void buttonDoneClick(View view) {
		   Editor edit = getSharedPreferences(CONTACTS_FILENAME, 
				   							MODE_PRIVATE).edit();
		   edit.putStringSet("stored_contacts", mStoredContacts);
		   edit.commit();
		 	Intent intent = new Intent();
		 	intent.putStringArrayListExtra("accounts", mSelectedContacts);
		 	if (mGroupId != -1)
		 		intent.putExtra("groupPosition", mGroupId);
		 	setResult(RESULT_OK, intent);
		 	finish();
	    }
	 
	 /** Shows a dialog to manually add an user's email */
	 public void buttonAddEmailClick(View view) {
		 final EditText input = new EditText(this);
		 input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		 final AlertDialog d = new AlertDialog.Builder(ContactPickActivity.this)
			 .setTitle(R.string.addEmailTitle)
		     .setView(input)
		     .setPositiveButton(R.string.positiveEmailButton, new Dialog.OnClickListener() {
			 @Override
			 public void onClick(DialogInterface dialog, int whichButton) {
				 // Overridden, see below
			 }
		 })
		 .setNegativeButton(R.string.negativeEmailButton, new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int whichButton) {
				 // Cancelled, nothing to do
			 }
		 })
		 .create();
		 
		 d.setOnShowListener(new DialogInterface.OnShowListener() {

	            @Override
	            public void onShow(DialogInterface dialog) {

	                Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
	                b.setOnClickListener(new View.OnClickListener() {

	                    @Override
	                    public void onClick(View view) {
	                    	String value = input.getText().toString();
	             		   if(android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches())  {
	             			   if(!mContacts.contains(value))  {  mContacts.add(value);  }
	             			   if(!mSelectedContacts.contains(value)) { 
	             				   mSelectedContacts.add(value); 
	             			   }
	             			   if(!mStoredContacts.contains(value))  {
	             				   mStoredContacts.add(value);
	             			   }
	             			   mAdapter.notifyDataSetChanged();
		                       d.dismiss();
	             		   }  else  {
	             			   Toast.makeText(ContactPickActivity.this, R.string.malformedEmailWarning, Toast.LENGTH_SHORT).show();
	             		   }
	                    }
	                });
	            }
	        });
		 d.show();
	    }
	 
	 private class CustomArrayAdapter<T> extends ArrayAdapter<T> {

		    public CustomArrayAdapter(Context context, List<T> items) {

		        super(context, android.R.layout.simple_list_item_activated_1, android.R.id.text1, items);
		    }

		    public CustomArrayAdapter(Context context, T[] items) {

		        super(context, android.R.layout.simple_list_item_activated_1, android.R.id.text1, items);
		    }

		    @Override
		    public View getView(int position, View convertView, ViewGroup parent) {

		        View view = super.getView(position, convertView, parent);
		        TextView textView = (TextView) view.findViewById(android.R.id.text1);
		        textView.setTextSize(18);
		        return view;
		    }
		}
}
