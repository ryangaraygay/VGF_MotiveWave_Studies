package study_examples;

import java.awt.Color;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.GuideDescriptor;
import com.motivewave.platform.sdk.common.desc.IndicatorDescriptor;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.desc.SettingGroup;
import com.motivewave.platform.sdk.common.desc.SettingTab;
import com.motivewave.platform.sdk.common.desc.SettingsDescriptor;
import com.motivewave.platform.sdk.common.desc.ShadeDescriptor;
import com.motivewave.platform.sdk.common.desc.ValueDescriptor;
import com.motivewave.platform.sdk.study.*;

@StudyHeader(
 namespace="org.vgf", 
 id="VGF_PRICE_VS_DELTA", 
 name="VGF Price-Delta Convergence",
 label="VGF Price-Delta Convergence",
 desc="",
 menu="VGF Studies",
 overlay=false,
 studyOverlay=false)
public class VGFPriceVsDelta extends Study
{
  enum Values { DELTACLOSE, CLOSE, CONVDIV, HLEMA1, DCEMA1, HLEMA2, HLEMA3, DCEMA2, DCEMA3, HLTEMA, DCTEMA, CORR };

  enum Names { ENABLED, MAXBARS, CONVDIVPERIOD};

  private enum CONVDIV {
    UPCONV(10),
    DOWNCONV(-10),
    DIVERGENCE(0);

    private final int value;
    private CONVDIV(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
  }

  @Override
  public void initialize(Defaults defaults)
  {
    SettingsDescriptor sd = new SettingsDescriptor();
    setSettingsDescriptor(sd);

    SettingTab tab = new SettingTab("General");
    sd.addTab(tab);

    SettingGroup general = new SettingGroup("General");
    general.addRow(new BooleanDescriptor(Names.ENABLED.toString(), "Enable Operations", true));
    general.addRow(new IntegerDescriptor(Names.MAXBARS.toString(), "Limit to Last N Bars", 20, 1, 10000, 1));
    general.addRow(new IntegerDescriptor(Names.CONVDIVPERIOD.toString(), "Price vs Delta Conv/Div Period", 21, 5, 200, 1));
    tab.addGroup(general);

    SettingGroup display = new SettingGroup("Display");
    display.addRow(new PathDescriptor(Values.CONVDIV.toString(), "Price/Delta Convergence", Color.BLUE, 1.0f, null, true, true, false));
    var mg = new GuideDescriptor(Inputs.MIDDLE_GUIDE, get("MIDDLE_GUIDE"), CONVDIV.DIVERGENCE.getValue(), 0, 999.1, .1, true);
    mg.setDash(new float[] {3, 3});
    display.addRow(mg);
    tab.addGroup(display);

    SettingGroup shading = tab.addGroup(get("SHADING"));
    shading.addRow(new ShadeDescriptor(Inputs.TOP_FILL, get("TOP_FILL"), Inputs.MIDDLE_GUIDE, Values.CONVDIV.toString(),
        Enums.ShadeType.ABOVE, defaults.getTopFillColor(), true, true));
    shading.addRow(new ShadeDescriptor(Inputs.BOTTOM_FILL, get("BOTTOM_FILL"), Inputs.MIDDLE_GUIDE, Values.CONVDIV.toString(),
        Enums.ShadeType.BELOW, defaults.getBottomFillColor(), true, true));

    SettingGroup indicators = new SettingGroup("Indicators");
    tab.addGroup(indicators);
    indicators.addRow(new IndicatorDescriptor(Inputs.IND, "Convergence Ind", null, null, false, true, true));

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.exportValue(new ValueDescriptor(Values.DELTACLOSE, "Delta Close", null));
    desc.exportValue(new ValueDescriptor(Values.HLTEMA, "HighLow TEMA", null));
    desc.exportValue(new ValueDescriptor(Values.DCTEMA, "Delta Close TEMA", null));
    desc.exportValue(new ValueDescriptor(Values.CORR, "Correlation", null));
    desc.declareIndicator(Values.CONVDIV, Inputs.IND);
    desc.declarePath(Values.CONVDIV, Values.CONVDIV.toString());
    desc.setRangeKeys(Values.CONVDIV);
  }

