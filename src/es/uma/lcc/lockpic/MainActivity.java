package es.uma.lcc.lockpic; 
 
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import es.uma.lcc.nativejpegencoder.R;
import es.uma.lcc.tasks.DecryptionRequestTask;
import es.uma.lcc.tasks.EncryptionUploaderTask;
import es.uma.lcc.tasks.PictureDetailsTask;
import es.uma.lcc.tasks.PicturesViewTask;
import es.uma.lcc.tasks.SendUpdateTask;
import es.uma.lcc.utils.LockPicIO;
import static es.uma.lcc.utils.Constants.*;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * Activity with the initial GUI. Handles authentication, and does the
 * communication with the server, through the AsyncTasks implemented
 * in es.uma.lcc.tasks.
 */

public class MainActivity extends Activity {
	
	static Account sAccount;
	static String sToken;
	static Cookie mCookie = null;
	String mDirectDecryptPath = null;
	// getters (needed for several tasks)
	public Cookie getCurrentCookie()  { return mCookie; }
	public String getUserEmail()  { return sAccount.name; }
	
	/**
	 * Encrypts a JPEG image and saves it to memory.
	 * This is a native method, please see jni/wrapper.c
	 * @param srcFilename path of file to encrypt
	 * @param dstFilename path to which the encrypted image will be saved
	 * @param squareNum number of regions to encrypt
	 * @param horizStarts left coordinates of regions
	 * @param horizEnds right coordinates of regions
	 * @param vertStarts upper coordinates of regions 
	 * @param vertEnds lower coordinates of regions
	 * @param keys array of keys for encryption
	 * @param picId picture identifier (as assigned by the key server)
	 * @return true if encryption went OK, false otherwise
	 */
	public static native boolean encodeWrapperRegions(String srcFilename, 
					String dstFilename, int squareNum, int[] horizStarts, int[] horizEnds, 
					int[] vertStarts, int[] vertEnds, String[] keys, String picId);
	
