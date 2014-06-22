package harlequinmettle.gaero;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

public class StatInfo implements DBLabels{
	public int ID;
	public int repaintCounter = 0;
	public static final int SIDE_BUFFER = 50;
	public static final int drag_proximity = 20;
	float sum, sumOfSquares, mean, median, min, max, n, range;

	int[] histogram;

	float emin, emax, percentile = 2;

	float interval;

	public float standardDeviation;

	float graphicsScale = 1.0f;
	float frameH, frameW;
	int maxBar = Integer.MIN_VALUE;
	int secondToMaxBar = Integer.MIN_VALUE;

	private static final int BAR_BUFFER = 2;

	static final int DONT_SHOW = 10001000;

	static int nbars = 70;
	float barwidth = 10;
	float dataQuality = 0;

	public boolean varsSet = false;
	public boolean usingMax = true;
	public int nullCt = 0;
	String title = "untitled";

	// add as many background graphs as is appropriate - map scalefactor
	// (changeable) to interval mapping
	TreeMap<Float, Float> marketComparison = new TreeMap<Float, Float>();
	// preserve link to marketchanges
	TreeMap<Float, Float> marketchange;

	public float xLim, nLim;
	public float gmax, gmin, gmaxVert, gminVert;
	public float graphInterval;
	// list of mappings of bundle sizes - for each date id->bundle size
	ArrayList<TreeMap<Float, Integer>> bSizes = new ArrayList<TreeMap<Float, Integer>>();
	ArrayList<TreeMap<Float, TwoNumbers>> bSizesChanges = new ArrayList<TreeMap<Float, TwoNumbers>>();
	HashMap<TwoNumbers, Integer> displaySizes = new HashMap<TwoNumbers, Integer>();
	HashMap<TwoNumbers, TwoNumbers> displaySetChanges = new HashMap<TwoNumbers, TwoNumbers>();

	ArrayList<TreeMap<Float, Float>> percentChanges = new ArrayList<TreeMap<Float, Float>>();

	// load with stats for bundles that we are using - qualify based on set
	// category limits
	StatInfo subsetStats, beatMarket, marketBeat;
	boolean setXlim = false;
	boolean setNlim = false;

	static final String[] LABELS = concat(labels, COMPUTED);
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TITLE: " + title + "\nsum: " + sum + "\nsum of sq: "
				+ sumOfSquares + "\nmin: " + min + "\nmax: " + max  + "\nemin: " + emin + "\nemax: " + emax + "\nmean: "
				+ mean + "\nmedian: " + median + "\nstandard deviation: "
				+ standardDeviation + "\nhistogram: "
				+ Arrays.toString(histogram) + "\ninterval: " + interval
				+ "\ngraphicsScale: " + graphicsScale + "\nframeH: " + frameH
				+ "\nframeW: " + frameW + "\nmaxBar: " + maxBar
				+ "\nbarwidth: " + barwidth + "\ndataquality: " + dataQuality
				+ "\nnullCt: " + nullCt

