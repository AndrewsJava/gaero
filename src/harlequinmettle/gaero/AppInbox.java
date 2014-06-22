package harlequinmettle.gaero;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.TreeMap;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AppInbox extends HttpServlet implements DBLabels {
	private boolean textIsHtml = false;

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		try {
			MimeMessage message = new MimeMessage(session, req.getInputStream());
			// EXTRACT DATA FROM MESSAGE AND STORE IN DATASTORE/BLOBSTORE
			// DataHandler emaildata = message.getDataHandler();
			// emaildata.

			String content = "no content";

			MimeMultipart emailbody = ((MimeMultipart) message.getContent());
			for (int j = 0; j < emailbody.getCount(); j++) {

				BodyPart bodyPart = emailbody.getBodyPart(j);

				String disposition = bodyPart.getDisposition();

				if (disposition != null
						&& (disposition.equalsIgnoreCase("ATTACHMENT"))) {

					DataHandler handler = bodyPart.getDataHandler();
					InputStream attachmentIn = bodyPart.getInputStream();

					content = convertStreamToString(attachmentIn);

					String nameID = bodyPart.getFileName();
					storeData(nameID, content);
					// DSUtil.addToDatastoreAsText(DSUtil.DEBUG, nameID,
					// "data"+j , content);
					// PROCESS APPROPRIATELY TO STORE ARRAYS IN DATASTORE

				} else {
					// content = getText(bodyPart); // the changed code
				}
				// String body = getText(emailbody);

			}
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	// for each file i convert stored data to numeric data
	static void storeData(String title, String fileData) {
		int DBSize = Qi.QQ.length;
		String[] DBTickers = Qi.QQ;

		String yORq = title.split("_")[0];
		String DBT = "";
		String DBF = "";
		if (yORq.equals("nas")) {
			DBF = DSUtil.NASDAQ_FUNDAMENTALS;
			DBT = DSUtil.NASDAQ_TECHNICALS;

		} else if (yORq.equals("ny")) {
			DBSize = Yi.YY.length;
			DBTickers = Yi.YY;
			DBF = DSUtil.NYSE_FUNDAMENTALS;
			DBT = DSUtil.NYSE_TECHNICALS;

		}

		// CHOOSE QQ VS YY HERE FOR SIZE OF ARRAYS
		// for each time stage file construct array 85 fundamental data
		// points for each symbol
		float[][] data = new float[DBSize][];
		// for each time stage file construct array 7 technical data points
		// for each symbol
		float[][][] pdata = new float[DBSize][][];
		float[] weeksPrices = new float[DBSize];
		TreeMap<String, String> textData = new TreeMap<String, String>();
		Float days = Float
				.parseFloat(title.replaceAll("\\.txt", "").split("_")[1]);
		loadStringDataIntoTreeMap(fileData, textData);
		int nullcount = 0;
		// ASSUMES A 1 TO 1 EXISTENCE OF NAS AND NY FIELS - TRUE SO FAR
		for (int j = 0; j < DBSize; j++) {
			String ticker = DBTickers[j];
			String textdata = textData.get(ticker);

			if (textdata == null) {

			}

			final int[] sizes = { 2, 36, 44, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 1,
					1, 1 };
			// fundamental data points
			final float[] values = new float[labels.length];
			// technical data points: last ten days (dayNumber open high low
			// close volume adjClose)
			// adding some measure of volatility in price and volume
			//
			final float[][] dailyData = new float[10][TECHNICAL.length];

			if (textdata != null) {

				// still need validation aspect
				float[] rawData = validSmallDataSet(textdata, sizes);
				if (rawData.length != 175)
					continue;

				// puts fundamental data points from rawData into values
				fillFundamentalData(values, rawData);
				// puts technical data from rawData into dailyData
				// System.out.println(dbSet[j]);
				fillTechnicalData(dailyData, rawData);
				// sets price values directly to float[][] prices
				fillPriceData(j, days, weeksPrices, dailyData);
			} else {

				Arrays.fill(values, Float.NaN);
				for (int z = 0; z < dailyData.length; z++)
					Arrays.fill(dailyData[z], Float.NaN);
			}
			// assign actual values to database
			data[j] = values;
			pdata[j] = dailyData;
		} // System.out.println(nullcount + " null  ");

		DSUtil.saveAsFloatArray2D(DBF,
				"FUNDAMENTAL_" + title.replaceAll("\\.txt", ""), data);
		DSUtil.saveAsFloatArray3D(DBT,
				"TECHNICAL_" + title.replaceAll("\\.txt", ""), pdata);
	}

	private static void loadStringDataIntoTreeMap(String fileData,
			TreeMap<String, String> textData) {
		BufferedReader br = new BufferedReader(new StringReader(fileData));

		try {
			String a;
			while ((a = br.readLine()) != null) {
				String[] enter = a.split("\\^");
				textData.put(enter[0].trim(), enter[1].trim());
			}
			br.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	static float[] validSmallDataSet(String string, int[] pattern) {
		// possibly add # replacement value
		// big data is pre" "splitting arrays - overall length should always be
		// one
		String[] validateBySize = string.split(" ");
		for (int i = 0; i < validateBySize.length; i++) {
			int size = validateBySize[i].replaceAll("@", " ")
					.replaceAll("_", " ").split(" ").length;
			// /COMPARE SIZES TO PATTERN i - IF SIZE IS WRONG REPLACE WITH NAN
		}
		String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
				"Aug", "Sep", "Oct", "Nov", "Dec" };

		String[] datain = string.replaceAll("@", " ").replaceAll("_", " ")
				.split(" ");
		int len = datain.length;

		float dub = 0;
		float factor = 1;
		float[] processed = new float[datain.length];
		for (int i = 0; i < datain.length; i++) {
			factor = 1;
			String data = datain[i].replace("$", "");// remove unrecognized
														// symbols
			data = data.replace("%", "");
			data = data.replaceAll("--", "");
			data = data.replaceAll("\\(", "-");// NEG IN ACCOUNTING IN
												// PARENTHESIS
			data = data.replaceAll("\\)", "");
			data = data.replaceAll("NM", "");
			data = data.replaceAll("Dividend", "");

			// replaced individual if stmts with for loop HOPE STILL TO WORK
			// convert month text into number
			for (int j = 0; j < months.length; j++) {
				if (data.equals(months[j])) {
					data = "" + j;
				}
			}
			// would like to use industry rank info better 12|50?
			// possibly convert directly to ratio
			if (data.contains("|"))
				data = data.substring(0, data.indexOf("|"));
			// cnn recommendations converted to numbers
			if (data.equals("Sell"))
				data = "-100";
			if (data.equals("Underperform"))
				data = "-10";
			if (data.equals("Hold"))
				data = "1";
			if (data.equals("Outperform"))
				data = "10";
			if (data.equals("Buy"))
				data = "100";// note the B would cause billions
			if (data.equals("#"))
				processed[i] = Float.NaN;// number to replace blanks #

			if (data.contains("T")) {// for when billions are abreviated B
				factor = 1000000000000f;
				data = data.replaceAll("T", "");
			}
			if (data.contains("B")) {// for when billions are abreviated B
				factor = 1000000000;
				data = data.replaceAll("B", "");
			}
			if (data.contains("M")) {
				factor = 1000000;
				data = data.replaceAll("M", "");
			}
			if (data.contains("K")) {
				factor = 1000;
				data = data.replaceAll("K", "");
			}
			if (data != null) {
				String dat = data.replaceAll(",", "");// remove commas from
														// zeros1,000
				dub = doFloat(dat);
				processed[i] = dub * factor;
			} else {
				// System.out.println("** unknown string default -> -1e-7 "+
				// data);
				processed[i] = Float.NaN;
			}
		}

		return processed;
	}

	private static float doFloat(String value) {
		try {
			return Float.parseFloat(value);

		} catch (Exception e) {
			return Float.NaN;
		}
	}

	private static void fillPriceData(int j, float days, float[] weeksPrices,
			float[][] dailyData) {

		if (days - dailyData[0][0] < 4 && days - dailyData[0][0] >= 0) {
			weeksPrices[j] = dailyData[0][6];
			// dateset.add((int) (days - dailyData[0][0]));//
			// /////////////////////////////////////////
		} else {
			weeksPrices[j] = Float.NaN;
		}

	}

	private static void fillTechnicalData(float[][] dailyData, float[] rawData) {

		// construct technical data
		// 82 - 171

		// 82 = month
		// 83 = day
		// 84 = year
		// 85-90 = 6 daily data points
		// repeats 10 times
		// System.out.println("   --------------              ---------------------    -----");
		for (int x = 0; x < 10; x++) {
			final int month = (int) rawData[82 + 9 * x] + 1;
			final int day = (int) rawData[83 + 9 * x];
			final int year = (int) rawData[84 + 9 * x];
			String mnth = "";
			if (month < 10)
				mnth += "0";
			mnth += month;
			String dy = "";
			if (day < 10)
				dy += "0";
			dy += day;
			try {
				final Date date1 = new SimpleDateFormat("MM/dd/yy").parse(mnth
						+ "/" + dy + "/" + year);
				final long moneyTime = date1.getTime();
				float dayTime = moneyTime / 1000.0f;
				dayTime /= 3600.0f;
				dayTime /= 24.0f;
				// dateset.add(dayTime);////////////////////////////////////////////////////////////
				final float open = rawData[85 + 9 * x];
				final float high = rawData[86 + 9 * x];
				final float low = rawData[87 + 9 * x];
				final float close = rawData[88 + 9 * x];
				final float vol = rawData[89 + 9 * x];
				final float adjClose = rawData[90 + 9 * x];
				final float[] dlda = { dayTime, open, high, low, close, vol,
						adjClose };
				// System.out.println(Arrays.toString(dlda));
				dailyData[x] = dlda;
			} catch (ParseException e) {
				e.printStackTrace();
			}

		}

	}

	private static void fillFundamentalData(float[] values, float[] rawData) {
		for (int k = 0; k < 82; k++) {
			values[k] = rawData[k];
		}
		values[82] = rawData[172];
		values[83] = rawData[173];
		values[84] = rawData[174];

	}

	public static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	private String getText(Part p) throws MessagingException, IOException {
		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			textIsHtml = p.isMimeType("text/html");
			return s;
		}

		if (p.isMimeType("multipart/alternative")) {
			// prefer html text over plain text
			Multipart mp = (Multipart) p.getContent();
			String text = null;
			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);
				if (bp.isMimeType("text/plain")) {
					if (text == null)
						text = getText(bp);
					continue;
				} else if (bp.isMimeType("text/html")) {
					String s = getText(bp);
					if (s != null)
						return s;
				} else {
					return getText(bp);
				}
			}
			return text;
		} else if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				String s = getText(mp.getBodyPart(i));
				if (s != null)
					return s;
			}
		}

		return null;
	}

}
