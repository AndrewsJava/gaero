package harlequinmettle.gaero;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;

@SuppressWarnings("serial")
public class StartDataQualityTask extends HttpServlet {
	static final String PARAMETER = "THREAD_COUNT";
	static final String PARAMETER2 = "THREAD_TYPE";

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		doGet(req, resp);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		dataQualityCheck_fundamental(DSUtil.NASDAQ_FUNDAMENTALS);
		dataQualityCheck_fundamental(DSUtil.NYSE_FUNDAMENTALS);
		dataQualityCheck_technical(DSUtil.NASDAQ_TECHNICALS);
		dataQualityCheck_technical(DSUtil.NYSE_TECHNICALS);
	}

	public static void dataQualityCheck_fundamental(String index) {
		ArrayList<String> stats = new ArrayList<String>();
		Query query = new Query(index);
		for (Entity e : DSUtil.ds.prepare(query).asIterable()) {

			Float date = Float.parseFloat(e.getKey().getName().split("_")[2]);
			Blob data = (Blob) e.getProperty(DSUtil.P_FLOAT_ARRAY);
			float[][] fundamental = DSBlobs
					.get2DFloatFromBytes(data.getBytes());
			String someStats = generateDataStatistics_Fund(fundamental);
			stats.add(someStats);

			DSUtil.addToDatastoreAsText(DSUtil.DEBUG, index + " " + date,
					"STATISTICS", someStats);

		}

	}

	private static String generateDataStatistics_Fund(float[][] fundamental) {
		String allStats = new String();
		float nan = 0;
		for (int id = 0; id < fundamental[0].length; id++) {
			ArrayList<Float> stats = new ArrayList<Float>();

			for (float[] d : fundamental) {
				stats.add(d[id]);
				if (d[id] != d[id])
					nan++;
			}
			StatInfo completeStats = new StatInfo(stats, id);
			allStats += "   -->" + completeStats.title + "  ->"
					+ Arrays.toString(completeStats.histogram) + "   ("
					+ completeStats.min + " , " + completeStats.max
					+ ")  <br><br><br>";

		}
		return "<h2>" + nan / (fundamental.length * fundamental[0].length)
				+ "</h2><br>";// + "  :  " + allStats;
	}

	public static void dataQualityCheck_technical(String index) {
		ArrayList<String> stats = new ArrayList<String>();
		Query query = new Query(index);
		for (Entity e : DSUtil.ds.prepare(query).asIterable()) {

			Float date = Float.parseFloat(e.getKey().getName().split("_")[2]);

			Blob data = (Blob) e.getProperty(DSUtil.P_FLOAT_ARRAY);
			float[][][] technical = DSBlobs
					.get3DFloatFromBytes(data.getBytes());
			String someStats = generateDataStatistics_Tech(technical);
			stats.add(someStats);

			DSUtil.addToDatastoreAsText(DSUtil.DEBUG, index + " " + date,
					"STATISTICS", someStats);
		}
	}

	private static String generateDataStatistics_Tech(float[][][] technical) {
		float nan = 0;
		for (int id = 0; id < technical.length; id++) {

			for (int id2 = 0; id < technical[0].length; id2++) {

				for (int id3 = 0; id < technical[0][0].length; id3++) {
					try {
						if (technical[id][id2][id3] != technical[id][id2][id3])
							nan++;
					} catch (ArrayIndexOutOfBoundsException bounds) {

					}
				}
			}
		}
		return "<h2>" + nan / (technical.length * technical[0].length)
				+ "</h2><br>";
	}

}