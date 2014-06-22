package harlequinmettle.gaero;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.google.appengine.api.LifecycleManager;
import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.datastore.EntityNotFoundException;

public class Backend implements Qi, Yi // NASDAQ, NYSE //
{   
	

	public static final boolean BIG_ONE = false;
	public static final boolean GIG = false;
	// 1 -- 128MB; 600MHz; 0.08/hr //
	// 2 -- 256MB; 1.2Ghz; 0.16/hr //
	// or 4 -- 512MB; 2.4Ghz; 32/hr //
	public static final int BACKEND = 4;
	public static final int THREAD_COUNT = 12; 
	//1Gb only with BACKEND = 4
	public static  final String BACKEND_EXTRA;//"1gig";

	public static  final int EXTRA_MEMORY ;//400;
	//public static final int EXTRA_MEMORY = 0;
	public static final int MB_BUFFER = 10+ 5*BACKEND;
	public static   int MEM_CLASS  ;// b2 --> 250 b4
	
	// Project->Properties->Google->App Engine->|Application ID|
  public static final String ApplicationID = "backend-test-app";
  //public static final String ApplicationID = "cloudapp22x";
  //public static final String ApplicationID = "cloudcomputer99";
  //public static final String ApplicationID = "financialdatacollector";
 
	public static volatile int threadStarts = 0;
	public static AtomicLong time = new AtomicLong(System.currentTimeMillis());
	public static AtomicInteger iter = new AtomicInteger(0);
	public static String fileTitleQ = "date not setx";
	public static String fileTitleY = "date not setxx";
	private final static AtomicBoolean emailSent = new AtomicBoolean(false);

	public static final SimpleDateFormat DATE = new SimpleDateFormat(
			"MMM-dd' at 'HH:mm:ss");

	// used to identify individual thread progress
	private final static ConcurrentHashMap<String, AtomicInteger> vars = new ConcurrentHashMap<String, AtomicInteger>();

	final static ConcurrentHashMap<String, TreeMap<String, String>> saveProgress = new ConcurrentHashMap<String, TreeMap<String, String>>();

	final static ConcurrentHashMap<String, String> completeResults = new ConcurrentHashMap<String, String>();

	private final static ConcurrentHashMap<String, Integer> memoryMonitor = new ConcurrentHashMap<String, Integer>();

	private final static ConcurrentLinkedQueue<String> tickers = new ConcurrentLinkedQueue<String>();
	
	public final static ConcurrentLinkedQueue<String> retryTickers = new ConcurrentLinkedQueue<String>();

	public final static ConcurrentHashMap<String, String[]> previousData = new ConcurrentHashMap<String, String[]>();
	
	public final static ConcurrentHashMap<String, Integer> optimizationIndex = new ConcurrentHashMap<String, Integer>();

	private final static int BATCH = 20;
	public volatile static double big_Count = 0;

	static {	 
		if(GIG){
			BACKEND_EXTRA = "1gig";

			  EXTRA_MEMORY =  400;
	}else{

		  EXTRA_MEMORY =  0;
		BACKEND_EXTRA = "";
	}
		MEM_CLASS = 125 * BACKEND-MB_BUFFER+EXTRA_MEMORY;// b2 --> 250 b4

		//DSBlobs.postMemoryData("b41gMemory>>>>(---"+MEM_CLASS+"---)");
		int index = 0;
		for(String s: QQ){
			optimizationIndex.put(s, index++);
		}
		for(String s: YY){
			optimizationIndex.put(s, index++);
		}
	}

	private static boolean isNewWeek() {

		float lastCompletionDate = lastDate();

		int currently = (int) (System.currentTimeMillis() / 1000 / 3600 / 24);

		return currently > 4 + lastCompletionDate;

	}

	private static float lastDate() {
		try {
			if (BIG_ONE) {
				return Float
						.parseFloat((String) (DSUtil.ds.get(DSUtil.LAST_DATE_lg
								.getKey()).getProperty("data")));
			} else {
				return Float
						.parseFloat((String) (DSUtil.ds.get(DSUtil.LAST_DATE_sm
								.getKey()).getProperty("data")));
			}
		} catch (NumberFormatException e) {
		} catch (EntityNotFoundException e) {
		}
		return 15000;
	}

	public static void fillTickers() {
		tickers.addAll(Arrays.asList(QQ));
		tickers.addAll(Arrays.asList(YY));
	}