		;
	}

	public StatInfo(ArrayList<Float> stats, int id) {

		ID = id;
		xLim = frameW - SIDE_BUFFER;
		nLim = SIDE_BUFFER;
		doStatsOnList(stats);
		calculateHistogramEffective(stats);

		varsSet = true;
		title =  LABELS[id];

	}

	public void doStatsOnList(ArrayList<Float> data) {
		ArrayList<Float> medianFinder = new ArrayList<Float>();
		for (float dataPoint : data) {

			if (dataPoint != dataPoint)
				continue;

			n++;
			sum += dataPoint;
			sumOfSquares += dataPoint * dataPoint;
			medianFinder.add(dataPoint);
		}
		mean = sum / n;
		if (medianFinder.size() < 1)
			return;
		Collections.sort(medianFinder);
		median = ((medianFinder.get((int) (n / 2)) + medianFinder
				.get((int) ((n - 1) / 2))) / 2);
		min = medianFinder.get(0);
		max = medianFinder.get(medianFinder.size() - 1);
		// mode = findMode(medianFinder);

		standardDeviation = (float) Math.sqrt((sumOfSquares - sum * sum / n)
				/ n);
		range = max - min;

		dataQuality = new BigDecimal(n / data.size()).round(new MathContext(3))
				.floatValue();
		float lowThird = (mean - percentile * standardDeviation);
		emin = lowThird < min ? min : lowThird;
		float highThird = (mean + percentile * standardDeviation);
		emax = highThird > max ? max : highThird;
		// round our numbers to 4 sig fig
		BigDecimal bd = new BigDecimal(emin);
		bd = bd.round(new MathContext(4));
		emin = bd.floatValue();
		bd = new BigDecimal(emax);
		bd = bd.round(new MathContext(4));
		emax = bd.floatValue();

	}

	public int locationInHistogram(float f) {
		return (int) ((f - (emin)) / (interval)); 
	}

	public void calculateHistogramEffective(ArrayList<Float> _data) {
		// ///////////////////////////////////

		range = emax - emin;
		interval = range / nbars;

		// //////////////////////////////////////
		int[] histogram = new int[nbars];
		for (float dataPt : _data) {
			if (dataPt != dataPt)
				continue;
			int histo_pt = (int) ((dataPt - (emin)) / (interval));
			if (histo_pt >= 0 && histo_pt < nbars)
				histogram[histo_pt]++;
		}

		for (int i : histogram) {
			if (i > maxBar) {

				maxBar = i;
			}
		}
		for (int i : histogram) {
			if (i > secondToMaxBar && i != maxBar) {
				secondToMaxBar = i;
			}
		}
		if (maxBar > 1.3 * secondToMaxBar) {
			usingMax = false;
			// maxBar = secondToMaxBar;
		}
		this.histogram = histogram;
	}

	public int[] calculateHistogramEffective(ArrayList<Float> _data,
			float overrideMin, float overrideMax) {
		// ///////////////////////////////////

		range = overrideMax - overrideMin;
		interval = range / nbars;

		// //////////////////////////////////////
		int[] histogram = new int[nbars];
		for (float dataPt : _data) {
			if (dataPt != dataPt)
				continue;
			int histo_pt = (int) ((dataPt - (overrideMin)) / (interval));
			if (histo_pt >= 0 && histo_pt < nbars)
				histogram[histo_pt]++;
		}

		for (int i : histogram) {
			if (i > maxBar) {

				maxBar = i;
			}
		}
		for (int i : histogram) {
			if (i > secondToMaxBar && i != maxBar) {
				secondToMaxBar = i;
			}
		}
		if (maxBar > 1.3 * secondToMaxBar) {
			usingMax = false;
			// maxBar = secondToMaxBar;
		}
		this.histogram = histogram;
		return histogram;
	}

	public static float findMinimum(Collection<Float> values) {
		float MIN = Float.POSITIVE_INFINITY;

		for (float f : values)
			if (f < MIN)
				MIN = f;

		return MIN;
	}

	public static float findMaximum(Collection<Float> values) {
		float MAX = Float.NEGATIVE_INFINITY;
		for (float f : values)
			if (f > MAX)
				MAX = f;

		return MAX;
	}

	static float[][][] concat3d(float[][][] A, float[][][] B) {
		int aLen = A.length;
		int bLen = B.length;
		float[][][] C = new float[aLen + bLen][][];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}

	static float[][] concat2d(float[][] A, float[][] B) {
		int aLen = A.length;
		int bLen = B.length;
		float[][] C = new float[aLen + bLen][];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}

	float[] concat(float[] A, float[] B) {
		int aLen = A.length;
		int bLen = B.length;
		float[] C = new float[aLen + bLen];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}

	static String[] concat(String[] A, String[] B) {
		int aLen = A.length;
		int bLen = B.length;
		String[] C = new String[aLen + bLen];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}

}
