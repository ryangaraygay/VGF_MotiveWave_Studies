package study_examples;

import java.util.DoubleSummaryStatistics;

public class Utils {
    
    public static double getAverage(double[] values) {
        DoubleSummaryStatistics stats2 = new DoubleSummaryStatistics();
        for (double d : values) {
            stats2.accept(d);
        }
        return stats2.getAverage();
    }

    public static double[] cumulative(double[] values) {
        double[] r = new double[values.length];
        r[0] = values[0];
        for (int i = 1; i < values.length; i++) {
            r[i] = values[i] + r[i-1];
        }
        return r;
    }

    public static double[] zscore(double[] values) {
        // double variance = StatUtils.populationVariance(values);
        // double sd = Math.sqrt(variance);
        double sd = calculateStandardDeviation(values);
        double mean = mean(values);
        double[] r = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            r[i] = (value-mean)/sd;
        }

        return r;
    }

    public static double mean(double[] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }

    public static double calculateStandardDeviation(double[] array) {

        // get the sum of array
        double sum = 0.0;
        for (double i : array) {
            sum += i;
        }
    
        // get the mean of array
        int length = array.length;
        double mean = sum / length;
    
        // calculate the standard deviation
        double standardDeviation = 0.0;
        for (double num : array) {
            standardDeviation += Math.pow(num - mean, 2);
        }
    
        return Math.sqrt(standardDeviation / length);
    }
}