	private static void resetTickersLessSavedData() {
		// only refill if tickers has no more
		if (!tickers.isEmpty())
			return;
		fillTickers();
		// restores all TreeMap Blobs under E_TEMP_BLOBS
		TreeMap<String, String> dataRestore = DSBlobs.restoreProgress();

		completeResults.putAll(dataRestore);

		for (TreeMap<String, String> local : saveProgress.values()) {
			completeResults.putAll(local);
		}

		tickers.removeAll(completeResults.keySet());
	}

	static void runCollectionInBackend() {
		// if we are making sufficient progress or it has not been a week since
		// last time dont proceed starting a new thread
		// if (!isNewWeek() || makingProgress())
		if (!isNewWeek()) {
			// DSUtil.deleteDatastore(DSUtil.MEM_LOG);
			// DSBlobs.deleteTempBlobs();
			return;
		}
		if (vars.size() == 0)
			time.set(System.currentTimeMillis());
		final String ID = ""
				+ (Long.parseLong(""
						+ (Long.MAX_VALUE - System.currentTimeMillis())));

		fileTitleQ = fileTitle("nas");
		fileTitleY = fileTitle("ny");

		if (BIG_ONE) {
			fileTitleQ = "BIG_" + fileTitleQ;
			fileTitleY = "BIG_" + fileTitleY;
		}

		memoryMonitor.put(ID,
				(int) (Runtime.getRuntime().totalMemory() / 1000000)
						- (int) (Runtime.getRuntime().freeMemory() / 1000000));

//		DSBlobs.postMemoryDataId(ID + "(>--   " + getTotalMemoryUsed()
//				+ "M  --<)");

		// DEFINE A THREAD
		Thread thread = ThreadManager.createBackgroundThread(new Runnable() {

			public void run() {
				// add a treemap for this thread
				saveProgress.put(ID, new TreeMap<String, String>());
				// add an atomic counter for this thread
				vars.put(ID, new AtomicInteger(1));
				// record starting time
				DSUtil.addToDatastoreAsString(DSUtil.THREAD, ID, " startTime",
						DATE.format(new Date()) + "  (" + tickers.size() + ")");
				// completion test: preloader.size==QQ.length+YY.length (first
				// thread to finish perform blobstore and email files)
				while (!isComplete(ID)) {
					if (iter.getAndIncrement() % 50 == 0)
						DSBlobs.logTotalMemory();
					int memory = getTotalMemoryUsed();
					if (memory > MEM_CLASS && vars.size() > 1) {

						logThreadComplete(ID, " X ", memory);

						saveCurrentResultsToDS(ID, -1
								* vars.remove(ID).intValue());
						memoryMonitor.remove(ID);
						return;
					}
					updateQueIfNecessary();

					String ticker = tickers.poll();

					// ///////////////////////////////////////
					// /PRIMARY ACTIVITY
					if (BIG_ONE) {
						//NOT ADAPTED TO UPDATED IMPROVEMENTS IN DATA
						NetUtil.addToLocalData_Big(ticker, ID);
					} else {
						NetUtil.addToLocalData(ticker, ID,null);
					}// UPON COMPLETION PRELOAD HAS 1 MORE DATA
						// saveProgress has 1 more
						// ///////////////////////////////////////
				 
					DSUtil.addToDatastoreAsString(DSUtil.THREAD, ID, "shutdownTime", ""+NetUtil10M.getIndexFromTicker(ticker)+" : "+ticker+" ("+vars.get(ID).get()+")");
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {

					}
				}
			}

		});
		thread.start();

	}

	private static void logThreadComplete(String ID, String tag, int memory) {
		String text = "complete (" + vars.get(ID) + "):"
				+ DATE.format(new Date()) + "  " + tag + "  totalMemory: "
				+ memory;
		DSUtil.addToDatastoreAsString(DSUtil.THREAD, ID, "shutdownTime", text);
	}

