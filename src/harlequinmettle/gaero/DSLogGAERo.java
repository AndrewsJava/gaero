package harlequinmettle.gaero;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;

public class DSLogGAERo {
	static int maxMemoryUsed = 0;

	public static void checkMemoryUse() {

		int currentTotalMemory = getTotalMemoryUsed();

		if (currentTotalMemory > maxMemoryUsed) {

			maxMemoryUsed = currentTotalMemory;

			logMemoryUseage();

		}
	}

	static void logMemoryUseage() {

		float totalMemory = getTotalMemoryUsed();

		float maxMemory = (float) ((Runtime.getRuntime().maxMemory()) / 1000000);

		log("" + totalMemory + " / " + maxMemory);
	}

	public static int getTotalMemoryUsed() {

		return (int) (Runtime.getRuntime().totalMemory() / 1000000)
				- (int) (Runtime.getRuntime().freeMemory() / 1000000);
	}

	static void log(String instanceName, String propertyData) {

		Entity one = new Entity("DEBUG", instanceName);

		Key getter = KeyFactory.createKey("DEBUG", instanceName);

		one.setProperty(DatastoreAccessGAERo.DEBUG_PROPERTY,
				new Text(propertyData.toString()));

		DatastoreAccessGAERo.DATASTORE.put(one);
		// Key getter = KeyFactory.createKey(commonRoot,uniqueKey);
	}

	public static void log(String msg) {
		DatastoreAccessGAERo.addToDatastoreDebugLogger(msg);
	}

}
