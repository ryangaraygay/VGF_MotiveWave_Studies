package study_examples;

import java.util.DoubleSummaryStatistics;
import com.motivewave.platform.sdk.common.DataSeries;

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

    public static String formatTwoDecimalPoints(double d) {
        return String.format("%,.2f", d);
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

    public static double Correlation(double[] xs, double[] ys) {
        if (xs == null || ys == null) return 0;
        if (xs.length == 0 || ys.length == 0) return 0;
        if (xs.length != ys.length) return 0;
    
        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double syy = 0.0;
        double sxy = 0.0;
    
        int n = xs.length;
    
        for(int i = 0; i < n; ++i) {
          double x = xs[i];
          double y = ys[i];
    
          sx += x;
          sy += y;
          sxx += x * x;
          syy += y * y;
          sxy += x * y;
        }
    
        // covariation
        double cov = sxy / n - sx * sy / n / n;
        // standard error of x
        double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
        // standard error of y
        double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);
    
        // correlation is just a normalized covariation
        return cov / sigmax / sigmay;
      }

    public static double[] getDoubleValues(DataSeries series, Object o, int index, int period) {
        double[] retVal = new double[period];
        for (int i = 1; i <= period; i++) {
            retVal[i-1] = series.getDouble(index - (period - i), o);
        }
        return retVal;
    }

    public static int[] getIntValues(DataSeries series, Object o, int index, int period) {
        int[] retVal = new int[period];
        for (int i = 1; i <= period; i++) {
            retVal[i-1] = series.getInt(index - (period - i), o);
        }
        return retVal;
    }

    public static double getEMA(double newValue, Double oldValue, double alpha) {
        double returnValue = newValue;
        if (oldValue != null) {
            returnValue = oldValue + (alpha * (returnValue - oldValue));
        }
        return returnValue;
    }

    public static Direction evaluateDirection(int[] arr) {
        int oldValue = arr[0];
        boolean decreasing = true; // assume until broken
        boolean increasing = true; // assume until broken
        int deltaSequenceCount = arr.length;
        for (int i = 1; i < deltaSequenceCount; i++) {
            int newValue = arr[i];
            System.err.println(i + " " + newValue);
            if (newValue > oldValue) {
                decreasing &= false;
            } else if (newValue < oldValue) {
                increasing &= false;
            } else if (newValue == oldValue) {
                decreasing &= false;
                increasing &= false;
            }
  
            oldValue = newValue;
            oldValue = newValue;
        }
  
        if (increasing) {
            return Direction.Rise;
        } else if(decreasing) {
            return Direction.Fall;
        } else {
            return Direction.Inconsistent;
        }
    }
}