	// completion: all data gathered and email has not been set
	protected static boolean isComplete(String id) {
		memoryMonitor.put(id,
				(int) (Runtime.getRuntime().totalMemory() / 1000000)
						- (int) (Runtime.getRuntime().freeMemory() / 1000000));
		// every X times (for each thread) save progress as blob
		considerSavingProgress(id);

		if (completeResults.size() == (QQ.length + YY.length)) {
tryToCollectMissingData();
			logThreadComplete(id, " + ", getTotalMemoryUsed());
			if (!emailSent.getAndSet(true)) {

				logThreadComplete(id, " @@ ", getTotalMemoryUsed());
//				DSBlobs.postMemoryDataId("   start email: "
//						+ DATE.format(new Date()));
				EmailUtil.sendDataByEmail(Qi.QQ, Backend.fileTitleQ,
						"parelius@uw.edu");
				EmailUtil.sendDataByEmail(Yi.YY, Backend.fileTitleY,
						"parelius@uw.edu");
				// convert to DS Arrays
				if (!BIG_ONE) {
					DSBlobs.saveNewToDatabase(QQ, fileTitleQ);
					DSBlobs.saveNewToDatabase(YY, fileTitleY);
				}
				// writing to blobstore DEPRECATED!!!use email
				DSUtil.writeToBlobStore(YY, fileTitleY);
				DSUtil.writeToBlobStore(QQ, fileTitleQ);
				// key to date determined weekly cron starting backend
				if (BIG_ONE) {
					DSUtil.addToDatastoreAsString(DSUtil.LAST_DATE_lg, "data",
							"" + daysSince());

				} else {
					DSUtil.addToDatastoreAsString(DSUtil.LAST_DATE_sm, "data",
							"" + daysSince());
				}
//				DSBlobs.postMemoryDataId("  end email: "
//						+ DATE.format(new Date()));
				DSBlobs.deleteTempBlobs();
			}

			return true;// stop thread loop

		}
		return false;// continue thread loop
	}

	private static void tryToCollectMissingData() {
	for(int i = 0; i<2; i++){
		int size = retryTickers.size();
		for(int j= 0;j<size; j++){
String ticker = retryTickers.poll();
			NetUtil.addToLocalData(ticker, null,previousData.get(ticker));
		}
		
		
		
	}
	}

	protected static void considerSavingProgress(String id) {
		int progress = vars.get(id).getAndIncrement();

		if (getTotalMemoryUsed() > MEM_CLASS || progress % BATCH == (BATCH - 1)
				|| (completeResults.size() == (QQ.length + YY.length))) {

			saveCurrentResultsToDS(id, progress);
		}

	}

	private static void saveCurrentResultsToDS(String id, int progress) {
		TreeMap<String, String> saving = saveProgress.get(id);
		DSBlobs.object_asBlob_toDatastore_temp(id + " (" + progress + ")",
				saving);
		saving.clear();
	}

	public static int getTotalMemoryUsed() {
		int mem = 0;
		for (int m : memoryMonitor.values())
			mem += m;
		return mem;
	}

	protected static void updateQueIfNecessary() {
		if ((tickers.isEmpty() || tickers.size() > QQ.length + YY.length)
				&& (completeResults.size() != (QQ.length + YY.length))) {

			tickers.clear();
			resetTickersLessSavedData();

		}
	}

	private static void genericBackendOutline() {

		LifecycleManager.getInstance().setShutdownHook(
				new LifecycleManager.ShutdownHook() {
					public void shutdown() {

						LifecycleManager.getInstance().interruptAllRequests();

					}
				});

		Thread thread = ThreadManager.createBackgroundThread(new Runnable() {

			public void run() {

			}

		});
		thread.start();

	}

	public static String fileTitle(String nasOrNy) {
		double time = System.currentTimeMillis();
		// convert to seconds
		time /= 1000.0;
		// convert to hours
		time /= 3600.0;
		// to days
		time /= 24.0;
		// limit to one decimal place
		time = (double) ((int) (time * 10) / 10.0);

		return nasOrNy + "_" + time + ".txt";
		//
	}

	public static float secondsSince() {
		Date d = new Date();
		float hours = d.getHours() * 1000000;
		float time = d.getMinutes() * 60;
		time += d.getSeconds();
		time += hours;
		return time;
		//
	}

	public static float daysSince() {
		float time = System.currentTimeMillis();
		// convert to seconds
		time /= 1000.0;
		// convert to hours
		time /= 3600.0;
		// to days
		time /= 24.0;
		// limit to one decimal place
		time = (float) ((int) (time * 10) / 10.0);
		return time;
		//
	}

	public static void clearData() {

		Thread thread = ThreadManager.createBackgroundThread(new Runnable() {

			public void run() {
				// clear datastore

				DSUtil.deleteDatastore("TEMP_BLOBS");
				DSUtil.deleteDatastore(DSUtil.DEBUG);
				DSUtil.deleteDatastore(DSUtil.MEM_LOG);
				DSUtil.deleteDatastore(DSUtil.THREAD);

			}

		});
		thread.start();

	}
	public static void clearDataFor1Minute(){

		DSUtil.deleteDatastore("TEMP_BLOBS");
		DSUtil.deleteDatastore(DSUtil.DEBUG);
		DSUtil.deleteDatastore(DSUtil.MEM_LOG);
		DSUtil.deleteDatastore(DSUtil.THREAD);

	}
}
