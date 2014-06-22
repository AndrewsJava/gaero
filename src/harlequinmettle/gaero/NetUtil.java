package harlequinmettle.gaero;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

public class NetUtil {

	public static final String yahoobase = "http://finance.yahoo.com/q";
	public static final String fcstbase = "http://money.cnn.com/quote/forecast/forecast.html?symb=";

	public static String eodbase = "http://www.eoddata.com/stockquote/";
	public static String cnnbase = "http://money.cnn.com/quote/quote.html?symb=";
	public static String longString = "123456789012345678901234567890123456789012345678901234567890-12345678901234567890-12345678901234567890";

	public static AtomicInteger fetchFails = new AtomicInteger(0);

	static String[] collectAndExtractDataFromNetwork(String tk) {
		fetchFails.set(0);
		String[] saveAsText = new String[5];

		saveAsText[0] = (cnnForecast(tk, true).trim());

		saveAsText[1] = (analystEstimates(tk));

		saveAsText[2] = (keyBasedData(yahoobase + "/ks?s=" + tk, keykeys));

		saveAsText[3] = (limitToTen(pastPrices(tk)));

		saveAsText[4] = (optionsCount(tk));

		if (fetchFails.get() > 5) {
			DSUtil.addToDatastoreAsText(DSUtil.DEBUG, "     " + tk
					+ "  fails: " + fetchFails.get(), "data",
					saveAsText.toString());// ///////////////////////////////////////////////////////////////
		}
		return saveAsText;
	}

	static String[] collectAndExtractDataFromNetwork(String tk,
			String[] previousData) {
		fetchFails.set(0);

		int cnnerrors = previousData[0].split("#").length;
		int yhooests = previousData[1].split("#").length;
		int yhoostats = previousData[2].split("#").length;
		int yhoprices = previousData[3].split("#").length;
		if (cnnerrors > 0) {
			String retry = (cnnForecast(tk, true).trim());
			int retrycnnerrors = retry.split("#").length;
			if (retrycnnerrors < cnnerrors)
				previousData[0] = retry;
		}
		if (yhooests > 0) {
			String retry = (analystEstimates(tk));
			int retryyhooests = retry.split("#").length;
			if (retryyhooests < yhooests)
				previousData[1] = retry;
		}
		if (yhoostats > 0) {
			String retry = (keyBasedData(yahoobase + "/ks?s=" + tk, keykeys));
			int retryyhoostats = retry.split("#").length;
			if (retryyhoostats < yhoostats)
				previousData[2] = retry;
		}
		if (yhoprices > 0) {
			String retry = (limitToTen(pastPrices(tk)));
			int retryyhoprices = retry.split("#").length;
			if (retryyhoprices < yhoprices)
				previousData[3] = retry;
		}

		return previousData;
	}

	static void addToLocalData(String tk, String threadID, String[] prevData) {
		String data = "";
		String[] dataArray;
		if (prevData == null)
			dataArray = collectAndExtractDataFromNetwork(tk);
		else
			dataArray = collectAndExtractDataFromNetwork(tk, prevData);

		if (hasMissingData(dataArray)) {
			Backend.retryTickers.add(tk);
			Backend.previousData.put(tk, dataArray);
		}
		for (String s : dataArray) {
			data += s + " ";
		}
		data = data.trim();
		// data = NetUtil10M.collectAndExtract_Big_DataFromNetwork( tk);
		data = tk + "^" + data.replace("\n", "#_#_#_#").replace("\r", "")
				+ "\n";
		if (threadID != null)
			Backend.saveProgress.get(threadID).put(tk, data);

		Backend.completeResults.put(tk, data);

	}

	private static boolean hasMissingData(String[] dataArray) {

		if (dataArray[0].split("#").length > 0)
			return true;
		if (dataArray[1].split("#").length > 0)
			return true;
		if (dataArray[2].split("#").length > 0)
			return true;
		if (dataArray[3].split("#").length > 0)
			return true;

		return false;
	}

