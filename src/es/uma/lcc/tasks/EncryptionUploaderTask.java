package es.uma.lcc.tasks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TreeSet;

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
import es.uma.lcc.utils.Rectangle;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import static es.uma.lcc.utils.Constants.*;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * AsyncTask which notifies the server of the intention to encrypt an image.
 * It sends the server a list of regions and permissions, and receives the keys
 * which will be used to encrypt said regions.
 */

public class EncryptionUploaderTask extends AsyncTask<Void, Integer, String>  {
	
	final static String LOG_ERROR = "EncryptionUploaderError";
	String mPermissions;
	MainActivity mMainActivity;
	Cookie mCookie = null;
	String mSrc;
	int mWidth, mHeight; // image dimensions
	ArrayList<String> mRectangles;
	boolean mIsAuthError = false;
	boolean mIsFirstRun = true;
	int[] mHorizStarts;
	int[] mHorizEnds;
	int[] mVertStarts;
	int[] mVertEnds;
	String[] mKeys;
	int mSquareNum;
	String mNewId;
	boolean mSuccess = false, mConnectionSucceeded = true;
	ProgressDialog mProgressDialog;
	
	public EncryptionUploaderTask(String src, int width, int height,
				ArrayList<String> rects, MainActivity mainActivity)  {
		mSrc = src;
		mWidth = width;
		mHeight = height;
		mRectangles = rects;
		mMainActivity = mainActivity;
	}
	
