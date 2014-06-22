package harlequinmettle.gaero;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Future;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

public class NetUtil10M implements QratesI_1, QratesI_2, QratesI_3, QratesI_4,
		YratesI_1, YratesI_2, YratesI_3, YratesI_4 {
	static final float TOLERANCE = 2.1f;
	static final boolean USE_RESTRICTION = true;
	static final int theyearis = 2013;
	static final String yahoobase = NetUtil.yahoobase;
	static final String fcstbase = NetUtil.fcstbase;
	static final String eodbase = NetUtil.eodbase;
	static final String cnnbase = NetUtil.cnnbase;
	static byte[][] RATES;
	// static float[] factors = { 0.05f, 10, 10, 10, 10, 10, 10, 10, 10, 10,
	// 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f };
	static {
		byte[][][] allRates = { RATES_Q_1, RATES_Q_2, RATES_Q_3, RATES_Q_4,
				RATES_Y_1, RATES_Y_2, RATES_Y_3, RATES_Y_4 };
		byte[][] combined = concatBytes(allRates[0], allRates[1]);
		for (int i = 2; i < allRates.length; i++) {
			combined = concatBytes(combined, allRates[i]);
		}
		RATES = combined;
	}

	public static byte[][] concatBytes(byte[][] a, byte[][] b) {
		byte[][] result = new byte[a.length + b.length][];

		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	static String getIndexFromTicker(String ticker) {
		String index = "NASDAQ";

		// arrays should be in natural order
		if (Arrays.binarySearch(Yi.YY, ticker) >= 0)
			index = "NYSE";
		return index;
	}

	static String collectAndExtract_Big_DataFromNetwork(String stock) {
		String index = getIndexFromTicker(stock);

		StringBuilder saveAsText = new StringBuilder();
		int methodID = 0;
		String d1 = options(stock, methodID); // no d2
		methodID++;// 1
		String d2 = competitors(stock, methodID);
		methodID++;// 2
		String d3 = industry(stock, methodID); // no d5
		methodID++;// 3
		String d4 = keyBasedData(yahoobase + "/ao?s=" + stock, opinionkeys,
				stock, methodID);
		methodID++;// 4
		String d5 = estOpinions(stock, methodID);
		methodID++;// 5
		String d6 = cnnForecast(stock, methodID);
		methodID++;// 6
		String d7 = keyBasedData(eodbase + index + "/" + stock + ".htm",
				eodkeys, stock, methodID);
		methodID++;// 7
		String d8 = keyBasedData(cnnbase + stock, cnnkeys, stock, methodID);
		methodID++;// 8
		String d9 = keyBasedData(yahoobase + "?s=" + stock, genkeys, stock,
				methodID);
		methodID++;// 9
		String d10 = keyBasedData(yahoobase + "/ks?s=" + stock, keykeys, stock,
				methodID);
		methodID++;// 10

		String d11 = statements(yahoobase + "/is?s=" + stock + "&annual",
				incomekeys, false, stock, methodID);
		methodID++;// 11
		String d12 = statements(yahoobase + "/is?s=" + stock, incomekeys, true,
				stock, methodID);
		methodID++;// 12
		String d13 = statements(yahoobase + "/bs?s=" + stock + "&annual",
				balancekeys, false, stock, methodID);
		methodID++;// 13
		String d14 = statements(yahoobase + "/bs?s=" + stock, balancekeys,
				true, stock, methodID);
		methodID++;// 14
		String d15 = statements(yahoobase + "/cf?s=" + stock + "&annual",
				cashkeys, false, stock, methodID);
		methodID++;// 15
		String d16 = statements(yahoobase + "/cf?s=" + stock, cashkeys, true,
				stock, methodID);

		String totaldata = d1 + " " + d2 + " " + d3 + " " + d4 + " " + d5 + " "
				+ d6 + " " + d7 + " " + d8 + " " + d9 + " " + d10 + " " + d11
				+ " " + d12 + " " + d13 + " " + d14 + " " + d15 + " " + d16;

		return totaldata;

		// return saveAsText.toString();
	}

	/**
	 * This method returns a String of http data f
	 * 
	 * @param suf
	 *            The http address to get string from.
	 */
	static protected String getHtmlXx(String suf) {

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
			isr.close();
			r.close();
			is.close();
		} catch (MalformedURLException e) {
			System.out.println("Must enter a valid URL");
		} catch (IOException e) {
			// System.out.println( "IO error getting  html data for site :  " +
			// suf);
		}
		return str;
	}

	// ////////////////////////////////////////////////////////////////

	/**
	 * This method colects into a string data from tables about stock in the
	 * form nnn_mmm_nnn_mmm@nnn_mmm_nnn_mmm@etc
	 * 
	 * @param addy
	 *            The internet address to get html from.
	 * @param keys
	 *            The text keys expected to be found in the html data
	 */
	static protected String keyBasedData(String addy, String[] keys,
			String stock, int methodId) {
		for (int i = 0; i < 40; i++) {// try 40 times to get html or else return
										// #_#
			String httpdata = getHtmlForResults(addy, stock, methodId);
			int counter = 0;
			String price = "";
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
					strx = removeHtml(strx).replaceAll("@", " ");// just in case
					if (strx.length() == 0)
						strx = "#";// placeholder if data does not exist
					yhdata += strx + "_";
				} else {
					yhdata += "#_";

					if (key.equals("Strong Buy") || key.equals("Buy")
							|| key.equals("Hold") || key.equals("Underperform")
							|| key.equals("Sell"))
						yhdata += "#_#_#_";

					if (key.equalsIgnoreCase("52wk Range"))
						yhdata += "#_";
				}
			}
			return spacify(yhdata.replaceAll("--", "#"));
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
	public static String removeHtml(String withhtml) {
		String stripped = withhtml;//
		String save = "";
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

	// /////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////////////////
	// several methods to extract data from finance.yahoo sitses
	// counts how may years into the future are available options data

	public static String options(String stock, int methodId) {
		String httpdata = getHtmlForResults(yahoobase + "/op?s=" + stock,
				stock, methodId);
		if (httpdata.contains(">There is no")
				|| httpdata.contains("Check your spelling"))
			return "0";
		int ct = 0;
		for (int i = 1; i < 7; i++)
			if (httpdata.contains("" + (theyearis + i)))
				ct++;
		return "" + ct;
	}

	/**
	 * This method returns a String of http data f
	 * 
	 * @param suf
	 *            The http address to get string from.
	 */
	static protected String getHtmlForResults(String suf, String tkr,
			int methodID) {
		if (Backend.BIG_ONE
				&& USE_RESTRICTION
				&& (RATES[Backend.optimizationIndex.get(tkr)][methodID] < NetUtil10M.TOLERANCE)) {
			return NetUtil.longString;
		}
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
			} catch (MalformedURLException e) {
				str = ("Must enter a valid URL                  " + NetUtil.longString);
			} catch (Exception e) {
				str = ("urlfetch: InterruptedException, ExecutionException,	CancellationException");
				try {
					Thread.sleep(300);
				} catch (InterruptedException ee) {

				}
			}
			if (str.length() < 100) {
				NetUtil.fetchFails.getAndIncrement();

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

	// /////////////////#######################################
	public static String pastPrices(String stock, int methodId) {
		String httpdata = getHtmlForResults(yahoobase + "/hp?s=" + stock,
				stock, methodId);
		if (!httpdata.contains("Adj Close"))
			return "HISTORIC";
		httpdata = removeHtml(reformat(httpdata.substring(httpdata
				.indexOf("Adj Close"))));
		httpdata = httpdata.substring(0, httpdata.lastIndexOf("@"));
		if (httpdata.indexOf("@") < 2)
			httpdata = httpdata.substring(httpdata.indexOf("@") + 1);
		if (httpdata.lastIndexOf("@") > 1)
			httpdata = (httpdata.substring(0, httpdata.lastIndexOf("@"))
					.replaceAll(",", ""));
		return httpdata;
	}

	// //////////////////###################################
	public static String competitors(String stock, int methodId) {
		String httpdata = getHtmlForResults(yahoobase + "/co?s=" + stock,
				stock, methodId);
		if (httpdata.contains("Check your spelling")
				|| httpdata.contains("There is no"))
			return "#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#";
		String rval = "";
		String chop = httpdata;

		if (chop.contains("Direct Competitor Comparison")) {
			chop = chop.substring(chop.indexOf("Direct Competitor Comparison"));
		}
		if (chop.contains("</table>")) {
			chop = chop.substring(chop.indexOf("</table>"));
		}
		if (chop.contains("</table>")) {
			chop = chop.substring(0, chop.indexOf("</table>"));
		}
		chop = reformat(chop);
		chop = "MarketCap:" + removeHtml(chop).replaceAll("_", " ");

		if (httpdata.contains("Market Cap:")) {
			chop = httpdata.substring(httpdata.indexOf("Market Cap:"));
			chop = reformat(chop);
			chop = "MarketCap:" + removeHtml(chop).replaceAll("_", " ");
		} else {
			chop = removeHtml(chop).replaceAll("_", " ");
		}
		for (String k : competekeys) {
			k = k.replaceAll("\\s", "");
			if (chop.contains(k)) {
				String datapart = chop.substring(chop.indexOf(k));
				if (datapart.contains(":"))
					datapart = datapart.substring(datapart.indexOf(":") + 1);
				if (datapart.contains("@"))
					datapart = datapart.substring(0, datapart.indexOf("@"))
							.trim();
				String[] fnl = datapart.split(" ");
				if (fnl.length > 2) {
					rval += fnl[0] + "_" + fnl[fnl.length - 1] + "_";
				}
			} else
				rval += "#_#_";
		}
		return spacify(rval);
	}

	// ///////////////////////#################################
	public static String industry(String stock, int methodId) {
		String httpdata = getHtmlForResults(yahoobase + "/in?s=" + stock,
				stock, methodId);
		if (!httpdata.contains("Rank"))
			return "#_#_#_#_#_#_#_#_#";
		String rval = "";
		String chop = httpdata.substring(httpdata.indexOf("Rank")).replaceAll(
				"> /", "># /");
		chop = reformat(chop);
		chop = "MarketCap:"
				+ removeHtml(chop).replaceAll("_/_", "|").replaceAll("_", " ");

		for (String k : induskeys) {
			k = k.replaceAll("\\s", "");
			if (chop.contains(k)) {
				String datapart = chop.substring(chop.indexOf(k));
				if (datapart.contains(":"))
					datapart = datapart.substring(datapart.indexOf(":") + 1);
				if (datapart.contains("@"))
					datapart = datapart.substring(0, datapart.indexOf("@"))
							.trim();
				String[] fnl = datapart.split(" ");
				if (fnl.length > 1) {
					rval += fnl[fnl.length - 1] + "_";
				}
			} else
				rval += "#_";
		}
		return spacify(rval.replaceAll("_", " ").trim().replaceAll(" ", "_"));
	}

	// #################################
	// ////////////////////past and current estimates on earnings revenue growth
	public static String estOpinions(String stock, int methodId) {
		int counting = 0;
		// Year Ago EPS //after this key substring to Revenue Est
		String httpdata = getHtmlForResults(yahoobase + "/ae?s=" + stock,
				stock, methodId);
		String chop = "";
		if (httpdata.contains("Earnings Est")
				&& httpdata.contains("Currency in USD")) {
			chop = httpdata
					.substring(httpdata.indexOf("Earnings Est"),
							httpdata.indexOf("Currency in USD"))
					.replaceAll("d><t", "d> <t").replaceAll("d></t", "d>@</t");
		}
		String rval = "";
		chop = removeHtml((chop)).replaceAll("_", " ");
		for (String k : estimatekeys) {
			counting++;
			if (chop.contains(k)) {
				if (counting == 6 && chop.contains("Revenue Est"))
					chop = chop.substring(chop.indexOf("Revenue Est"));
				if (k.equals("Current Qtr.") && chop.contains("Growth Est"))
					chop = chop.substring(chop.indexOf("Growth Est"));
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
		return spacify(rval.replaceAll("_", " ").trim().replaceAll(" ", "_"));
	}

	// #################################
	// /////////////forecast data from cnn price analysts status
	public static String cnnForecast(String stock, int methodId) {

		String httpdata = getHtmlForResults(fcstbase + stock, stock, methodId);
		if (httpdata == null)
			System.out.println("null forecast");
		if (httpdata.contains(">There is no")
				|| httpdata.contains("was not found")) {
			return "#_#_#";
		}
		String chop = "#_#";

		if (httpdata.contains(">Stock Price Forecast")) {

			chop = httpdata
					.substring(httpdata.indexOf(">Stock Price Forecast"));
			if (chop.contains("Earnings and Sales Forecasts"))
				chop = removeHtml(":"
						+ chop.substring(0,
								chop.indexOf("Earnings and Sales Forecasts")));

			if (chop.contains("The"))
				chop = chop.substring(chop.indexOf("The") + 3);
			String analysts = "#";
			String forecast = "#";
			String status = "#";// some work to do here
			try {
				if (chop.contains("analyst"))
					analysts = chop.substring(0, chop.indexOf("analyst"))
							.replaceAll("_", "");
				if (chop.contains("represents_a"))
					forecast = chop.substring(
							chop.indexOf("represents_a") + 12,
							chop.indexOf("%")).replaceAll("_", "");

				if (!chop.contains("There are no") && chop.contains("is_to")
						&& chop.contains("stock")) {
					status = chop.substring(chop.indexOf("is_to") + 5);
					status = status.substring(0, status.indexOf("stock"))
							.replaceAll("_", "");
				}
			} catch (Exception e) {
			}
			chop = analysts + "_" + forecast + "_" + status;
		}
		return spacify(chop);

	}

	// /////////////#################################
	public static String statements(String addy, String[] keys,
			boolean quarterly, String stock, int methodId) {
		String rval = "";
		int factor = 1;
		String chop = (getHtmlForResults(addy, stock, methodId));
		if (chop.contains("All numbers in thousands"))
			factor = 1000;
		if (chop.contains("All numbers in millions"))
			factor = 1000000;
		if (chop.contains("All numbers in billions"))
			factor = 1000000000;
		if (chop.contains("Period Ending")
				&& chop.contains("All rights reserved"))
			chop = chop.substring(chop.indexOf("Period Ending"),
					chop.indexOf("All rights reserved"));
		chop = reformat(chop);
		chop = removeHtml(chop).replaceAll("_", " ").replaceAll("  ", " ");

		if (chop.contains(">There is no"))
			return "#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#_#";
		for (String key : keys) {
			key = key.replaceAll("\\s", "");
			// three dangerous overlaping keys averted by skipping ahead in
			// string
			if (chop.contains("ExtraordinaryItems")
					&& key.equals("ExtraordinaryItems"))
				chop = chop.substring(chop.indexOf(key));// key between overlaps
			if (chop.contains("OtherCurrentLiabilities")
					&& key.equals("OtherCurrentLiabilities"))
				chop = chop.substring(chop.indexOf(key));// key between overlaps
			if (chop.contains("RedeemablePreferredStock")
					&& key.equals("PreferredStock"))
				chop = chop.substring(chop.indexOf(key) + 1);// actual key

			if (chop.contains(key)) {
				String datapart = chop.substring(chop.indexOf(key)
						+ key.length());
				if (datapart.indexOf("@") != -1) {
					datapart = datapart.substring(0, datapart.indexOf("@"))
							.trim();
					rval += datapart + " ";

				}
			} else {
				if (quarterly)
					rval += "# # # # ";
				if (!quarterly)
					rval += "# # # ";
			}
		}
		return spacify((rval + factor));
	}

	// ///////////replace @,^,shorten to end of table,
	public static String reformat(String input) {
		String output = input.replaceAll("@", "_").replaceAll("^", "_")
				.replaceAll("\\*", "_").replaceAll("\\s", "");
		if (input.contains("</table>"))
			output = output.substring(0, output.indexOf("</table>"));
		output = output.replaceAll("d><t", "d> <t").replaceAll("h><t", "h> <t")
				.replaceAll("d></t", "d>@</t").replaceAll("&nbsp;", "^");
		output = output.replaceAll("\\^", "");
		return output;
	}

	// ///////////// #################################
	public static String spacify(String in) {
		String inpt = in.replaceAll("_", " ").replaceAll("\\^", "")
				.replaceAll("\\*", "").replaceAll("@", "");
		while (inpt.contains("  "))
			inpt = inpt.replaceAll("  ", " ");
		return inpt.trim().replaceAll(" ", "_");
	} // ///////////// #################################

	public static int instanceCount(String in, String inst) {
		return in.split(inst).length - 1;
	}

	// #################################
	// ////////convert String data into 2d double array////////////////////////
	public static double[][] formulate(String dats801) {
		double[][] rval = new double[16][];// space for final product
		String[] res16 = dats801.split(" ");// split all into 16 groups
		if (res16.length != 16) {// every one should be 16 segments long
			System.out
					.println("                                            ## --//-- >>SIZE ERROR");
			return rval;
		}
		for (int i = 0; i < 16; i++) {
			String[] inputs = res16[i].split("_");// split each into individual
													// values
			if (inputs.length != sizes[i])
				inputs = nullset(sizes[i]).split("_");// replace with null if
														// incomplete info
			rval[i] = dataSubprocessor(inputs);
		}
		return rval;
	}

	/**
	 * Converts String data into double data removing commas, n/a's, and
	 * replacing M's, and B's with six and nine zeros respectively.
	 * 
	 * @param datain
	 *            raw data from stock data web site.
	 */
	public static double[] dataSubprocessor(String[] datain) {
		// possibly add # replacement value
		double dub = 0;
		int factor = 1;
		double[] processed = new double[datain.length];
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

			// convert sector text into a number
			for (int j = 0; j < sectors.length; j++) {
				if (data.equals(sectors[j])) {
					data = "" + j;
				}
			}
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
				dub = doDouble(dat);
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
	public static double doDouble(String value) {
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

	// ///////////// #################################
	public static String nullset(int lnth) {
		String rval = "";
		for (int i = 0; i < lnth - 1; i++)
			rval += "#_";
		return rval += "#";
	}

	// #################################///////
	// ///////////////////////////////////////
	// #################################///////
	// ///////////////////////////////////////
	// #################################///////
	// ///////////////////////////////////////
	// public static String constructData(String stock, String index) {
	// int err = 0;
	// String d1 = options(stock); // no d2
	// String d3 = competitors(stock);
	// String d4 = industry(stock); // no d5
	// String d6 = keyBasedData(yahoobase + "/ao?s=" + stock, opinionkeys);
	// String d7 = estOpinions(stock);
	// String d8 = cnnForecast(stock);
	// String d9 = keyBasedData(eodbase + index + "/" + stock + ".htm",
	// eodkeys);
	// String d10 = keyBasedData(cnnbase + stock, cnnkeys);
	// String d11 = keyBasedData(yahoobase + "?s=" + stock, genkeys);
	// String d12 = keyBasedData(yahoobase + "/ks?s=" + stock, keykeys);
	// String d14 = statements(yahoobase + "/is?s=" + stock + "&annual",
	// incomekeys, false);
	// String d15 = statements(yahoobase + "/is?s=" + stock, incomekeys, true);
	// String d16 = statements(yahoobase + "/bs?s=" + stock + "&annual",
	// balancekeys, false);
	// String d17 = statements(yahoobase + "/bs?s=" + stock, balancekeys, true);
	// String d18 = statements(yahoobase + "/cf?s=" + stock + "&annual",
	// cashkeys, false);
	// String d19 = statements(yahoobase + "/cf?s=" + stock, cashkeys, true);
	// int ct1 = (d1.split("_").length);
	// int ct3 = (d3.split("_").length);
	// int ct4 = (d4.split("_").length);
	// int ct6 = (d6.split("_").length);
	// int ct7 = (d7.split("_").length);
	// int ct8 = (d8.split("_").length);
	// int ct9 = (d9.split("_").length);
	// int ct10 = (d10.split("_").length);
	// int ct11 = (d11.split("_").length);
	// int ct12 = (d12.split("_").length);
	// int ct14 = (d14.split("_").length);
	// int ct15 = (d15.split("_").length);
	// int ct16 = (d16.split("_").length);
	// int ct17 = (d17.split("_").length);
	// int ct18 = (d18.split("_").length);
	// int ct19 = (d19.split("_").length);
	// String totaldata = d1 + " " + d3 + " " + d4 + " " + d6 + " " + d7 + " "
	// + d8 + " " + d9 + " " + d10 + " " + d11 + " " + d12 + " " + d14
	// + " " + d15 + " " + d16 + " " + d17 + " " + d18 + " " + d19;
	//
	// if (ct1 != 1)
	// System.out.print("   options ");
	// if (ct3 != 24)
	// System.out.print("   comp comp ");
	// if (ct4 != 9)
	// System.out.print("   ind rank ");
	// if (ct6 != 20)
	// System.out.print("  chg opin ");
	// if (ct7 != 128)
	// System.out.print("  estimates ");
	// if (ct8 != 3)
	// System.out.print("  cnn ftre " + d8);
	// if (ct9 != 11)
	// System.out.print("  eod ");
	// if (ct10 > 19 || ct10 < 17)
	// System.out.print("  cnn sector  ");
	// if (ct11 != 6)
	// System.out.print("  yhoo gen  >" + d11 + "<|");
	// if (ct12 != 44)
	// System.out.print("  yhoo spec  ");
	// int totalsize = totaldata.split(" ").length;
	// if (totalsize != 16)
	// err++;
	// if (totaldata.length() > 6000)
	// System.out.println("**************" + stock + "  " + totaldata);
	// System.out.println("  fields : " + err + "  " + ct1 + "  " + ct3 + "  "
	// + ct4 + "  " + ct6 + "  " + ct7 + "  " + ct8 + "  " + ct9
	// + "  " + ct10 + "  " + ct11 + "  " + ct12 + "  " + ct14 + "  "
	// + ct15 + "  " + ct16 + "  " + ct17 + "  " + ct18 + "  " + ct19
	// + " total bits: " + totaldata.length() + "  --->" + stock
	// + "   " + d8 + "  " + d9);
	// return totaldata;
	// }

	public static String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
			"Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

	public static int[] sizes = { 1, 24, 9, 20, 128, 3, 11, 17, 6, 44, 70, 93,
			103, 137, 58, 77 };

	public static String[] sectors = {
			"Commercial|Services", // 0
			"Communications",
			"Consumer|Durables",
			"Consumer|Non-Durables",
			"Consumer|Services", // 4
			"Distribution|Services",
			"Electronic|Technology",
			"Energy|Minerals",
			"Finance",
			"Health|Services", // 9
			"Health|Technology", "Industrial|Services", "Miscellaneous",
			"Non-Energy|Minerals",
			"Process|Industries",
			"Producer|Manufacturing", // 15
			"Retail|Trade", "Technology|Services", "Transportation",
			"Utilities" }; // 19

	public static String[] genkeys = { "Last Trade", "52wk Range", "Avg Vol",
			"P/E", "EPS" };// 6 values becaus 52wk h l
	public static String[] keykeys = { "Market Cap", "Enterprise Value",
			"Trailing P/E", "Forward P/E", "PEG Ratio", "Price/Sales",
			"Price/Book", "Enterprise Value/Revenue",
			"Enterprise Value/EBITDA ", "Profit Margin", "Operating Margin",
			"Return on Assets", "Return on Equity", "Revenue",
			"Revenue Per Share", "Qtrly Revenue Growth", "Gross Profit",
			"EBITDA", "Net Income Avl to Common", "Diluted EPS",
			"Qtrly Earnings Growth", "Total Cash", "Total Cash Per Share",
			"Total Debt", "Total Debt/Equity", "Current Ratio",
			"Book Value Per Share", "Operating Cash Flow",
			"Levered Free Cash Flow", "Beta", "52-Week Change",
			"50-Day Moving Average", "200-Day Moving Average",
			"Avg Vol (3 month)", "Avg Vol (10 day)", "Shares Outstanding",
			"Float", "% Held by Insiders", "% Held by Institutions",
			"Shares Short (as of", "Short Ratio (as of",
			"Short % of Float (as of", "Shares Short (prior month)",
			"Payout Ratio" };// 44 values

	public static String[] induskeys = { "Market Capitalization",
			"P/E Ratio (ttm)", "PEG Ratio (ttm, 5 yr expected)",
			"Revenue Growth (Qtrly YoY)", "EPS Growth (Qtrly YoY)",
			"Long-Term Growth Rate (5 yr)", "Return on Equity (ttm)",
			"Long-Term Debt/Equity (mrq)", "Dividend Yield (annual)" };// 9
	public static String[] competekeys = { "Market Cap", "Employees",
			"Qtrly Rev Growth", "Revenue", "Gross Margin", "EBITDA ",
			"Operating Margin", "Net Income", "EPS", "P/E", "PEG", "P/S" };// 12
																			// values
	public static String[] prokeys = { "Sector:", "Industry:",
			"Full Time Employees:", "</table><p>" };

	public static String[] cnnkeys = { "Previous close", "Volume",
			"Average volume (3 months)", "Market cap", "Dividend yield",
			"Earnings growth (last year)", "Earnings growth (this year)",
			"Earnings growth (next 5 years)", "Revenue growth (last year)",
			"P/E ratio", "Price/Sales", "Price/Book",
			"EPS forecast (this quarter)", "Annual revenue (last year)",
			"Annual profit (last year)", "Net profit margin", "Sector" };// 17
	public static String[] eodkeys = { "P/E Ratio", "PEG Ratio", "EPS",
			"DivYield", "PtB", "PtS", "EBITDA", "Shares", "Market Cap",
			"52wk range" };

	public static String[] incomekeys = {
			"Total Revenue",
			"Cost of Revenue",//
			"Gross Profit",
			"Research Development",
			"Selling General and Administrative",
			"Non Recurring",
			"Others",
			"Total Operating Expenses",
			"Operating Income or Loss",//
			"Total Other Income/Expenses Net",
			"Earnings Before Interest And Taxes",//
			"Interest Expense",
			"Income Before Tax",
			"Income Tax Expense",
			"Minority Interest",//
			"Net Income From Continuing Ops",
			"Discontinued Operations",
			"Extraordinary Items", //
			"Effect Of Accounting Changes", //
			"Other Items",
			"Net Income",// overlap
			"Preferred Stock And Other Adjustments",
			"Net Income Applicable To Common Shares" };// 23

	public static String[] balancekeys = {//
	"Cash And Cash Equivalents",
			"Short Term Investments",//
			"Net Receivables",
			"Inventory",//
			"Other Current Assets",
			"Total Current Assets",// 5
			"Long Term Investments", "Property Plant and Equipment",
			"Goodwill", // overlap ok
			"Intangible Assets",//
			"Accumulated Amortization", //
			"Other Assets", "Deferred Long Term Asset Charges", // 12
			"Total Assets", //
			"Accounts Payable", //
			"Short/Current Long Term Debt", "Other Current Liabilities",//
			"Total Current Liabilities", "Long Term Debt", // overlap
			"Other Liabilities",//
			"Deferred Long Term Liability Charges", // 20
			"Minority Interest", //
			"Negative Goodwill", //
			"Total Liabilities", //
			"Misc Stocks Options Warrants",//
			"Redeemable Preferred Stock",//
			"Preferred Stock",// overlap
			"Common Stock", // 27
			"Retained Earnings", //
			"Treasury Stock", //
			"Capital Surplus", //
			"Other Stockholder Equity", //
			"Total Stockholder Equity",// 32
			"Net Tangible Assets" };// 34 ok

	public static String[] opinionkeys = { "Strong Buy", "Buy", "Hold",
			"Underperform", "Sell" };

	// ///////////

	public static String[] cashkeys = { //
	"Net Income", //
			"Depreciation",//
			"Adjustments To Net Income",//
			"Changes In Accounts Receivables",//
			"Changes In Liabilities", //
			"Changes In Inventories",//
			"Changes In Other Operating Activities",//
			"Total Cash Flow From Operating Activities",//
			"Capital Expenditures", //
			"Investments",//
			"Other Cash flows from Investing Activities",// /
			"Total Cash Flows From Investing Activities",//
			"Dividends Paid",//
			"Sale Purchase of Stock",//
			"Net Borrowings",//
			"Other Cash Flows from Financing Activities",//
			"Total Cash Flows From Financing Activities",//
			"Effect Of Exchange Rate Changes",//
			"Change In Cash and Cash Equivalents" };// 19

	public static String[] estimatekeys = {
			"Avg. Estimate",
			"No. of Analysts",
			"Low Estimate",
			"High Estimate",
			"Year Ago EPS", // after this key substring to Revenue Est

			"Avg. Estimate",
			"No. of Analysts", // ngbf field has no data shorts rest of values
			"Low Estimate", "High Estimate", "Year Ago Sales",
			"Sales Growth (year/est)",

			"EPS Est", "EPS Actual", "Difference", "Surprise %",

			"Current Estimate", "7 Days Ago", "30 Days Ago", "60 Days Ago",
			"90 Days Ago",

			"Up Last 7 Days", "Up Last 30 Days", "Down Last 30 Days",
			"Down Last 90 Days",

			"Current Qtr.", "Next Qtr.", "This Year", "Next Year",
			"Past 5 Years (per annum)", "Next 5 Years (per annum)",
			"Price/Earnings (avg. for comparison categories)",
			"PEG Ratio (avg. for comparison categories)" };// 32
	public static String[][] allkeys = { { "optns" }, competekeys, induskeys,
			opinionkeys, { " " },
			{ "CNN Analysts", "CNN Median Forecast", "CNN recommendation" },
			eodkeys, cnnkeys, genkeys, keykeys, incomekeys, incomekeys,
			balancekeys, balancekeys, cashkeys, cashkeys };
	public static String[] sectorsinorder = {// NEEDS ORDERING
	"Commercial|Services", // 0 //-0.04
			"Communications", // -0.0068
			"Consumer|Durables", // 0.0042
			"Consumer|Non-Durables", // -0.0247
			"Consumer|Services", // 4 //-0.0244
			"Distribution|Services", // -0.0772
			"Electronic|Technology", // -0.0529
			"Energy|Minerals", // -.0375
			"Finance", // -.0185
			"Health|Services", // 9 // -.0346
			"Health|Technology", // -.0327,,,,,,,,,
			"Industrial|Services", // -.0022
			"Miscellaneous", // -.0909
			"Non-Energy|Minerals", // 0.0
			"Process|Industries", // -.0151
			"Producer|Manufacturing",// 15 // -.0643
			"Retail|Trade", // -.0011
			"Technology|Services", // -.0303
			"Transportation", // -.0266
			"Utilities" }; // 19 // -.0074

}