	static void addToLocalData_Big(String tk, String threadID) {

		String data = "no data";

		// data = collectAndExtractDataFromNetwork(tk);
		data = NetUtil10M.collectAndExtract_Big_DataFromNetwork(tk);
		data = tk + "^" + data.replace("\n", "#_#_#_#").replace("\r", "")
				+ "\n";

		Backend.saveProgress.get(threadID).put(tk, data);

		Backend.completeResults.put(tk, data);

	}

	// #################################
	// ////////////////////past and current estimates on earnings revenue growth
	public static String analystEstimates(String stock) {
		int counting = 0;
		// Year Ago EPS //after this key substring to Revenue Est
		String httpdata = getHtml(yahoobase + "/ae?s=" + stock);
		String chop = "";
		if (httpdata.contains("Earnings Est")
				&& httpdata.contains("Currency in USD")) {
			chop = httpdata
					.substring(httpdata.indexOf("Earnings Est"),
							httpdata.indexOf("Currency in USD"))
					.replaceAll("d><t", "d> <t").replaceAll("d></t", "d>@</t");
		}
		String rval = "";
		chop = removeHtml((chop), true).replaceAll("_", " ");
		for (String k : estimatekeys) {
			counting++;
			if (chop.contains(k)) {
				if (counting == 6 && chop.contains("Earnings Hist"))
					chop = chop.substring(chop.indexOf("Earnings Hist"));
				String datapart = chop.substring(chop.indexOf(k) + k.length());
				if (datapart.contains("@"))
					datapart = datapart.substring(0, datapart.indexOf("@"))
							.trim();
				for (int i = 0; i < 4 - datapart.split(" ").length; i++)
					datapart += "_#";
				rval += datapart + " ";
			} else
				rval += "# # # # ";
		}
		String backAt = (rval.replaceAll("_", " ").trim().replaceAll(" ", "_"));
		// System.out.println(backAt);

		return backAt;
	}

	// #################################
	// /////////////forecast data from cnn price analysts status
	public static String cnnForecast(String stock, boolean firstTry) {

		String httpdata = getHtml(fcstbase + stock);
		if (httpdata == null)
			System.out.println("null forecast");
		if (httpdata.contains(">There is no")
				|| httpdata.contains("was not found")) {
			// System.out.println("NO CNN DATA FOR: " + stock);
			return "#_#";
		}
		String chop = "#_#";

		if (httpdata.contains(">Stock Price Forecast")) {

			chop = httpdata
					.substring(httpdata.indexOf(">Stock Price Forecast"));
			if (chop.contains("Earnings and Sales Forecasts")
					&& chop.contains("The"))

				chop = chop.substring(chop.indexOf("The") + 3);
			String analysts = "#";
			String forecast = "#";
			try {
				if (chop.contains("analyst"))
					analysts = chop.substring(0, chop.indexOf("analyst"))
							.trim();

				if (chop.contains("represents a"))
					forecast = chop.substring(
							chop.indexOf("represents a") + 12,
							chop.indexOf("%")).replaceAll("_", "");
				if (forecast.contains(">"))
					forecast = forecast.substring(forecast.indexOf(">") + 1)
							.trim();
				if (forecast.length() > 23)
					forecast = "#";
			} catch (Exception e) {
			}
			chop = analysts + "_" + forecast;
		}
		if (firstTry && chop.equals("#_#")) {
			chop = cnnForecast(stock, false);
		}
		return (chop);

	}

	// //////////////////////////////////////
	private static boolean printQuickCount(StringBuilder saveAsText) {
		String[] sections = saveAsText.toString().replaceAll("@", " ")
				.split(" ");
		StringBuilder build = new StringBuilder();
		for (String s : sections) {
			build.append(" " + s.split("_").length);
		}
		// System.out.println(build);
		return build.toString().equals(" 2 36 44 9 9 9 9 9 9 9 9 9 9 1 1 1");
	}

