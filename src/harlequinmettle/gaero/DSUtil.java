package harlequinmettle.gaero;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.TreeMap;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class DSUtil {
	
	


	static final String P_FLOAT_ARRAY = "Float Array";
	static final String NASDAQ_FUNDAMENTALS = "DB_Q_FUND";
	static final String NASDAQ_TECHNICALS = "DB_Q_TECH";
	static final String NYSE_FUNDAMENTALS = "DB_Y_FUND";
	static final String NYSE_TECHNICALS = "DB_Y_TECH";

	static final String[] Database_KINDS = { NASDAQ_FUNDAMENTALS,NASDAQ_TECHNICALS ,NYSE_FUNDAMENTALS,NYSE_TECHNICALS };
	
	
	
	static final String NAS = "NASDAQ_MICRO";
	static final String NY = "NYSE_MICRO";
	static final String NASXL = "NASDAQ";
	static final String NYXL = "NYSE";
	static final String SAVE = "SAVE";
	static final String BLOBS = "BLOBSTORE";
	static final String DATA = "DATA";
	static final String TEMPBLOBS = "TEMPBLOBS";

	static final String THREAD = "THREAD_INFO";
	static final String STATE_INFO = "THREAD_STATE";

	static final String DATABASE = "THE_DATABASE";
	static final String FROM = "PAGE ACCESS";
	static final String DEBUG = "DEBUG";
	static final String MEM_LOG = "MEMORY";
	static final String BLOB_BUG = "buggers";
	static final String BLOB_PATH = "blobkeypath";
	static final String P_STRING_ARRAY = "String Array";
	static final String P_BLOB_OB = "OBJECT BYTES";
	
	static final String[] DS_KINDS = { NAS, NY, THREAD, FROM, DEBUG,
			STATE_INFO, NASXL, NYXL, SAVE, BLOBS, DATA ,TEMPBLOBS,DATABASE ,   };
	//static final int DS_MAX = 8;
	// determines which thread action to take
	static final Entity THREAD_STATE = new Entity(STATE_INFO, "threadAction");
	static final Entity LAST_DATE_lg = new Entity(STATE_INFO, "dayNumberLG");
	static final Entity LAST_DATE_sm = new Entity(STATE_INFO, "dayNumberSM");
	static final Entity TICKER = new Entity(STATE_INFO, "TICKER");
	// determines either nasdaq or nyse to load into memcach
	//static final Entity THREAD_KIND = new Entity(STATE_INFO, "fromIndex");
	// which memstore collection date (property) to load into memcache
	//static final Entity THREAD_PROPERTY = new Entity(STATE_INFO, "propertyID");
	// /////////////

	
	
	
	//public static final Entity savePt = new Entity(SAVE, "lastTicker");
	// public static final Entity saveSet = new Entity(SAVE, "firstSet");

	static DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
	
	static MemcacheService syncCache = MemcacheServiceFactory
			.getMemcacheService(); 

	public static void loadMemcacheFromDatastore(String index, String property) {
	 int counter = 0;
		Query query = new Query(index);
		for (Entity e : DSUtil.ds.prepare(query).asIterable()) {

			String memData = (String) DSUtil.syncCache.get(e.getKey());
			if (memData == null) {
				String output = e.getKey().getName()
						+ "^"
						+ ((Text) e.getProperty(property.trim())).getValue()
								.toString() + "<br/>";

				// Key key = KeyFactory.createKey(index, e.getKey().getName());
				DSUtil.syncCache.put(e.getKey(), output);
				counter++;
				if (counter % 200 == 0)
					try {
						Thread.sleep(33);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
			}
		}
	}

	public static void printDatastore(PrintWriter w, String kind, String field)
			throws IOException {

		Query query = new Query(kind);

		FetchOptions fetch_options = FetchOptions.Builder.withPrefetchSize(900)
				.chunkSize(900);

		for (Entity e : DSUtil.ds.prepare(query).asQueryResultList(
				fetch_options)) {
			String memData = (String) DSUtil.syncCache.get(e.getKey());
//// STRINGS CAUSE ERROR TRYING TO PRINT BECAUSE CASTING TO TEXT!!!!!!!!!!!!!!!!!!!!!
			if (memData == null) {
				try {
					memData = e.getKey().getName()
							+ "^"
							+ ((Text) e.getProperty(field.trim())).getValue()
									.toString() + "<br/>";

				} catch (NullPointerException npe) {

				}
			}
			if (memData != null)
				w.println(memData);
		}

	}

	public static String getDataFromDatastore(String index, String property) {
		StringBuilder b = new StringBuilder();

		Query query = new Query(index);
		for (Entity e : DSUtil.ds.prepare(query).asIterable()) {

			String output = e.getKey().getName()
					+ "^"
					+ ((Text) e.getProperty(property.trim())).getValue()
							.toString() + "\n";
			b.append(output);

			Thread.yield();

		}

		return b.toString();
	}

	static void addToDatastoreAsText(int dsAccess, String uniqueKey,
			String propertyName, String resultData) {

		Entity one = new Entity(DS_KINDS[dsAccess], uniqueKey);

		Key getter = KeyFactory.createKey(DS_KINDS[dsAccess], uniqueKey);

		try {
			Entity two = ds.get(getter);
			if (two != null)
				one = two;
		} catch (EntityNotFoundException e) {

		}

		one.setProperty(propertyName, new Text(resultData.toString()));

		ds.put(one);
		// Key getter = KeyFactory.createKey(commonRoot,uniqueKey);
	}

	static void addToDatastoreAsText(String limited, String uniqueKey,
			String propertyName, String resultData) {

		Entity one = new Entity(limited, uniqueKey);

		Key getter = KeyFactory.createKey(limited, uniqueKey);

		try {
			Entity two = ds.get(getter);
			if (two != null)
				one = two;
		} catch (EntityNotFoundException e) {

		}

		one.setProperty(propertyName, new Text(resultData.toString()));

		ds.put(one);
		// Key getter = KeyFactory.createKey(commonRoot,uniqueKey);
	}

	static void addToDatastoreAsString(int dsAccess, String uniqueKey,
			String propertyName, String resultData) {

		Entity one = new Entity(DS_KINDS[dsAccess], uniqueKey);

		Key getter = KeyFactory.createKey(DS_KINDS[dsAccess], uniqueKey);

		try {
			Entity two = ds.get(getter);
			if (two != null)
				one = two;
		} catch (EntityNotFoundException e) {

		}

		one.setProperty(propertyName, (resultData));

		ds.put(one);
	}

	static void addToDatastoreAsString(String limited, String uniqueKey,
			String propertyName, String resultData) {

		Entity one = new Entity(limited, uniqueKey);

		Key getter = KeyFactory.createKey(limited, uniqueKey);

		try {
			Entity two = ds.get(getter);
			if (two != null)
				one = two;
		} catch (EntityNotFoundException e) {

		}

		one.setProperty(propertyName, (resultData));

		ds.put(one);
	}

	static void addToDatastoreAsString(Entity one, String propertyName,
			String resultData) {

		Key getter = one.getKey();

		try {
			one = ds.get(getter);
		} catch (EntityNotFoundException e) {
		}
		one.setProperty(propertyName, (resultData));

		ds.put(one);
	}

	static void deleteDatastore(String deleteFromRoot) {

		Query query = new Query(deleteFromRoot);
		for (Entity e : ds.prepare(query).asIterable()) {
			ds.delete(e.getKey());
		}

	}

	public static void writeToBlobStore(String[] yy, String title) {
		// 1
		// DSUtil.addToDatastoreAsString(DSUtil.DEBUG, "0 WRITING TO BLOBSTORE",
		// BLOB_NAME, "BLA");
		try {
			// Get a file service
			FileService fileService = FileServiceFactory.getFileService();

			// 2
			// DSUtil.addToDatastoreAsString(DSUtil.DEBUG, "1 GOT FILESERVICE",
			// BLOB_NAME, fileService.toString());
			// Create a new Blob file with mime-type "text/plain"
			// /AppEngineFile file =
			// fileService.createNewBlobFile("text/plain");
			AppEngineFile file = fileService.createNewBlobFile("text/plain",
					title);

			// Open a channel to write to it
			boolean lock = true;
			FileWriteChannel writeChannel = fileService.openWriteChannel(file,
					lock);
			// BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(new
			// BlobKey( "myblob"));

			// 3
			// DSUtil.addToDatastoreAsString(DSUtil.DEBUG, "2 got filechannel",
			// BLOB_NAME, writeChannel.toString());
			// Different standard Java ways of writing to the channel
			// are possible. Here we use a PrintWriter:
			// PrintWriter out = new
			// PrintWriter(Channels.newWriter(writeChannel, "UTF8"));

			// DSUtil.addToDatastoreAsString(DSUtil.DEBUG, "3 GOT printwriter",
			// BLOB_NAME, writeChannel.toString());
			// for (String s : yy) out.print(ThreadUtil.preloader.get(s));
			for (String s : yy)
				writeChannel.write(ByteBuffer.wrap(Backend.completeResults.get(s)
						.getBytes()));
			// DSUtil.addToDatastoreAsString(DSUtil.DEBUG, "4 data written",
			// BLOB_NAME,"items written:"+ yy.length);

			// Now finalize
			writeChannel.closeFinally();

			// DSUtil.addToDatastoreAsString(DSUtil.DEBUG, "5 channel closed",
			// BLOB_NAME, writeChannel.toString());

			BlobKey bk = fileService.getBlobKey(file);
			Entity BLOBENTITY = new Entity(BLOBS, title);
			BLOBENTITY.setProperty(BLOB_PATH, bk);
			DSUtil.ds.put(BLOBENTITY);

			// DSUtil.addToDatastoreAsString(DSUtil.BLOBS, title, BLOB_NAME,
			// path);

		} catch (Exception ioe) {
			ioe.printStackTrace();
		}

	}

	// FIX SO THAT READS ALL LINES FROM FILE AND SENDS IN AN EMAIL
	// PROBLEM SAVING FILE PATH!!!!!!! !!!!!! !!!!!
	public static void emailDataFromBlobstore(String commonName) {

		Entity BLOBENTITY = new Entity(BLOBS, commonName);
		// BLOBENTITY.setProperty("blob path", bk);
		BlobKey blobKey = null;
		try {
			blobKey = (BlobKey) DSUtil.ds.get(BLOBENTITY.getKey()).getProperty(
					BLOB_PATH);
		} catch (EntityNotFoundException e1) {
			e1.printStackTrace();
		}

		BlobstoreService blobStoreService = BlobstoreServiceFactory
				.getBlobstoreService();
		// Start reading
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		long inxStart = 0;
		long inxEnd = 1024;
		boolean flag = false;

		do {
			try {
				byte[] b = blobStoreService
						.fetchData(blobKey, inxStart, inxEnd);
				out.write(b);

				if (b.length < 1024)
					flag = true;

				inxStart = inxEnd + 1;
				inxEnd += 1025;

			} catch (Exception e) {
				flag = true;
			}

		} while (!flag);

		byte[] filebytes = out.toByteArray();

		String myString = new String(filebytes);
 
		// readChannel.close();
		EmailUtil.sendDataByEmail(myString, commonName);

	}

	 

	public static void saveAsFloatArray3D(String dsID, String title,
			float[][][] saveData) {

		Entity one = new Entity(dsID, title);

		Key getter = one.getKey();// KeyFactory.createKey(limited, uniqueKey);

		try {
			Entity two = ds.get(getter);
			if (two != null)
				one = two;
		} catch (EntityNotFoundException e) {

		}

		Blob serial = new Blob(DSBlobs.convertToSerialBytes(saveData));
		one.setProperty(P_FLOAT_ARRAY, serial);
		ds.put(one);
	}

	public static void saveAsFloatArray2D(String data2, String title,
			float[][] saveData) {

		Entity one = new Entity(data2, title);

		Key getter = one.getKey();// KeyFactory.createKey(limited, uniqueKey);

		try {
			Entity two = ds.get(getter);
			if (two != null)
				one = two;
		} catch (EntityNotFoundException e) {

		}
		Blob serial = new Blob(DSBlobs.convertToSerialBytes(saveData));
		one.setProperty(P_FLOAT_ARRAY, serial);

		ds.put(one);
	}

	public static Blob getBlobByName(String fromEntity,String blobName) {
		Blob data = null;
		Entity one = new Entity(fromEntity, blobName);

		Key getter = one.getKey();// KeyFactory.createKey(limited, uniqueKey);

		try {
			Entity two = ds.get(getter);
			if (two != null)
				one = two;
		} catch (EntityNotFoundException e) {

		}
		data = (Blob) one.getProperty(P_FLOAT_ARRAY);

		return data;
	}

	public TreeMap<Float,float[][][]>   loadTechnicalDBBlobs() {
		 TreeMap<Float,float[][][]> techies = new TreeMap<Float,float[][][]>();
			 Query query = new Query(DSUtil.NASDAQ_TECHNICALS);
			for (Entity e : ds.prepare(query).asIterable()) {
			 

				Float date =   Float.parseFloat( e.getKey().getName().split("_")[2]);

				Blob data = (Blob) e.getProperty(P_FLOAT_ARRAY);
								 
				
				
			}
			return techies;
	}


	public static void loadFundamentalDBBlobs() {
		// TODO Auto-generated method stub
		
	}

}
