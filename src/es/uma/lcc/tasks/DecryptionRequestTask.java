package es.uma.lcc.tasks;

import static es.uma.lcc.utils.Constants.ACTION_READPERMISSIONS;
import static es.uma.lcc.utils.Constants.APP_TAG;
import static es.uma.lcc.utils.Constants.JSON_HEND;
import static es.uma.lcc.utils.Constants.JSON_HSTART;
import static es.uma.lcc.utils.Constants.JSON_ISAUTHERROR;
import static es.uma.lcc.utils.Constants.JSON_KEY;
import static es.uma.lcc.utils.Constants.JSON_REASON;
import static es.uma.lcc.utils.Constants.JSON_RESULT;
import static es.uma.lcc.utils.Constants.JSON_RESULT_ERROR;
import static es.uma.lcc.utils.Constants.JSON_VEND;
import static es.uma.lcc.utils.Constants.JSON_VSTART;
import static es.uma.lcc.utils.Constants.QUERYSTRING_ACTION;
import static es.uma.lcc.utils.Constants.QUERYSTRING_PICTUREID;
import static es.uma.lcc.utils.Constants.SERVERURL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import es.uma.lcc.lockpic.MainActivity;
import es.uma.lcc.lockpic.MainActivity.CookieRefresherThread;
import es.uma.lcc.nativejpegencoder.R;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * Task used to request the server for permission to decrypt an image.
 * If keys are retrieved, it also applies decryption and shows the result.
 */

public class DecryptionRequestTask extends AsyncTask<Void, Integer, Void> {
	static final String LOG_ERROR = "DecryptionRequestError";
	String mPicId;
	String mSrc;
	MainActivity mMainActivity;
	Cookie mCookie;
	boolean mIsAuthError = false;
	boolean mIsFirstRun = true;
	int[] mHorizStarts, mHorizEnds, mVertStarts, mVertEnds;
	String[] mKeys;
	int mSquareNum;
	ProgressDialog mProgressDialog;
	Bitmap mDecodedBmp;
	int mWidth, mHeight;
	boolean mHasFoundKeys = false, mConnectionSucceeded = true;
	
	public DecryptionRequestTask(String picId, String src, MainActivity mainActivity)  {
		mPicId = picId;
		mSrc = src;
		mMainActivity = mainActivity;
		mCookie = mainActivity.getCurrentCookie();
	}
	