	// ///////////////////////////////////////////////////////////////////
	static String limitToTen(String pastPrices) {
		// TODO Auto-generated method stub
		if (pastPrices.contains("Split"))
			System.out.println("**S-->" + pastPrices);
		if (pastPrices.equals("HISTORIC"))
			return "#_#_#_#_#_#_#_#_#@#_#_#_#_#_#_#_#_#@#_#_#_#_#_#_#_#_#@#_#_#_#_#_#_#_#_#@#_#_#_#_#_#_#_#_#@#_#_#_#_#_#_#_#_#@#_#_#_#_#_#_#_#_#@#_#_#_#_#_#_#_#_#@#_#_#_#_#_#_#_#_#@#_#_#_#_#_#_#_#_#"
					+ " #" + " #";
		double split = 1;
		double dividends = 0;
		String[] days = pastPrices.split("@");
		StringBuilder reconstruct = new StringBuilder();
		int counter = 0;
		for (int i = 0; i < days.length; i++) {
			String[] data = days[i].split("_");
			if (data.length < 9) {
				System.out.println(pastPrices);
				String[] determine = days[i].split(":");
				try {
					if (determine.length < 2)
						dividends += doDouble(days[i].split("_")[3]);
					else if (counter < 5) {
						split = doDouble(determine[0].split("_")[3])
								/ doDouble(determine[1].toLowerCase()
										.replace("stock_split", "")
										.replaceAll("_", ""));
						System.out.println("SPLIT: " + split + "\nFrom: "
								+ pastPrices);
					}
				} catch (Exception e) {
					System.out.println(days[i]);
					e.printStackTrace();
				}
			} else {
				reconstruct.append(days[i] + "@");
				counter++;
				if (counter == 10)
					break;
			}
		}
		for (int j = counter - 1; j < 9; j++)
			reconstruct.append("#_#_#_#_#_#_#_#_#@");
		return reconstruct.append(" " + dividends + " " + split)
				.deleteCharAt(reconstruct.lastIndexOf("@")).toString();
	}

	// ///////////////////////////////////////////////////////////////////
	public static String optionsCount(String stock) {
		String httpdata = getHtml(yahoobase + "/op?s=" + stock);
		if (httpdata.contains(">There is no")
				|| httpdata.contains("Check your spelling")
				|| httpdata.indexOf("View By Expiration") < 0)
			return "0";
		try {
			httpdata = httpdata.substring(httpdata
					.indexOf("View By Expiration"));
			httpdata = httpdata.substring(0, httpdata.indexOf("table"));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "" + (httpdata.toLowerCase().split("a href").length - 1);
	}

	/**
	 * Converts String data into double data removing commas, n/a's, and
	 * replacing M's, and B's with six and nine zeros respectively.
	 * 
	 * @param datain
	 *            raw data from stock data web site.
	 */
	static double[] dataLightSubprocessor(String datain) {
		// possibly add # replacement value
		String[] dataInSplit = datain.replaceAll("@", " ").replaceAll("_", " ")
				.split(" ");
		//
		if (dataInSplit.length != 175)
			System.out.println(dataInSplit.length);
		double[] processed = new double[dataInSplit.length];
		for (int i = 0; i < dataInSplit.length; i++) {
			double factor = 1;
			String data = dataInSplit[i].replace("$", "");// remove unrecognized
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
			if (data.equals("#"))
				processed[i] = -0.0000001;// number to replace blanks #

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
				double dub = doDouble(dat);
				processed[i] = dub * factor;
			} else {
				// System.out.println("** unknown string default -> -1e-7 "+
				// data);
				processed[i] = -0.0000001;
			}
		}
		return processed;
	}

	/*
	 * returns the double value of a string and returns -1E-7 if the string
	 * could not be parsed as a double.
	 * 
	 * @param value the string that gets converted into a double.
	 */
	private static double doDouble(String value) {
		try {
			double val = Double.parseDouble(value);
			if (val == val)// only return value if its not NaN , NaN==NaN is
							// false
				return val;
			else
				return -0.0000001;
		} catch (Exception e) {
			// System.out.println(" TEXT TO NUMBER ERR "+ value +" to -1e-7 ");
			return -0.0000001;
		}
	}