	/**
	 * Decrypts a JPEG image and returns it as an array of bytes.
	 * The array will contain the information in RGBA, unsigned (0-255) quadruplets.
	 * 
	 * This is a native method, please see jni/wrapper.c
	 * @param srcFilename path of file to encrypt
	 * @param squareNum number of regions to encrypt
	 * @param horizStarts left coordinates of regions
	 * @param horizEnds right coordinates of regions
	 * @param vertStarts upper coordinates of regions 
	 * @param vertEnds lower coordinates of regions
	 * @param keys array of keys for encryption
	 * @param picId picture identifier (as retrieved from the file comment)
	 * @return an array of pixels as described above
	 */
	public static native byte[] decodeWrapperRegions(String srcFilename, int squareNum, 
					int[] horizStarts, int[] horizEnds, int[] vertStarts, int[] vertEnds, 
					String[] keys, String picId);
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainactivity);
		System.loadLibrary("jpg");  // load native libraries

		// This activity is able to open images directly from a file browser.
		// If this is the case, we store the path, which will be used in onPostCreate
		Intent intent = getIntent();
		if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getType() != null && 
				(savedInstanceState == null || savedInstanceState.getBoolean("firstTime", true))) {
			if (intent.getType().startsWith("image/")) {
				mDirectDecryptPath = LockPicIO.getPathFromIntent(MainActivity.this, intent);
				if(mDirectDecryptPath == null)
					Toast.makeText(this, R.string.unknownIntent, Toast.LENGTH_SHORT).show();
			}
		}
		
		// disable screen capture
		getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE); 
	}
	
	@Override
	protected void onPostCreate(Bundle bundle)  {
		super.onPostCreate(bundle);
		(new AuthenticationTask(MainActivity.this)).execute(); //authenticate
		if(mDirectDecryptPath != null)  { 
			// activity launched from file manager 
			decryptImage(mDirectDecryptPath);
			mDirectDecryptPath = null;
		}
	}
	
	/**
	 * Overrides the back button functionality to behave in a more intuitive way.
	 * When an encrypted/decrypted image is being displayed, pressing Back goes back to the menu
	 * instead of closing the application.
	 */
	@Override
	public void onBackPressed(){
		 ImageView iv = ((ImageView) findViewById(R.id.imageView));
		 if(iv.getVisibility() == ImageView.VISIBLE)  {
			 findViewById(R.id.imageBlock).setVisibility(View.GONE);
			 findViewById(R.id.encryptBlock).setVisibility(View.VISIBLE);
			 findViewById(R.id.decryptBlock).setVisibility(View.VISIBLE);
			 findViewById(R.id.myPicsBlock).setVisibility(View.VISIBLE);
			 findViewById(R.id.accountBlock).setVisibility(View.VISIBLE);
			 findViewById(R.id.filler1).setVisibility(View.VISIBLE);
			 findViewById(R.id.filler2).setVisibility(View.VISIBLE);
			 findViewById(R.id.filler3).setVisibility(View.VISIBLE);
			 findViewById(R.id.filler4).setVisibility(View.VISIBLE);
			 iv.setVisibility(ImageView.INVISIBLE);
			 (findViewById(R.id.shareButton)).setVisibility(View.INVISIBLE);
		 }
		 else
			 this.finish();
	}
	
	@Override
	public void onSaveInstanceState(Bundle bundle)  {
		bundle.putBoolean("firstTime", false);
		// avoid repeating direct decrypt in unnecesary situations
	}
	
	/** 
	 * UI callbacks
	 */

	public void buttonEncryptClick(View view) {
		// intent for launching the gallery
		Intent i = new Intent(Intent.ACTION_PICK,
	               android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, ACTIVITY_PICK_IMAGE_ENC);
    }

	public void buttonDecryptClick(View view) {
		// intent for launching the gallery
		Intent i = new Intent(Intent.ACTION_PICK,
	               android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, ACTIVITY_PICK_IMAGE_DEC);
    }
	
	public void buttonMyPicturesClick(View view) {
		(new PicturesViewTask(MainActivity.this)).execute();
	}
	
	public void logout()  {
		Editor edit = getSharedPreferences(SETTINGS_FILENAME, MODE_PRIVATE).edit();
		edit.remove("preferred_account_name");
		edit.remove("cookie_value");
		edit.remove("cookie_expiry");
		edit.commit();
		mCookie = null;
		sAccount = null;
		(new AuthenticationTask(MainActivity.this)).execute();
	}
	
	public void menuLogoutClick(MenuItem item) {
		logout();
	}
	
	public void buttonLogoutClick(View view)  {
		logout();
	}
	
	/**
	 * reactions to results of activities
	 */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		  if (requestCode == ACTIVITY_SELECTOR && resultCode == RESULT_OK) {
	          encryptImage(data);
		  }  else if (requestCode == ACTIVITY_PICK_IMAGE_ENC && resultCode == RESULT_OK)  {
			  String path = LockPicIO.getPathFromIntent(MainActivity.this, data);
			  if(path != null)  {
				  selectRegions(path);
			  }  else  {
				  Toast.makeText(this, R.string.unreachablePathWarning, Toast.LENGTH_SHORT).show();
			  }
		  } else if (requestCode == ACTIVITY_PICK_IMAGE_DEC && resultCode == RESULT_OK)  {
			  String path = LockPicIO.getPathFromIntent(MainActivity.this, data);
			  if(path != null)  {
				  decryptImage(path);
			  }  else  {
				  Toast.makeText(this, R.string.unreachablePathWarning, Toast.LENGTH_SHORT).show();
			  }
		  }  else if (requestCode == ACTIVITY_VIEW_MY_PICTURES && resultCode == RESULT_OK)  {
			  if(data.getStringExtra("picId") != null)
				  queryPermissions(data.getStringExtra("picId"));
		  }  else if (requestCode == ACTIVITY_PICTURE_DETAILS && resultCode == RESULT_OK)  {
			  if(data.getStringArrayListExtra("operations") != null &&
					  data.getStringArrayListExtra("operations").size() > 0)
				  sendUpdateToServer(data.getStringArrayListExtra("operations"),
						  			 data.getStringExtra("picId") );
		  }
		}
	
	private void encryptImage(Intent data)  {
		String src = data.getStringExtra("path");
		int width = data.getIntExtra("width", 0);
		int height = data.getIntExtra("height", 0);
		(new EncryptionUploaderTask(src, width, height,
									data.getStringArrayListExtra("rectangles"), 
									MainActivity.this)).execute();
	}
	
	private void selectRegions(String src)  {
		Intent intent = new Intent(this, SelectorActivity.class);
		intent.putExtra("path", src);
		startActivityForResult(intent, ACTIVITY_SELECTOR);
	}
	
	private void decryptImage(String src)  {		
		try {
			String id = LockPicIO.getJpegComment(src);
			new DecryptionRequestTask(id, src, MainActivity.this).execute();
		} catch (IOException e) {
			Toast.makeText(this, R.string.fileCorruptedWarning, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void queryPermissions(String pictureId) {
		(new PictureDetailsTask(MainActivity.this, pictureId)).execute();
	}
	
	private void sendUpdateToServer(ArrayList<String> operations, String picId) {
		(new SendUpdateTask(operations, picId, MainActivity.this)).execute();
	}
	
	
	/** 
	 * Authentication functions 
	 **/
	
	private class AuthenticationTask extends AsyncTask<Void,Integer,Boolean>  {

		private MainActivity _mMainActivity;
		private ProgressDialog _mProgressDialog;
		private boolean _mIsConnectedToServer = true;
		
		protected AuthenticationTask(MainActivity mainActivity)  {
			_mMainActivity = mainActivity;
		}
		
		@Override
		public void onPreExecute()  {
	         _mProgressDialog = new ProgressDialog(_mMainActivity, ProgressDialog.THEME_HOLO_DARK);
		}
		
		@Override
		protected Boolean doInBackground(Void... args) {
			return authenticate();
		}
		
		@Override
		protected void onPostExecute(Boolean authSuccess)  {
			try  {
			_mProgressDialog.dismiss();
			}  catch(IllegalArgumentException illArgEx)  {
				// this may be thrown if thread is destroyed before dismiss is processed.
				// In that case the dialog will be discarded anyway
			}
			if(!_mIsConnectedToServer)
				Toast.makeText(getApplicationContext(), R.string.noConnectionWarning, Toast.LENGTH_SHORT).show();
			else if(authSuccess)  {
				 (findViewById(R.id.imageView)).setVisibility(ImageView.INVISIBLE);
				 (findViewById(R.id.buttonEncrypt)).setEnabled(true);
				 (findViewById(R.id.buttonDecrypt)).setEnabled(true);
				 (findViewById(R.id.buttonMyPics)).setEnabled(true);
				 (findViewById(R.id.shareButton)).setVisibility(View.INVISIBLE);
				 (findViewById(R.id.accountBlock)).setVisibility(View.VISIBLE);
				 ((TextView)findViewById(R.id.emailTextView)).setText(sAccount.name);
				 (findViewById(R.id.emailTextView)).setVisibility(View.VISIBLE);
				 (findViewById(R.id.logoutButton)).setVisibility(View.VISIBLE);
			}  else  {
				final Account[] accounts = AccountManager.get(_mMainActivity).getAccountsByType("com.google");
				AlertDialog.Builder builder = new AlertDialog.Builder(_mMainActivity);
				builder.setTitle(R.string.emailSelectTitle);
				builder.setCancelable(false);
				final int size = accounts.length;
				String[] names = new String[size];
				for (int i = 0; i < size; i++) {
					names[i] = accounts[i].name;
				}
				builder.setItems(names, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						sAccount = accounts[which];
						Editor editor = getSharedPreferences(SETTINGS_FILENAME, MODE_PRIVATE).edit();
						editor.putString("preferred_account_name", sAccount.name);
						editor.commit();
						(new AuthenticationTask(_mMainActivity)).execute();
					}});
				builder.create();
				builder.show();
			}
		}
		
		private boolean authenticate()  {
			SharedPreferences settings = getSharedPreferences(SETTINGS_FILENAME, MODE_PRIVATE);
			final Account[] accounts = AccountManager.get(_mMainActivity).getAccountsByType("com.google");
			String accountname = settings.getString("preferred_account_name", "--");
			if(accountname.equals("--"))  { // no preferred account declared
				return false;
			}  else  {  //we already have a preferred account declared
				for(Account a : accounts)  {
					if(a.name.equals(accountname))
						sAccount = a;
				}
				getCookie();
				return true;
			}
		}
		
		private void getCookie()  {
			boolean isNewCookieNeeded;
			SharedPreferences settings = getSharedPreferences(SETTINGS_FILENAME, MODE_PRIVATE);
			String cookieValue = settings.getString("cookie_value", "--");
			BasicClientCookie cookie = new BasicClientCookie("SACSID", cookieValue);
			isNewCookieNeeded = cookieValue.equals("--");
			if(!isNewCookieNeeded)  {
				try {
					String cookieExpiryDate = settings.getString("cookie_expiry", "--");
					cookie.setExpiryDate(new SimpleDateFormat("yyyy.MM.dd-HH:mm.ss", Locale.US).parse(cookieExpiryDate));
					isNewCookieNeeded = cookie.isExpired(new Date());
				} catch (ParseException e) { // Should never happen
					isNewCookieNeeded = true;
				}
			}
			if(!isNewCookieNeeded)  { // we already had a non-expired cookie
				mCookie = cookie;
			}  else  { // we need to fetch a new cookie
				publishProgress(10); // show "connecting" dialog (please see onProgressUpdate)
				final AccountManager manager = AccountManager.get(getApplicationContext());
				
				Thread t = new TokenGetterThread(manager, MainActivity.this);
				t.start();
				try {
					t.join();
					t = new CookieGetterThread();
					t.start();
					t.join();
				} catch (InterruptedException e) {
					Log.e(APP_TAG, "Attempt to reach the server was interrupted.");
					e.printStackTrace();
				}
				
//				if(mCookie != null)  {
//					Editor editor = settings.edit();
//					editor.putString("cookie_value", mCookie.getValue());
//					editor.putString("cookie_expiry", 
//							new SimpleDateFormat("yyyy.MM.dd-HH:mm.ss", Locale.US).format(mCookie.getExpiryDate()));
//					editor.commit();
//				}  else  {
//					_mIsConnectedToServer = false;
//				}
				if(mCookie == null) { _mIsConnectedToServer = false; }
			}
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			if(progress[0] == 10)  {
				 (findViewById(R.id.imageView)).setVisibility(ImageView.INVISIBLE);
				 (findViewById(R.id.buttonEncrypt)).setEnabled(false);
				 (findViewById(R.id.buttonDecrypt)).setEnabled(false);
				 (findViewById(R.id.buttonMyPics)).setEnabled(false);
				 (findViewById(R.id.shareButton)).setVisibility(View.INVISIBLE);
				 (findViewById(R.id.emailTextView)).setVisibility(View.INVISIBLE);
				 (findViewById(R.id.logoutButton)).setVisibility(View.INVISIBLE);
		         _mProgressDialog.setTitle(R.string.authenticatingDialog);
		         _mProgressDialog.setMessage(_mMainActivity.getString(R.string.pleaseWaitDialog));
		         _mProgressDialog.setCancelable(false);
		         _mProgressDialog.setIndeterminate(true);
		         _mProgressDialog.show();
			}
	     }
	}
	
	private class CookieGetterThread extends Thread  {
		public void run()  {
			try {
				mCookie = getAuthCookie(sToken);
				// if anything goes wrong, it will be handled by getCookie()
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class TokenGetterThread extends Thread  {
		private AccountManager _mManager;
		private Activity _mActivity;
		
		public TokenGetterThread(AccountManager manager, Activity activity)  {
			this._mManager = manager;
			this._mActivity = activity;
		}
		
		public void run() {
			try {
				AccountManagerFuture<Bundle> future = _mManager.getAuthToken(sAccount, "ah", null, _mActivity, null, null);
				Bundle bundle = future.getResult();
				String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				sToken = token;
				// This token is NOT NECESSARILY still valid. That will be determined
				// by the server; we manage that as an authentication error.
				// if anything goes wrong, it will be handled by getCookie()
			} catch (OperationCanceledException cancelEx) {
				cancelEx.printStackTrace();
			} catch (AuthenticatorException authEx) {
				authEx.printStackTrace();
			} catch (IOException ioEx) {
				ioEx.printStackTrace();
			}
		}
	}
	
	public static class CookieRefresherThread extends Thread  {
		/** this class handles updating tokens.
		 * Authentication tokens assigned to an account expire after a set amount of time,
		 * and they cannot be directly queried on whether or not they are still good.
		 * Thus, since asking the server for a new token can be slow, and it is thus not
		 * a good idea to do this every time the token is used, we only do it whenever
		 * the server states user is not authenticated.
		 * This is only retried once, since the problem may be an actual authentication
		 * problem, which would otherwise trigger an infinite loop (authentication failed,
		 * try to get new token, retry, authentication fails again...)
		 */
		
		private AccountManager _mManager;
		private MainActivity _mMainActivity;
		
		public CookieRefresherThread(AccountManager manager, MainActivity activity)  {
			this._mManager = manager;
			this._mMainActivity = activity;
		}
		
		public void run() {
			try {
				AccountManagerFuture<Bundle> future = _mManager.getAuthToken(sAccount, "ah", null, _mMainActivity, null, null);
				Bundle bundle = future.getResult();
				String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				_mManager.invalidateAuthToken("com.google", token);

				future = _mManager.getAuthToken(sAccount, "ah", null, _mMainActivity, null, null);				
				bundle = future.getResult();  //blocking
				token = bundle.getString(AccountManager.KEY_AUTHTOKEN);

				sToken = token;
				mCookie = _mMainActivity.getAuthCookie(sToken);
				
			} catch (OperationCanceledException cancelEx) {
				cancelEx.printStackTrace();
			} catch (AuthenticatorException authEx) {
				authEx.printStackTrace();
			} catch (IOException ioEx) {
				ioEx.printStackTrace();
			}
		}
	}
	
	protected Cookie getAuthCookie(String authToken) throws ClientProtocolException, IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		Cookie retCookie = null;
		String cookieUrl = BASEURL + "/_ah/login?continue=" + URLEncoder.encode(BASEURL, "UTF-8") + "&auth=" + URLEncoder.encode(authToken, "UTF-8");
		HttpGet httpget = new HttpGet(cookieUrl);
		HttpResponse response = httpClient.execute(httpget);
		if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK
				|| response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_NO_CONTENT) {

			if (httpClient.getCookieStore().getCookies().size() > 0) {
				retCookie = httpClient.getCookieStore().getCookies().get(0);
			}
		}
		
		// store the cookie
		SharedPreferences settings = getSharedPreferences(SETTINGS_FILENAME, MODE_PRIVATE);
		Editor editor = settings.edit();
		editor.putString("cookie_value", retCookie.getValue());
		editor.putString("cookie_expiry", 
				new SimpleDateFormat("yyyy.MM.dd-HH:mm.ss", Locale.US).format(retCookie.getExpiryDate()));
		editor.commit();
		
		return retCookie;
	}
}