	private DecryptionRequestTask(String picId, String src, MainActivity mainActivity, boolean isFirstRun)  {
		mPicId = picId;
		mSrc = src;
		mMainActivity = mainActivity;
		mCookie = mainActivity.getCurrentCookie();
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
	
	public Void doInBackground(Void... parameters) {
		if(mPicId == null)  {
			mDecodedBmp = BitmapFactory.decodeFile(mSrc);
		}  else  {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			final HttpParams params = new BasicHttpParams();
			HttpClientParams.setRedirecting(params, false);
			httpclient.setParams(params);
			String target = SERVERURL + "?" + QUERYSTRING_ACTION + "=" + ACTION_READPERMISSIONS 
					+ "&" + QUERYSTRING_PICTUREID + "=" + mPicId;
			HttpGet httpget = new HttpGet(target);
			
			while(mCookie == null) // loop until authentication finishes, if necessary
				mCookie = mMainActivity.getCurrentCookie();
			
			httpget.setHeader("Cookie", mCookie.getName() + "=" + mMainActivity.getCurrentCookie().getValue());
			System.out.println("Cookie in header: " + mMainActivity.getCurrentCookie().getValue());
			try {
				HttpResponse response = httpclient.execute(httpget);
	
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
				         JSONObject jsonObj;
				         if(jsonArray.length() == 0)  {
				        	 // should never happen
				        	 Log.e(APP_TAG, LOG_ERROR + ": Malformed response from server");
				         }  else  {
				        	 // Elements in a JSONArray keep their order
					         JSONObject successState = jsonArray.getJSONObject(0);
					         if(successState.get(JSON_RESULT).equals(JSON_RESULT_ERROR))  {
					        	 if(successState.getBoolean(JSON_ISAUTHERROR) && mIsFirstRun)  {
					        		 mIsAuthError = true;
					        		 Log.e(APP_TAG, LOG_ERROR + ": Server found an auth error: " + successState.get(JSON_REASON));
					        	 }  else  {
					        		 Log.e(APP_TAG, LOG_ERROR + ": Server found an error: " + successState.get(JSON_REASON));
					        	 }
					         }  else  {
					        	 mSquareNum = jsonArray.length()-1;
					        	 mHorizStarts = new int[mSquareNum];
					        	 mHorizEnds = new int[mSquareNum];
					        	 mVertStarts = new int[mSquareNum];
					        	 mVertEnds = new int[mSquareNum];
					        	 mKeys = new String[mSquareNum];
					        	 for(int i = 0; i < mSquareNum; i++)  {
					        		 jsonObj = jsonArray.getJSONObject(i+1);
					        		 mHorizStarts[i] = jsonObj.getInt(JSON_HSTART);
					        		 mHorizEnds[i] = jsonObj.getInt(JSON_HEND);
					        		 mVertStarts[i] = jsonObj.getInt(JSON_VSTART);
					        		 mVertEnds[i] = jsonObj.getInt(JSON_VEND);
					        		 mKeys[i] = jsonObj.getString(JSON_KEY);
					        	 }
					 			publishProgress(10);
					 			BitmapFactory.Options options = new BitmapFactory.Options();
								options.inJustDecodeBounds = true; // get dimensions without reloading the image
								BitmapFactory.decodeFile(mSrc, options);
								mWidth = options.outWidth;
								mHeight = options.outHeight;
								mDecodedBmp = Bitmap.createBitmap(options.outWidth, options.outHeight, Config.ARGB_8888);
													
								if(mSquareNum == 0)  {
									mDecodedBmp = BitmapFactory.decodeFile(mSrc);
								}  else  {
									mHasFoundKeys = true;
									byte[] pixels = MainActivity.decodeWrapperRegions(mSrc, mSquareNum, mHorizStarts,
											mHorizEnds, mVertStarts, mVertEnds, mKeys, mPicId);
									
									/* Native side returns each pixel as an RGBA quadruplet, with
									 each a C unsigned byte ([0,255]). However, Java reads those
									 as signed bytes ([-128, 127]) in two's complement.
									 We mask them with 0xFF to ensure most significant bits set to 0,
									 therefore interpreting them as their positive (unsigned) value.
									 Furthermore, Java functions take pixels in ARGB (and not RGBA),
									 so we manually re-order them */
									int baseIndex;
									for(int i = 0; i < mWidth; i++)  {
										for(int j = 0; j < mHeight; j++)  {
											baseIndex = 4*(j*mWidth + i);
											mDecodedBmp.setPixel(i, j, Color.argb(pixels[baseIndex+3]&0xff, pixels[baseIndex]&0xff, 
													pixels[baseIndex+1]&0xff, pixels[baseIndex+2]&0xff));
										}
									}
								}
					         }
				         }
					} catch (JSONException jsonEx)  {
						Log.e(APP_TAG, LOG_ERROR + ": Malformed JSON response from server");
						mSquareNum = 0; 
					}
				}
			} catch (ClientProtocolException e) {
				mConnectionSucceeded = false;
				Log.e(APP_TAG, LOG_ERROR + ": " + e.getMessage());
			} catch (IOException e) {
				mConnectionSucceeded = false;
				Log.e(APP_TAG, LOG_ERROR + ": " + e.getMessage());
			}
		}
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		if(progress[0] == 10)  {
			mProgressDialog.setTitle(R.string.decryptingDialog);
		}
	}
	
	@Override
	public void onPostExecute(Void l)  {
		mProgressDialog.dismiss();
		if(mConnectionSucceeded)  {
			if(mIsAuthError)  {
				handleAuthenticationError();
			}  else  {
				mMainActivity.findViewById(R.id.imageBlock).setVisibility(View.VISIBLE);
				mMainActivity.findViewById(R.id.encryptBlock).setVisibility(View.GONE);
				mMainActivity.findViewById(R.id.decryptBlock).setVisibility(View.GONE);
				mMainActivity.findViewById(R.id.myPicsBlock).setVisibility(View.GONE);
				mMainActivity.findViewById(R.id.accountBlock).setVisibility(View.GONE);
				mMainActivity.findViewById(R.id.filler1).setVisibility(View.GONE);
				mMainActivity.findViewById(R.id.filler2).setVisibility(View.GONE);
				mMainActivity.findViewById(R.id.filler3).setVisibility(View.GONE);
				mMainActivity.findViewById(R.id.filler4).setVisibility(View.GONE);
				ImageView imageView = (ImageView) mMainActivity.findViewById(R.id.imageView);
				imageView.setVisibility(ImageView.VISIBLE);
				if(!mHasFoundKeys)  {
					Toast.makeText(mMainActivity, R.string.noKeysRetrievedWarning, Toast.LENGTH_SHORT).show();
				}
				
				if(mWidth >= 2048 || mHeight >= 2048)  {
					double factor = (mWidth > mHeight ? 2048.0/mWidth : 2048.0/mHeight);
					imageView.setImageBitmap(Bitmap.createScaledBitmap(mDecodedBmp, (int)(factor*mWidth), (int)(factor*mHeight), false));
				}  else  {
					imageView.setImageBitmap(mDecodedBmp);
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
			(new DecryptionRequestTask(mPicId, mSrc, mMainActivity, false)).execute();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
