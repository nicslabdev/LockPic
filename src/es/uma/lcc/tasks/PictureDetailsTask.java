package es.uma.lcc.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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

import es.uma.lcc.lockpic.MainActivity;
import es.uma.lcc.lockpic.PictureDetailsActivity;
import es.uma.lcc.lockpic.MainActivity.CookieRefresherThread;
import es.uma.lcc.nativejpegencoder.R;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import static es.uma.lcc.utils.Constants.*;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * AsyncTasks which queries the server for regions and permissions on an
 * already encrypted picture.
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

public class PictureDetailsTask extends AsyncTask<Void, Void, Void> {
	final static String LOG_ERROR = "PictureDetailsError";
	
	boolean mIsFirstRun = true;
	boolean mIsAuthError = false;
	String mPicId;
	Cookie mCookie;
	MainActivity mMainActivity;
	ProgressDialog mProgressDialog;
	
	public PictureDetailsTask(MainActivity mainActivity, String picId)  {
		mMainActivity = mainActivity;
		mPicId = picId;
	}
	
	public PictureDetailsTask(MainActivity mainActivity, String picId, boolean isFirstRun)  {
		mMainActivity = mainActivity;
		mIsFirstRun = isFirstRun;
		mPicId = picId;
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
		int width, height;
		final HttpParams params = new BasicHttpParams();
		HttpClientParams.setRedirecting(params, false);
		httpclient.setParams(params);
		String target = SERVERURL + "?" + QUERYSTRING_ACTION + "=" + ACTION_PICTUREDETAILS 
				+ "&" + QUERYSTRING_PICTUREID + "=" + mPicId;
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
				        		 Log.e(APP_TAG, LOG_ERROR + ": Server found an error: " + successState.get(JSON_REASON));
			        		 }
				         }  else  {
				        	 ArrayList<String> users = new ArrayList<String>();
				        	 ArrayList<String> coords = new ArrayList<String>();
				        	 ArrayList<String> ids = new ArrayList<String>();
				        	 JSONObject obj = jsonArray.getJSONObject(0);
				        	 width = obj.getInt(JSON_IMGWIDTH);
				        	 height = obj.getInt(JSON_IMGHEIGHT);
				        	 for(int i=1; i < jsonArray.length(); i++)  {
				        		 obj = jsonArray.getJSONObject(i);
				        		 users.add(obj.getString(JSON_USERNAME));
				        		 coords.add(formatCoordinates(obj.getInt(JSON_HSTART), obj.getInt(JSON_HEND),
				        				 						obj.getInt(JSON_VSTART), obj.getInt(JSON_VEND)));
				        		 ids.add(obj.getString(JSON_PERMISSIONID));
				        	 }
				        	 Intent intent = new Intent(mMainActivity, PictureDetailsActivity.class);
			        		 intent.putStringArrayListExtra("users", users);
			        		 intent.putStringArrayListExtra("coordinates", coords);
			        		 intent.putStringArrayListExtra("ids", ids);
			        		 intent.putExtra("height", height);
			        		 intent.putExtra("width", width);
			        		 intent.putExtra("picId", mPicId);
			        		 intent.putExtra("username", mMainActivity.getUserEmail());
			        		 mMainActivity.startActivityForResult(intent, ACTIVITY_PICTURE_DETAILS);
				         }
			         }
				} catch (JSONException jsonEx)  {
					Log.e(APP_TAG, LOG_ERROR + ": Malformed JSON response from server");
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
			(new PictureDetailsTask(mMainActivity, mPicId, false)).execute();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public String formatCoordinates(int x0, int xEnd, int y0, int yEnd)  {
		return "(" + x0*16 + ", " + y0*16 + ") " 
				+ mMainActivity.getResources().getString(R.string.coordSeparator)
				+ " (" + xEnd*16 + ", " + yEnd*16 + ")";
	}
}
