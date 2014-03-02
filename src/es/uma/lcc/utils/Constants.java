package es.uma.lcc.utils;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * Constants to ensure consistency in communication between client and server,
 * and between activities on the client side.
 * 
 * Expected to be the same as keyserver.Constants
 */

public class Constants {

	 public static final String BASEURL = "https://scrambler-keyserver.appspot.com",
			 				    SERVERURL = BASEURL + "/keyserver";
	 public static final String APP_TAG = "LockPic";
	 public static final String SETTINGS_FILENAME = "settings",
			 					CONTACTS_FILENAME = "contacts";
	 public static final String ENCRYPTED_FILE_PREFIX = "LP_";
	 
	// codes for child activities
	 public static final int ACTIVITY_SELECTOR = 1,
						     ACTIVITY_PICK_IMAGE_ENC = 2,
						     ACTIVITY_PICK_IMAGE_DEC = 3,
						     ACTIVITY_VIEW_MY_PICTURES = 4,
						     ACTIVITY_PICTURE_DETAILS = 5;
	 
	 // MAX_REGIONS_ALLOWED must be the same as in NativeJpegEncoder.DrawView
	 // MAX_PERMISSIONS_RETURNED_PER_PICTURE is a soft limit, used only in PICTUREDETAILS mode.
	 public static final int MAX_PERMISSIONS_RETURNED_PER_PICTURE = 200,
							 MAX_REGIONS_ALLOWED = 15;
							 
	 public static final String CURRENT_VERSION = "1.0";
	 public static final String MISC_DATE_FORMAT = "yyyy.MM.dd-HH:mm.ss";
	

	 // Query string parameters
	 public static final String QUERYSTRING_ACTION = "action", // see URL action parameters
								QUERYSTRING_PICTUREID = "requestedId"; // ID of requested picture
	
	 // URL action parameters
	 public static final String ACTION_MYPICTURES = "viewMyPictures",  //view all user's pictures
								ACTION_ONESTEPUPLOAD = "oneStepUpload", //encrypt a picture
								ACTION_PICTUREDETAILS = "pictureDetails", //view permissions on a picture
								ACTION_READPERMISSIONS = "requestInfo", //decrypt a picture
								ACTION_UPDATE = "update";  //modify permissions on a picture
	
	 // Database kind names
	 public static final String DB_KIND_PERMISSION = "Permission", // permission line (region-user pair)
								DB_KIND_PICTURE = "Picture",
								DB_KIND_USER = "User";
	
	 // Database field names. Named DB_<KIND>_<PARAMETER>
	 public static final String DB_PERMISSION_H_START = "horizStart",
								DB_PERMISSION_H_END = "horizEnd",
								DB_PERMISSION_V_START = "vertStart",
								DB_PERMISSION_V_END = "vertEnd",
								DB_PERMISSION_USERNAME = "username",
								DB_PICTURE_CREATED = "created",
								DB_PICTURE_FILENAME = "filename",
								DB_PICTURE_HEIGHT = "height",
								DB_PICTURE_WIDTH = "width",
								DB_USER_KEY = "key";
	
	 // JSON field names. Most of these are field names; those which are field values are named
	 // JSON_<FIELDNAME>_<VALUENAME>; e.g. JSON_RESULT can take values JSON_RESULT_OK or JSON_RESULT_ERROR
	 public static final String JSON_COORDINATES = "coordinates",
								JSON_DATECREATED = "date",
								JSON_FILENAME = "filename",
								JSON_HSTART = "horizStart",
								JSON_HEND = "horizEnd",
								JSON_IMGHEIGHT = "height",
								JSON_IMGWIDTH = "width",
								JSON_ISAUTHERROR = "isAuthError",
								JSON_KEY = "key",
								JSON_PARENT = "parent",
								JSON_PERMISSIONID="id",
								JSON_PICTUREID = "pictureId",
								JSON_PROTOCOLVERSION = "protocolVersion",
								JSON_REASON = "reason",		
								JSON_RESULT = "result",
								JSON_RESULT_OK = "ok",
								JSON_RESULT_ERROR = "error",
								JSON_TYPE = "type",
								JSON_UPDATEACTION = "action",
								JSON_UPDATEACTION_ADD = "add",
								JSON_UPDATEACTION_DELETE = "delete",
								JSON_USERNAME = "username",
								JSON_VSTART = "vertStart",
								JSON_VEND = "vertEnd";
	
	 public static final String JSON_DEFAULTOK = 
			"{\"" + JSON_PROTOCOLVERSION + "\": \"" + CURRENT_VERSION + "\", " +
			 "\"" + JSON_RESULT + "\": \"" + JSON_RESULT_OK + "\"}";
			// shortcut for {"protocolVersion": <version>, "result": "ok"}
}
