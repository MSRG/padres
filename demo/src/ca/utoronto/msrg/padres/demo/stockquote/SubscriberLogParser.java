/**
 * 
 */
package ca.utoronto.msrg.padres.demo.stockquote;

/**
 * @author Alex
 *
 * Calculates the average and standard deviation of end-to-end delivery delay of multiple subscriber
 * logs for each time interval.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TreeMap;

import ca.utoronto.msrg.padres.common.util.comparator.AscendingComparator;
import ca.utoronto.msrg.padres.common.util.math.Stats;



public class SubscriberLogParser {

	private static final String LOG_FILENAME_PREFIX = "RMIUniversalClient-S";
	private static final String OUTPUT_FILENAME = "SubscriberSummary.log";
	private static final int TOKEN_COUNT_FOR_TIME = 4;
	
	// stores the list of files in /build directory.  Note, not all files are the subscriber
	// log files that we want, so filtering is needed.
	private final String[] file;
	
	// accouting variables
	private final Map<Long, Record> recordMap;
	
	/**
	 * Constructor
	 *
	 */
	public SubscriberLogParser() {
		file = new File(".").list();
		recordMap = new TreeMap<Long, Record>(new AscendingComparator());
	}

	// Pick out the subscriber log files and read line by line, extracting the useful values
	// Output results to file
	public void run() {
		for (int i = 0; i < file.length; i++) {
			String filename = file[i];
			if (filename.startsWith(LOG_FILENAME_PREFIX)) {
				tally(filename, recordMap);
			}
		}
		
		writeResultsToFile(recordMap);
	}
	
	/*
	 * reads in values of log file and put them into the record
	 * 1. open file
	 * 2. read line by line, recording values
	 */
	private void tally(String filename, Map<Long, Record> recordMap) {
		String line;
		StringTokenizer tokenizer;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filename));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return;
		}
		try {
			while ( (line = reader.readLine()) != null) {
				try {
					tokenizer = new StringTokenizer(line);
					skipHeaderFields(tokenizer);
					long time = Long.parseLong(tokenizer.nextToken());
					time = roundToNearestTens(time);
					double delay = Double.parseDouble(tokenizer.nextToken());
					
					updateRecords(time, delay, recordMap);
					
				// These exceptions only arise due to unexpected lines read.
				} catch (NoSuchElementException nsee) {
					continue;
				} catch (NumberFormatException nfe) {
					continue;
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return;
		}
			

	}
	
	private void updateRecords(long time, double delay, Map<Long, Record> recordMap) {
		if (!recordMap.containsKey(time))
			recordMap.put(time, new Record());
		
		Record record = recordMap.get(time);
		record.record(delay);
	}
	
	private static void skipHeaderFields(StringTokenizer tokenizer) throws NoSuchElementException {
		for (int i = 0; i < TOKEN_COUNT_FOR_TIME; i++)
			tokenizer.nextToken();
	}
	
	private static void writeResultsToFile(Map<Long, Record> recordMap) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(OUTPUT_FILENAME, false));
		
			for (Iterator<Long> times = recordMap.keySet().iterator(); times.hasNext();) {
				long time = times.next();
				Record record = recordMap.get(time);
				writer.write(
						time + "\t"
						+ record.getAverage() + "\t"
						+ record.getStdDev());
				writer.newLine();
			}
			
			writer.flush();
			writer.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		}
	}
	
	private long roundToNearestTens(long value) {
		return Math.round((double)value / 10.0) * 10; 
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SubscriberLogParser parser = new SubscriberLogParser();
		parser.run();
	}
	
	/*
	 * Contains the set of values recorded for a specific time
	 */
	private class Record {
		
		private final List<Double> values;
		
		public Record() {
			values = new LinkedList<Double>();
		}
		
		public void record(double value) {
			values.add(value);
		}
		
		public double getAverage() {
			return Stats.calcMean(values.toArray(new Double[values.size()]));
		}
		
		public double getStdDev() {
			return Stats.calcStdDev(values.toArray(new Double[values.size()]));
		}
		
	}

}