	private EncryptionUploaderTask(String src, ArrayList<String> rects, MainActivity mainActivity, boolean isFirstRun)  {
		mSrc = src;
		mRectangles = rects;
		mMainActivity = mainActivity;
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
	public String doInBackground(Void...voids) {

		String dst = null;
		String filename = mSrc.substring(mSrc.lastIndexOf("/")+1, mSrc.lastIndexOf("."));
		
		ArrayList<Rectangle> rects = new ArrayList<Rectangle>();
		for(String s : mRectangles)
			rects.add(new Rectangle(s));
		
		mSquareNum = rects.size();
		mHorizStarts = new int[mSquareNum];
	   	mHorizEnds = new int[mSquareNum];
	   	mVertStarts = new int[mSquareNum];
	   	mVertEnds = new int[mSquareNum];
	   	mKeys = new String[mSquareNum];
	   	for(int i=0; i<mSquareNum; i++)  {
	   		 mHorizStarts[i] = rects.get(i).x0 / 16;
	   		 mHorizEnds[i] = rects.get(i).xEnd / 16;
	   		 mVertStarts[i] = rects.get(i).y0 / 16;
	   		 mVertEnds[i] = rects.get(i).yEnd / 16;
	   	}
	   	
		mNewId = null;
		boolean permissionToSelf = false;
		JSONArray permissions = new JSONArray();
		try  {
			JSONObject obj = new JSONObject();
			JSONArray usernames;
			obj.put(JSON_PROTOCOLVERSION, CURRENT_VERSION);
			obj.put(JSON_FILENAME, filename);
			obj.put(JSON_IMGHEIGHT, mHeight);
			obj.put(JSON_IMGWIDTH, mWidth);
			permissions.put(obj);
			
	    	for(int i=0; i<mSquareNum; i++)  {
				TreeSet<String> auxSet = new TreeSet<String>(); 
					// helps in checking a permission is not granted twice
    			obj = new JSONObject();
    			obj.put(JSON_HSTART, mHorizStarts[i]);
    			obj.put(JSON_HEND, mHorizEnds[i]);
    			obj.put(JSON_VSTART, mVertStarts[i]);
    			obj.put(JSON_VEND, mVertEnds[i]);
    			usernames = new JSONArray();
    			usernames.put(mMainActivity.getUserEmail().toLowerCase(Locale.ENGLISH));
    			auxSet.add(mMainActivity.getUserEmail().toLowerCase(Locale.ENGLISH));
	    		for(String str : rects.get(i).getPermissionsArrayList() )  {
	    			if(!auxSet.contains(str.toLowerCase(Locale.ENGLISH)))  {
	    				usernames.put(str.toLowerCase(Locale.ENGLISH));
	    				auxSet.add(str.toLowerCase(Locale.ENGLISH));
	    			}  else if (str.equalsIgnoreCase(mMainActivity.getUserEmail()))
	    				permissionToSelf = true;
	    		}
	    		obj.put(JSON_USERNAME, usernames);
	    		permissions.put(obj);
		   	}
		}  catch(JSONException jsonex)  {
			// Will never happen: every value is either a number, or a correctly formatted email
		}
		if(permissionToSelf)  {  publishProgress(5);  }
		DefaultHttpClient httpclient = new DefaultHttpClient();
		final HttpParams params = new BasicHttpParams();
		HttpClientParams.setRedirecting(params, false);
		httpclient.setParams(params);
		String target = SERVERURL + "?" + QUERYSTRING_ACTION + "=" + ACTION_ONESTEPUPLOAD;
		HttpPost httppost = new HttpPost(target);
		
		while(mCookie == null)   {  // loop until authentication finishes, if necessary
			mCookie = mMainActivity.getCurrentCookie();
		}
		
		try {
		    StringEntity permissionsEntity = new StringEntity(permissions.toString());
		    permissionsEntity.setContentType(new BasicHeader("Content-Type",
		        "application/json"));
		    httppost.setEntity(permissionsEntity);

			httppost.setHeader("Cookie", mCookie.getName() + "=" + mMainActivity.getCurrentCookie().getValue());
			System.out.println("Cookie in header: " + mMainActivity.getCurrentCookie().getValue());
			
			HttpResponse response = httpclient.execute(httppost);

			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() != 200) {
				mConnectionSucceeded = false;
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
						 mConnectionSucceeded = false;
			         }  else  {
			        	 // Elements in a JSONArray keep their order
				         JSONObject successState = jsonArray.getJSONObject(0);
				         if(successState.get(JSON_RESULT).equals(JSON_RESULT_ERROR))  {
				        	 if(successState.getBoolean(JSON_ISAUTHERROR) && mIsFirstRun)  {
				        		 mIsAuthError = true;
				        		 Log.e(APP_TAG, LOG_ERROR + ": Server found an auth error: " + successState.get(JSON_REASON));
				        	 }
				        	 else  {
				        		 mConnectionSucceeded = false;
				        		 Log.e(APP_TAG, LOG_ERROR +": Server found an error: " + successState.get("reason"));
			        		 }
				         }  else  { // everything went OK
				        		 mNewId = jsonArray.getJSONObject(1).getString(JSON_PICTUREID);
				        		 for(int i=0; i<mSquareNum; i++)  {
				        			 mKeys[i] = jsonArray.getJSONObject(i+2).getString(JSON_KEY);
				        		 }
				        		 if(mNewId == null)  {
				     					mConnectionSucceeded = false;
				        				Log.e(APP_TAG, "Encryption: Error connecting to server");
				        			}  else  {
				        				publishProgress(10);
				        				String date = new SimpleDateFormat(
				        						"yyyyMMddHHmmss", Locale.US).format(new Date());
				        				
				        				File directory = new File(Environment.getExternalStorageDirectory() + "/" + APP_TAG);
				        				if(!directory.exists())  {  directory.mkdir();  }
				        					
				        				
				        				dst = Environment.getExternalStorageDirectory() 
				        						+ "/" + APP_TAG + "/" + ENCRYPTED_FILE_PREFIX + filename + "_" + date + ".jpg";
				        				
				        				mSuccess = MainActivity.encodeWrapperRegions(mSrc, dst, mSquareNum, mHorizStarts, 
				        									mHorizEnds, mVertStarts, mVertEnds, mKeys, mNewId);
				        				
				        			    addToGallery(dst);
				        			}
					         }
			         }
				} catch (JSONException jsonEx)  {
					mConnectionSucceeded = false;
					Log.e(APP_TAG, LOG_ERROR + ": Malformed JSON response from server");
				}
			}
		} catch (ClientProtocolException e) {
			mConnectionSucceeded = false;
		} catch (IOException e) {
			mConnectionSucceeded = false;
		}
		return dst;
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		if(progress[0] == 10)  {
			mProgressDialog.setTitle(R.string.encryptingDialog);
		}  else if (progress[0] == 5)  {
			Toast.makeText(mMainActivity, R.string.permissionToSelfWarning, Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onPostExecute(String dst)  {
		mProgressDialog.dismiss();
		if(mConnectionSucceeded)  {
			if(mIsAuthError)  {
				handleAuthenticationError();
			}  else  {
				if(mSuccess)  {
			    	Toast.makeText(mMainActivity, R.string.encryptionSuccess, Toast.LENGTH_SHORT).show();
					mMainActivity.findViewById(R.id.imageBlock).setVisibility(View.VISIBLE);
					mMainActivity.findViewById(R.id.encryptBlock).setVisibility(View.GONE);
					mMainActivity.findViewById(R.id.decryptBlock).setVisibility(View.GONE);
					mMainActivity.findViewById(R.id.myPicsBlock).setVisibility(View.GONE);
					mMainActivity.findViewById(R.id.accountBlock).setVisibility(View.GONE);
					mMainActivity.findViewById(R.id.filler1).setVisibility(View.GONE);
					mMainActivity.findViewById(R.id.filler2).setVisibility(View.GONE);
					mMainActivity.findViewById(R.id.filler3).setVisibility(View.GONE);
					mMainActivity.findViewById(R.id.filler4).setVisibility(View.GONE);
					displayEncryptedImage(dst);
			    	showGooglePlusShareButton(dst);
			    }  else  {
			    	Toast.makeText(mMainActivity, R.string.encryptionFailure, Toast.LENGTH_SHORT).show();
			    }	
			}
		}  else  {
			Toast.makeText(mMainActivity, R.string.noConnectionWarning, Toast.LENGTH_SHORT).show();
		}
	}

	private void handleAuthenticationError()  {
		System.out.println("retrying");
		mIsFirstRun = false;
		Thread t = new CookieRefresherThread(AccountManager.get(mMainActivity), mMainActivity);
		t.start();
		try {
			t.join();
			new EncryptionUploaderTask(mSrc, mRectangles, mMainActivity, false).execute();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/* notify the gallery to include the file in path */
	private void addToGallery(String path)  {
		ContentValues values = new ContentValues();
	    values.put(MediaStore.Images.Media.DATA, path);
	    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
	    mMainActivity.getContentResolver().insert(
	    		MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
	}
	
	/* show in imageView the image located in path */
	private void displayEncryptedImage(String path)  {
		ImageView imageView = (ImageView) mMainActivity.findViewById(R.id.imageView);
		imageView.setVisibility(ImageView.VISIBLE);
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		Bitmap bmp = BitmapFactory.decodeFile(path, options);
		int w = options.outWidth;
		int h = options.outHeight;
		
		if(w >= 2048 || h >= 2048)  {
			double factor = (w > h ? 2048.0/w : 2048.0/h);
			imageView.setImageBitmap(Bitmap.createScaledBitmap(bmp, (int)(factor*w), (int)(factor*h), false));
		}  else  {
			imageView.setImageBitmap(bmp);
		}
	}
	
	/* initialize and make visible the "Share to Google+" button */
	private void showGooglePlusShareButton(String path)  {
		Button shareButton = (Button) mMainActivity.findViewById(R.id.shareButton);
		shareButton.setVisibility(View.VISIBLE);
		final String file = path;
		shareButton.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    	File f = new File(file);
		        Intent intent = new Intent(Intent.ACTION_SEND);
		        intent.setType("image/jpeg");
		        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                intent.setPackage("com.google.android.apps.plus");
                try  {
                	mMainActivity.startActivity(intent);
                }  catch(ActivityNotFoundException anfex)  {
                	intent.setPackage(null);
                	mMainActivity.startActivity(intent);
                }
		    }
		});
	}
	
}