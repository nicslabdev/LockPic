package es.uma.lcc.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import es.uma.lcc.lockpic.MainActivity;
import es.uma.lcc.lockpic.MainActivity.CookieRefresherThread;
import es.uma.lcc.nativejpegencoder.R;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import static es.uma.lcc.utils.Constants.*;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * AsyncTask to send changes on a picture's permissions.
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

public class SendUpdateTask extends AsyncTask<Void, Void, Void>  {
	final static String LOG_ERROR = "UpdateSenderError";
	String mPicId;
	ArrayList<String> mOperations;
	boolean mIsFirstRun = true;
	boolean mIsAuthError = false;
	boolean mIsSuccessfulUpdate = false;
	MainActivity mMainActivity;
	Cookie mCookie;
	ProgressDialog mProgressDialog;
	
	/* 
	 * Please see PictureDetailsActivity for further information on the format of param operations.
	 * It holds a list of "delete <permission id>" and "add <coordinates> <username>"
	 */
	public SendUpdateTask(ArrayList<String> operations, String picId, MainActivity mainActivity)  {
		mOperations = operations;
		mPicId = picId;
		mMainActivity = mainActivity;
		mCookie = mMainActivity.getCurrentCookie();
	}
	
	private SendUpdateTask(ArrayList<String> operations, String picId, MainActivity mainActivity, boolean isFirstRun)  {
		mOperations = operations;
		mPicId = picId;
		mMainActivity = mainActivity;
		mCookie = mMainActivity.getCurrentCookie();
		mIsFirstRun = isFirstRun;
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
		String target = SERVERURL + "?" + QUERYSTRING_ACTION + "=" + ACTION_UPDATE + "&" + QUERYSTRING_PICTUREID + "=" + mPicId;
		HttpPost httppost = new HttpPost(target);
		while(mCookie == null)
			mCookie = mMainActivity.getCurrentCookie();
		
		httppost.setHeader("Cookie", mCookie.getName() + "=" + mCookie.getValue());
		
		String jsonOperations = buildUpdateOperations(mOperations, mPicId);
		try {
		    StringEntity permissionsEntity = new StringEntity(jsonOperations);
		    permissionsEntity.setContentType(new BasicHeader("Content-Type",
		        "application/json"));
		    httppost.setEntity(permissionsEntity);
			HttpResponse response = httpclient.execute(httppost);

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
			        	 Log.e(APP_TAG, LOG_ERROR +": Malformed response from server");
			         }  else  {
			        	 // Elements in a JSONArray keep their order
				         JSONObject successState = jsonArray.getJSONObject(0);
				         if(successState.get(JSON_RESULT).equals(JSON_RESULT_ERROR))  {
				        	 if(successState.getBoolean(JSON_ISAUTHERROR) && mIsFirstRun)  {
				        		 mIsAuthError = true;
				        	 }
				        	 else  {
				        		 Log.e(APP_TAG, LOG_ERROR +": Server found an error: " + successState.get(JSON_REASON));
			        		 }
				         }  else  {// result is ok, if there were modifications we notify the user
				        	 if(mOperations != null && mOperations.size() > 0)
				        		 mIsSuccessfulUpdate = true;
				         }
			         }
				} catch (JSONException jsonEx)  {
					Log.e(APP_TAG, LOG_ERROR +": Malformed JSON response from server");
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void onPostExecute(Void l)  {
		mProgressDialog.dismiss();
		if(mIsAuthError)  {
			handleAuthenticationError();
		}  else if (mIsSuccessfulUpdate)  {
			Toast.makeText(mMainActivity, R.string.updateSuccessful, Toast.LENGTH_SHORT).show();
		}  else  {
			Toast.makeText(mMainActivity, R.string.updateFailure, Toast.LENGTH_SHORT).show();
		}
	}
	
	private String buildUpdateOperations(ArrayList<String> operations, String picId)  {
		JSONArray result = new JSONArray();
		JSONObject obj;
		StringTokenizer tok;
		obj = new JSONObject();
		try  {
			obj.put(JSON_PROTOCOLVERSION, CURRENT_VERSION);
			obj.put(JSON_PICTUREID, picId);
			result.put(obj);
		}  catch (JSONException jsonex)  {
			// never going to happen - JSON object is always correctly formed
		}
		for(String str : operations)  {
			obj = new JSONObject();
			try  {
				if(str.startsWith("add"))  {
					tok = new StringTokenizer(str, " ");
					obj.put(JSON_UPDATEACTION, tok.nextToken());
					obj.put(JSON_HSTART, (int)Integer.valueOf(tok.nextToken())/16);
					obj.put(JSON_VSTART, (int)Integer.valueOf(tok.nextToken())/16);
					obj.put(JSON_HEND, (int)Integer.valueOf(tok.nextToken())/16);
					obj.put(JSON_VEND, (int)Integer.valueOf(tok.nextToken())/16);
					obj.put(JSON_USERNAME, tok.nextToken());
					result.put(obj);
				}  else if(str.startsWith("delete")) {
					tok = new StringTokenizer(str, " ");
					obj.put(JSON_UPDATEACTION, tok.nextToken());
					obj.put(JSON_PERMISSIONID, tok.nextToken());
					result.put(obj);
				}

			// In case of trouble, inform and skip this line - won't happen anyway
			}  catch(JSONException jsonex)  {
				Log.d(APP_TAG, "buildUpdateOperations: JSON error parsing " + str);
			}  catch(NoSuchElementException nsuchex)  {
				Log.d(APP_TAG, "buildUpdateOperations: impossible to parse " + str);
			}
		}
		return result.toString();
	}
	
	private void handleAuthenticationError()  {
		Thread t = new CookieRefresherThread(AccountManager.get(mMainActivity), mMainActivity);
		t.start();
		try {
			t.join();
			new SendUpdateTask(mOperations, mPicId, mMainActivity, false).execute();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
