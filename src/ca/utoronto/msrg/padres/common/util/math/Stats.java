package ca.utoronto.msrg.padres.common.util.math;

public class Stats {

	// Calculates the standard deviation
	public static Double calcStdDev(Double[] values) {
		if (values == null) 
			return null;
		
		double mean = calcMean(values);
		double sumOfSquareDiffs = 0;
		for (int i = 0; i < values.length; i++) {
			sumOfSquareDiffs += Math.pow(values[i] - mean, 2);
		}
		return new Double( Math.sqrt(sumOfSquareDiffs / (double)values.length) );
	}
	
	/**
	 * Calculates the mean
	 * @param values
	 * @return
	 */
	public static Double calcMean(Double[] values) {
		if (values == null)
			return null;
		
		double sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
		}
		
		return new Double(sum / (double)values.length);
	}
}
