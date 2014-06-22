package harlequinmettle.gaero;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

public class DatastoreAccessGAERo {

	static final String PROPERTY_BLOB_OB = "Object Bytes";
	static final String DIVIDEND_BLOBS = "DIV_BLOBS";
	static final Entity BYTE_COUNT = new Entity("DEBUG", "BYTES");
	static final String DATES_ARRAY = "DATESARRAY";
	static final String DIVS_ARRAY = "DIVSARRAY";
	static final String DEBUG_PROPERTY = "info";
	static final String TIME_PROPERTY = "time (ms)";
	static final String LAST_POST_PROPERTY = "posted at";
	static final DatastoreService DATASTORE = DatastoreServiceFactory
			.getDatastoreService();
	private static int counter = 0;

 
	public static long getPreviousByteCount() {
		if (isNewGAEDay())
			return 0;
		long byteCount = 0l;
		Key getter = BYTE_COUNT.getKey();// KeyFactory.createKey(limited,
											// uniqueKey);

		Entity bytesEntity = null;
		try {
			bytesEntity = DATASTORE.get(getter);
			if (bytesEntity == null)
				return 0l;
			byteCount = Long.parseLong(((String) bytesEntity
					.getProperty(DEBUG_PROPERTY)).split(" ")[0]) * 1000;
		} catch (Exception e) {
			String name = new Object() {
			}.getClass().getEnclosingMethod().getName();
			DSLogGAERo.log(name + "   " + e.toString());

		}

		return byteCount;

	}

	private static boolean isNewGAEDay() {
		Date date = new Date(); // given date
		Calendar calendar = GregorianCalendar.getInstance(); // creates a new
																// calendar
																// instance
		calendar.setTime(date); // assigns calendar to given date

		int hour = calendar.get(Calendar.HOUR_OF_DAY)
				- (calendar.get(Calendar.ZONE_OFFSET) + calendar
						.get(Calendar.DST_OFFSET)) / (60 * 60 * 1000);

		return (hour > 8 && hour <= 10);
	}

 
	public static float[][] get2DFloatFromDatastore(String megaMaxBlobName) {
 
		ByteArrayInputStream bis = null;
		ObjectInput in = null;
		float[][] myArray = null;
		try {
			Key getter = KeyFactory.createKey(DIVIDEND_BLOBS, megaMaxBlobName);
 
			
			Entity e = DATASTORE.get(getter);  
			
			Blob data = (Blob) e.getProperty(PROPERTY_BLOB_OB); 

			bis = new ByteArrayInputStream(data.getBytes()); 
			

			in = new ObjectInputStream(bis);  

			myArray = (float[][]) in.readObject(); 
			

		} catch ( Exception ex) {
			String name = new Object() {
			}.getClass().getEnclosingMethod().getName();
			DSLogGAERo.log(name + "   ***   " + ex.toString());

		} finally {
			try {
				bis.close();
				in.close();
			} catch (Exception exep) {
				String name = new Object() {
				}.getClass().getEnclosingMethod().getName();
				DSLogGAERo.log(name + "   " + exep.toString());
			}
		}
  
		return myArray;
	}

	// overwrites any previously saved data
	public static boolean object_asBlob_toDatastore(String megaMaxBlobName,
			Object saveData) {

		byte[] d = convertToSerialBytes(saveData);
		// over 1Megabyte limit? dont try to save
		if (d.length > 1000000)
			return false;

		Entity one = new Entity(DIVIDEND_BLOBS, megaMaxBlobName);

		Key getter = one.getKey();// KeyFactory.createKey(limited, uniqueKey);

		Blob serial = new Blob(d);

		one.setProperty(PROPERTY_BLOB_OB, serial);
		addTimePostedProperties(one);
		DATASTORE.put(one);
		return true;
	}

	public static byte[] convertToSerialBytes(Object yourObject) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] yourBytes = null;

		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(yourObject);
			yourBytes = bos.toByteArray();

		} catch (IOException ioe) {
			String name = new Object() {
			}.getClass().getEnclosingMethod().getName();
			DSLogGAERo.log(name + "   " + ioe.toString());
		} finally {
			try {
				out.close();
				bos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return yourBytes;
	}

	static void addToDatastoreDebugLogger(String resultData) {
		String timeDate =    Backend.DATE.format(
						new Date(System.currentTimeMillis())).toString();
		Entity one = new Entity("DEBUG", timeDate);

		Key getter = KeyFactory.createKey("DEBUG", timeDate);
		//
		// try {
		// Entity two = DATASTORE.get(getter);
		// if (two != null)
		// one = two;
		// } catch (EntityNotFoundException e) {
		//
		// String name = new
		// Object(){}.getClass().getEnclosingMethod().getName();
		// DSLog.log(name+"   "+e.toString());
		// }

		one.setProperty(DEBUG_PROPERTY, new Text(resultData.toString()));
addTimePostedProperties(one);

		DATASTORE.put(one);
		// Key getter = KeyFactory.createKey(commonRoot,uniqueKey);
	}

	private static void addTimePostedProperties(Entity one) {
		one.setProperty(TIME_PROPERTY, ""+System.currentTimeMillis());
		one.setProperty(LAST_POST_PROPERTY,  Backend.DATE.format(
				new Date(System.currentTimeMillis())).toString());
	}

}