  @Override
  protected void calculate(int index, DataContext ctx)
  {
    if (!getSettings().getBoolean(Names.ENABLED.toString())) return;

    DataSeries series = ctx.getDataSeries();
    int seriesSize = series.size();
    if (seriesSize == 0) return;

    int barLimit = getSettings().getInteger(Names.MAXBARS.toString());
    int startingIndex = seriesSize - barLimit - 1;
    if (index < startingIndex) return;

    long sTime = series.getStartTime(index);
    long eTime = series.getEndTime(index);

    // attempt to get exported deltaclose value
    int existingDeltaClose = series.getInt(index, Values.DELTACLOSE);
    int currentDeltaClose = 0;
    if (existingDeltaClose == 0) {
      DeltaCloseCalculator dcc = new DeltaCloseCalculator();
      series.getInstrument().forEachTick(sTime, eTime, false, dcc);
      currentDeltaClose = dcc.getDeltaClose();
      series.setInt(index, Values.DELTACLOSE, currentDeltaClose);
      series.setComplete(index, Values.DELTACLOSE);
    }

    int period = getSettings().getInteger(Names.CONVDIVPERIOD.toString());
    double alpha = 2.0/((double)period + 1);

    // Triple EMA of high-low
    Double oldHighLowEMA = series.getDouble(index - 1, Values.HLEMA1);
    double newHighLowEMA = series.getClose(index) - series.getOpen(index);
    double hlema1 = getEMA(newHighLowEMA, oldHighLowEMA, alpha);
    series.setDouble(index, Values.HLEMA1, hlema1);

    Double oldHLEMA2 = series.getDouble(index - 1, Values.HLEMA2);
    double newHLEMA2 = hlema1;
    double hlema2 = getEMA(newHLEMA2, oldHLEMA2, alpha);
    series.setDouble(index, Values.HLEMA2, hlema2);

    Double oldHLEMA3 = series.getDouble(index - 1, Values.HLEMA3);
    double newHLEMA3 = hlema2;
    double hlema3 = getEMA(newHLEMA3, oldHLEMA3, alpha);
    series.setDouble(index, Values.HLEMA3, hlema3);

    double hlTEMA = (3 * hlema1) - (3 * hlema2) + (hlema3);
    series.setDouble(index, Values.HLTEMA, hlTEMA);

    // Triple EMA of delta close
    Double oldDeltaEMA1 = series.getDouble(index - 1, Values.DCEMA1);
    double newDeltaEMA1 = currentDeltaClose;
    double deltaEMA1 = getEMA(newDeltaEMA1, oldDeltaEMA1, alpha);
    series.setDouble(index, Values.DCEMA1, deltaEMA1);

    Double oldDeltaEMA2 = series.getDouble(index - 1, Values.DCEMA2);
    double newDeltaEMA2 = deltaEMA1;
    double deltaEMA2 = getEMA(newDeltaEMA2, oldDeltaEMA2, alpha);
    series.setDouble(index, Values.DCEMA2, deltaEMA2);

    Double oldDeltaEMA3 = series.getDouble(index - 1, Values.DCEMA3);
    double newDeltaEMA3 = deltaEMA2;
    double deltaEMA3 = getEMA(newDeltaEMA3, oldDeltaEMA3, alpha);
    series.setDouble(index, Values.DCEMA3, deltaEMA3);

    double deltaTEMA = (3 * deltaEMA1) - (3 * deltaEMA2) + (deltaEMA3);
    series.setDouble(index, Values.DCTEMA, deltaTEMA);

    double corrFactor = 1;
    if (index - period > startingIndex) { // we haven't computed prior to barlimit
      double[] hltemas = getValues(series, Values.HLTEMA, index, 5);
      double[] dctemas = getValues(series, Values.DCTEMA, index, 5);
      if (hltemas != null & dctemas != null) {
        corrFactor = Utils.Correlation(hltemas, dctemas);
      }
    }
    series.setDouble(index, Values.CORR, corrFactor);

    // convergence/divergence is based on value of TEMA
    // adjusted for correlation for strength
    boolean upConvergence = hlTEMA > 0 && deltaTEMA > 0;
    boolean downConvergence = hlTEMA < 0 && deltaTEMA < 0;
    double adjustedCorrelation = (corrFactor < 0) ? 0.1 : corrFactor; // technically divergence (or low strength) but we still want to favor both price/delta TEMA on the same side
    double value = upConvergence ? CONVDIV.UPCONV.getValue() * adjustedCorrelation : (downConvergence ? CONVDIV.DOWNCONV.getValue() * adjustedCorrelation : CONVDIV.DIVERGENCE.getValue());
    series.setDouble(index, Values.CONVDIV, value);

    series.setComplete(index);
  }

  private double[] getValues(DataSeries series, Object o, int index, int period) {
    double[] retVal = new double[period];
    for (int i = 1; i <= period; i++) {
      retVal[i-1] = series.getDouble(index - (period - i), o);
    }
    return retVal;
  }

  private double getEMA(double newValue, Double oldValue, double alpha) {
    double returnValue = newValue;
    if (oldValue != null) {
      returnValue = oldValue + (alpha * (returnValue - oldValue));
    }
    return returnValue;
  }

  private class DeltaCloseCalculator implements TickOperation {
    int deltaClose = 0;
    public int getDeltaClose() {
      return deltaClose;
    }

    @Override
    public void onTick(Tick t) {
      deltaClose += t.getVolume() * (t.isAskTick() ? 1 : -1);
    }    
  }

  public void debug(long millisTime, Object... args) {
      var instance = java.time.Instant.ofEpochMilli(millisTime);
      var zonedDateTime = java.time.ZonedDateTime.ofInstant(instance,java.time.ZoneId.of("US/Pacific"));
      var formatter = java.time.format.DateTimeFormatter.ofPattern("u-M-d hh:mm:ss a O");
      var s = zonedDateTime.format(formatter);
      StringBuilder sb = new StringBuilder();
      sb.append(s);
      sb.append(" ");
      for (int i = 0; i < args.length; i++) {
      sb.append(args[i]);
      sb.append(" ");
      }

      debug(sb.toString());
  }
}

/*
mv VGF_MotiveWave_Studies.jar /Users/ryangaraygay/MotiveWave\ Extensions/
touch /Users/ryangaraygay/MotiveWave\ Extensions/.last_updated 
*/
