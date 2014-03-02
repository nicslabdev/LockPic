package es.uma.lcc.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * Auxiliary class containing some operations of IO which some classes may need
 */

public class LockPicIO {
	
	/** Returns a float which, when multiplied by the original width and height,
	 * will give the best fit to the bounds specified by the required size
	 * i.e: we have a 1200x400 image to be painted in a 800x600 display.
	 * This function will return 2/3, so that (1200, 400)*2/3 = (800, 267)
	 * will fit exactly onscreen.
	 */
	public static float getBestFitKeepingAspectRatio(int reqWidth, int reqHeight,
					int originalWidth, int originalHeight)
	{
		float xRequiredAdjFactor = (float)reqWidth  / originalWidth;
		float yRequiredAdjFactor = (float)reqHeight / originalHeight;
		return Math.min(xRequiredAdjFactor, yRequiredAdjFactor);
	}
	
	/**
	 *   This function searches sequentially from the beginning of a JPEG file
	 *   until it finds either a comment header (which will be read and returned),
	 *   or the SOS marker (start of image data), in which case it is assumed no
	 *   comment data is included.
	 *   
	 *   Maybe at some point this function should be optimized (several bytes can
	 *   be skipped, taking into account marker lengths), and improved to be able
	 *   to recognize comments placed after the image data, but as of now, none
	 *   of those points is a priority. (8/2013)
	 */
	public static String getJpegComment(String filename) throws IOException {
		
		FileInputStream in = new FileInputStream(filename);
		int i, i2;
		boolean finished = false;
		String ret = null;
		i = in.read();
		i2 = in.read();
		if(i != 255 || i2 != 216) {
			// It does not start with 0xFFD8 (SOI). Not a valid JPEG!
			// Otherwise we might find unpredictable behavior when not
			// dealing with a JPEG file.
			finished = true;
		}
		while(!finished)  {// 0xDA (SOS)	0xFE (COM)
			if(i != 255 || (i2 != 218 ) && (i2 != 254))  {
				i = i2;
				i2 = in.read(); // read next byte
			}  else if( i2 == 218 )  { // SOS: no comment
				finished = true;
			}  else  {
				finished = true;
				i = in.read();
				i2 = in.read();
				int length = (i*256) + i2 - 2;
				byte[] comm = new byte[length];
				in.read(comm);
				ret = new String(comm);
			}
		}
		in.close();
		return ret;
	}
	
	/** Extracts the file path for an intent coming from the gallery **/
	public static String getPathFromIntent(Activity act, Intent imageReturnedIntent)
    {
		try{ // Intents from Android OS are caught this way
	    	Uri selectedImage = imageReturnedIntent.getData();
	        String[] filePathColumn = {MediaStore.Images.Media.DATA};
	        Cursor cursor = act.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
	        cursor.moveToFirst();
	        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	        String path = cursor.getString(columnIndex);
	        cursor.close();
	        return path;
		} catch(NullPointerException npex)  { // thrown from Cursor.moveToFirst
			String path = imageReturnedIntent.getData().getPath();
			// other Intent formats (as from file managers)
			return (new File(path).exists() ? path : null);
		}
    }
}