	// ///////////replace @,^,shorten to end of table,
	public static String reformat(String input) {
		String output = input.replaceAll("@", "_").replaceAll("^", "_")
				.replaceAll("\\*", "_");
		if (input.contains("</table>"))
			output = output.substring(0, output.indexOf("</table>"));
		output = output.replaceAll("d><t", "d> <t").replaceAll("h><t", "h> <t")
				.replaceAll("d></t", "d>@</t").replaceAll("&nbsp;", "-")
				.replaceAll("--", "");
		return output;
	}

	// /////////////////#######################################
	public static String pastPrices(String stock) {

		String httpdata = getHtml(yahoobase + "/hp?s=" + stock);
		if (!httpdata.contains("Adj Close"))
			return "HISTORIC";
		httpdata = removeHtml(
				reformat(httpdata.substring(httpdata.indexOf("Adj Close"))),
				false);
		httpdata = httpdata.substring(0, httpdata.lastIndexOf("@"));
		if (httpdata.indexOf("@") < 2)
			httpdata = httpdata.substring(httpdata.indexOf("@") + 1);
		if (httpdata.lastIndexOf("@") > 1)
			httpdata = (httpdata.substring(0, httpdata.lastIndexOf("@"))
					.replaceAll(",", ""));
		return httpdata;
	}

	/**
	 * This method returns a String of http data f
	 * 
	 * @param suf
	 *            The http address to get string from.
	 */
	static protected String getHtml(String suf) {

		int trycount = 0;
		URL url;
		String str = "";
		while (str.length() < 100) {
			trycount++;
			try {
				url = new URL(suf);
				URLFetchService fetcher = URLFetchServiceFactory
						.getURLFetchService();
				Future<HTTPResponse> htmlresults = fetcher
						.fetchAsync(new HTTPRequest(url, HTTPMethod.GET));
				HTTPResponse rawHtml = htmlresults.get();
				str = new String(rawHtml.getContent());
			} catch (Exception e) {
				str = ("urlfetch: InterruptedException, ExecutionException,	CancellationException");

				String name = new Object() {
				}.getClass().getEnclosingMethod().getName();
				DSLogGAERo.log(name + "   " + e.toString());
				try {
					Thread.sleep(300);
				} catch (InterruptedException ee) {

				}
			}
			if (str.length() < 100) {
				fetchFails.getAndIncrement();

			}

			if (trycount > 150) {
				return str;
				// System.exit(0);
			}

		}
		// try sleeping between urlfetches in case that is too much per second
		// for quota
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {

		}
		return str;
	}

	/**
	 * This method returns a String of http data f
	 * 
	 * @param suf
	 *            The http address to get string from.
	 */
	static protected String getHtml2(String suf) {

		URL url;
		InputStream is;
		InputStreamReader isr;
		BufferedReader r;
		String str = "";
		String nl = "";

		try {
			url = new URL(suf);

			is = url.openStream();
			isr = new InputStreamReader(is);
			r = new BufferedReader(isr);
			do {
				nl = r.readLine();
				if (nl != null) {
					nl = nl.trim() + " ";
				}
				str += nl;
			} while (nl != null);
		} catch (MalformedURLException e) {
			System.out.println("Must enter a valid URL");
		} catch (IOException e) {
			// System.out.println( "IO error getting  html data for site :  " +
			// suf);
		}
		return str;
	}

	/**
	 * This method colects into a string data from tables about stock in the
	 * form nnn_mmm_nnn_mmm@nnn_mmm_nnn_mmm@etc
	 * 
	 * @param addy
	 *            The internet address to get html from.
	 * @param keys
	 *            The text keys expected to be found in the html data
	 */
	static protected String keyBasedData(String addy, String[] keys) {
		for (int i = 0; i < 10; i++) {// try 10 times to get html or else return
			if (i > 0)
				System.out.println("\nConnection Failure. Trying again: " + i);
			String httpdata = getHtml(addy);
			String yhdata = "";
			String str = httpdata;
			if (str.contains("was not found"))
				return "#_#_#";
			if (str.contains("Recommendation Trends")) {
				str = (str.substring(str.indexOf("Recommendation Trends")));
				str = str.replaceAll("d><t", "d> <t");
			}
			for (String key : keys) {
				if (str.contains(">" + key)) {
					String strx = str.substring(str.indexOf(">" + key) + 1);
					if (!strx.contains("</tr>"))
						return "#_#_#";
					strx = strx.substring(0, strx.indexOf("</tr>"));
					if (key.equals("Sector"))
						strx = strx.replaceAll(" ", "|");
					strx = removeHtml(strx, true).replaceAll("@", " ");// just
																		// in
																		// case
					if (strx.length() == 0)
						strx = "#";// placeholder if data does not exist
					yhdata += strx + "_";
				} else {
					yhdata += "#_";

				}
			}
			// return spacify(yhdata.replaceAll("--", "#"));
			return (yhdata.replaceAll("--", "#").replaceAll("_", " ").trim()
					.replaceAll(" ", "_"));
		}
		return "#_#";
	}

