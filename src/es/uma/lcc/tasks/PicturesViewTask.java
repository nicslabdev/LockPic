package es.uma.lcc.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import es.uma.lcc.lockpic.MainActivity;
import es.uma.lcc.lockpic.PicturesViewActivity;
import es.uma.lcc.lockpic.MainActivity.CookieRefresherThread;
import es.uma.lcc.nativejpegencoder.R;
import static es.uma.lcc.utils.Constants.*;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * AsyncTasks which queries the user for all pictures uploaded by the user.
 * Calls PictureDetailsActivity when it receives the response.
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

public class PicturesViewTask extends AsyncTask<Void, Void, Void>  {
	
	final static String LOG_ERROR = "ViewMyPicturesError";
	boolean mIsFirstRun = true;
	MainActivity mMainActivity;
	Cookie mCookie;
	ProgressDialog mProgressDialog;
	boolean mIsAuthError = false;
	
	public PicturesViewTask(MainActivity mainActivity)  {
		mMainActivity = mainActivity;
		mCookie = mMainActivity.getCurrentCookie();
	}
	
	public PicturesViewTask(MainActivity mainActivity, boolean isFirstRun)  {
		this.mIsFirstRun = isFirstRun;
		mMainActivity = mainActivity;
		mCookie = mMainActivity.getCurrentCookie();
	}
	
	@Override
	public void onPreExecute()  {
         mProgressDialog = new ProgressDialog(mMainActivity, ProgressDialog.THEME_HOLO_DARK);
         mProgressDialog.setTitle(R.string.connectingServerDialog);
         mProgressDialog.setMessage(mMainActivity.getString(R.string.pleaseWaitDialog));
         mProgressDialog.setCancelable(false);
         mProgressDialog.setIndeterminate(true);
         mProgressDialog.show();
	}
	
	@Override
	public Void doInBackground(Void... args) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		final HttpParams params = new BasicHttpParams();
		HttpClientParams.setRedirecting(params, false);
		httpclient.setParams(params);
		String target = SERVERURL + "?" + QUERYSTRING_ACTION + "=" + ACTION_MYPICTURES;
		HttpGet httpget = new HttpGet(target);
		
		while(mCookie == null)
			mCookie = mMainActivity.getCurrentCookie();
		
		httpget.setHeader("Cookie", mCookie.getName() + "=" + mMainActivity.getCurrentCookie().getValue());
		try {
			HttpResponse response = httpclient.execute(httpget);

			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() != 200) {
				throw new IOException("Invalid response from server: " + status.toString());
			}

			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = entity.getContent();
				ByteArrayOutputStream content = new ByteArrayOutputStream();

				// Read response into a buffered stream
				int readBytes = 0;
				byte[] sBuffer = new byte[256];
				while ((readBytes = inputStream.read(sBuffer)) != -1) {
					content.write(sBuffer, 0, readBytes);
				}
				String result = new String(content.toByteArray());

				try  {
					JSONArray jsonArray = new JSONArray(result);
			         if(jsonArray.length() == 0)  {
			        	 // should never happen
			        	 Log.e(APP_TAG, LOG_ERROR + ": Malformed response from server");
			         }  else  {
			        	 // Elements in a JSONArray keep their order
				         JSONObject successState = jsonArray.getJSONObject(0);
				         if(successState.get(JSON_RESULT).equals(JSON_RESULT_ERROR))  {
				        	 if(successState.getBoolean(JSON_ISAUTHERROR) && mIsFirstRun)  {
				        		 mIsAuthError = true;
				        	 }
				        	 else  {
				        		 Log.e(APP_TAG, LOG_ERROR + ": Server found an error: " + successState.get("reason"));
			        		 }
				         }  else  {
				        	 ArrayList<String> pics = new ArrayList<String>();
				        	 ArrayList<String> ids = new ArrayList<String>();
				        	 ArrayList<String> dates = new ArrayList<String>();
				        	 SimpleDateFormat oldFormat = new SimpleDateFormat(MISC_DATE_FORMAT, Locale.US);
			        		 SimpleDateFormat newFormat = (SimpleDateFormat) java.text.SimpleDateFormat.getDateTimeInstance();
				        	 JSONObject obj;
				        	 for(int i=1; i < jsonArray.length(); i++)  {
				        		 obj = jsonArray.getJSONObject(i);
				        		 Date d = oldFormat.parse(obj.getString(JSON_DATECREATED));
				        		 dates.add(newFormat.format(d));
				        		 pics.add(obj.getString(JSON_FILENAME));
				        		 ids.add(obj.getString(JSON_PICTUREID));
				        	 }
				        	 Intent intent = new Intent(mMainActivity, PicturesViewActivity.class);
			        		 intent.putStringArrayListExtra("pictures", pics);
			        		 intent.putStringArrayListExtra("ids", ids);
			        		 intent.putStringArrayListExtra("dates", dates);
			        		 mMainActivity.startActivityForResult(intent, ACTIVITY_VIEW_MY_PICTURES);
				         }
			         }
				} catch (JSONException jsonEx)  {
					Log.e(APP_TAG, LOG_ERROR + ": Malformed JSON response from server");
				} catch (ParseException e) {
					// Will not happen: dates are sent by the server in a correct format
					Log.e(APP_TAG, LOG_ERROR + ": Malformed date sent by server");
				}
			}  else  { // entity is null
				Log.e(APP_TAG, LOG_ERROR + ": null response from server");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void onPostExecute(Void arg)  {
		mProgressDialog.dismiss();
		if(mIsAuthError)
			handleAuthenticationError();
	}
	
	private void handleAuthenticationError()  {
		Thread t = new CookieRefresherThread(AccountManager.get(mMainActivity), mMainActivity);
		t.start();
		try {
			t.join();
			(new PicturesViewTask(mMainActivity, false)).execute();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}