	// ///////////////////////////////////////////////////////////

	/**
	 * This method removes html tags such as <a href = ...> and
	 * <table>
	 * 
	 * @param withhtml
	 *            The string text with tags included
	 */
	public static String removeHtml(String withhtml, boolean colon) {
		String stripped = withhtml;//
		String save = "";
		if (colon)
			if (stripped.indexOf(":") > 0) {// jump ahead just past the colons
				stripped = withhtml.substring(withhtml.indexOf(":") + 1);
			}
		// skip all sections of html code between the <htmlcode>
		while (stripped.indexOf("<") >= 0 && stripped.indexOf(">") > 0) {
			stripped = stripped.substring(stripped.indexOf(">") + 1);
			if (stripped.indexOf("<sup") < 2 && stripped.indexOf("<sup") > -1)
				stripped = stripped.substring(stripped.indexOf("</sup>") + 6);
			if (stripped.indexOf("<") > 0)// keep any text inbetween
											// <code>keeptext<code>
				save += stripped.substring(0, stripped.indexOf("<"));
		}
		// save = save.replaceAll("-","_");//VITAL TO PRESERVE NEGATIVES
		save = save.replaceAll(" ", "_");
		save = save.replaceAll("___", "_");
		save = save.replaceAll("__", "_");
		save = save.replaceAll("_-_", "_");
		return save;

	}

	public static String[] keykeys = { "Market Cap", // 0
			"Enterprise Value", //
			"Trailing P/E",// 2
			"Forward P/E",// 3
			"PEG Ratio", // 4
			"Price/Sales", //
			"Price/Book",//
			"Enterprise Value/Revenue",// 7
			"Enterprise Value/EBITDA ",//
			"Profit Margin",//
			"Operating Margin",//
			"Return on Assets", //
			"Return on Equity",//
			"Revenue", // 13
			"Revenue Per Share",//
			"Qtrly Revenue Growth",//
			"Gross Profit",// 16
			"EBITDA",//
			"Net Income Avl to Common", //
			"Diluted EPS",// 19
			"Qtrly Earnings Growth",//
			"Total Cash",//
			"Total Cash Per Share",// 22
			"Total Debt",//
			"Total Debt/Equity",//
			"Current Ratio", // 25
			"Book Value Per Share",//
			"Operating Cash Flow",// 27
			"Levered Free Cash Flow", //
			"Beta",//
			"52-Week Change",// 30
			"50-Day Moving Average",//
			"200-Day Moving Average",// 32
			"Avg Vol (3 month)",//
			"Avg Vol (10 day)",//
			"Shares Outstanding",// 35
			"Float",//
			"% Held by Insiders",//
			"% Held by Institutions",// 38
			"Shares Short (as of",//
			"Short Ratio (as of",// 40
			"Short % of Float (as of",//
			"Shares Short (prior month)",//
			"Payout Ratio" // 43
	};// 44 values

	public static String[] estimatekeys = { "Avg. Estimate",// these 5 use
			"No. of Analysts",//
			"Low Estimate",//
			"High Estimate", // /
			"Year Ago EPS", // after
							// this
							// key
							// substring
							// to
							// Revenue
							// Est

			"EPS Est",// these 4 use
			"EPS Actual", //
			"Difference", //
			"Surprise %", };//

	public static String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
			"Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

}
