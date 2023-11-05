package study_examples;

import java.awt.Color;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.BarDescriptor;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.GuideDescriptor;
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
 name="VGF Price vs Delta",
 label="VGF Price vs Delta",
 desc="",
 menu="VGF Studies",
 overlay=false,
 studyOverlay=false)
public class VGFPriceVsDelta extends Study
{
  enum Values { DELTACLOSE, CLOSE, CONVDIV, HLEMA1, DCEMA1, HLEMA2, HLEMA3, DCEMA2, DCEMA3, HLTEMA, DCTEMA };

  enum Names { ENABLED, MAXBARS, CONVDIVPERIOD};

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

    SettingGroup colors = new SettingGroup("Display");
    // colors.addRow(new BarDescriptor(Values.CONVDIV.toString(), "Price/Delta Convergence", Color.BLUE, true, true));
    colors.addRow(new PathDescriptor(Values.CONVDIV.toString(), "Price/Delta Convergence", Color.BLUE, 1.0f, null, true, true, false));
    // colors.addRow(new PathDescriptor(Values.HLTEMA.toString(), "HighLow TEMA", Color.BLUE, 1.0f, null, true, true, false));
    // colors.addRow(new PathDescriptor(Values.DCTEMA.toString(), "Delta Close TEMA", Color.BLACK, 1.0f, null, true, true, false));
    // colors.addRow(new BarDescriptor(Values.DCTEMA.toString(), "Delta Close TEMA", Color.PINK, true, true));

    var mg = new GuideDescriptor(Inputs.MIDDLE_GUIDE, get("MIDDLE_GUIDE"), 0, 0, 999.1, .1, true);
    mg.setDash(new float[] {3, 3});
    colors.addRow(mg);

    tab.addGroup(colors);

    SettingGroup grp = tab.addGroup(get("SHADING"));
    grp.addRow(new ShadeDescriptor(Inputs.TOP_FILL, get("TOP_FILL"), Inputs.MIDDLE_GUIDE, Values.CONVDIV.toString(),
        Enums.ShadeType.ABOVE, defaults.getTopFillColor(), true, true));
    grp.addRow(new ShadeDescriptor(Inputs.BOTTOM_FILL, get("BOTTOM_FILL"), Inputs.MIDDLE_GUIDE, Values.CONVDIV.toString(),
        Enums.ShadeType.BELOW, defaults.getBottomFillColor(), true, true));

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.exportValue(new ValueDescriptor(Values.DELTACLOSE, "Delta Close", null));
    desc.exportValue(new ValueDescriptor(Values.CONVDIV, "Con/Di-vergence", null));
    // desc.exportValue(new ValueDescriptor(Values.HLEMA1, "H/L EMA1", null));
    // desc.exportValue(new ValueDescriptor(Values.HLEMA2, "H/L EMA2", null));
    // desc.exportValue(new ValueDescriptor(Values.HLEMA3, "H/L EMA3", null));
    // desc.exportValue(new ValueDescriptor(Values.DCEMA1, "Delta Close EMA1", null));
    // desc.exportValue(new ValueDescriptor(Values.DCEMA2, "Delta Close EMA2", null));
    // desc.exportValue(new ValueDescriptor(Values.DCEMA3, "Delta Close EMA3", null));
    desc.exportValue(new ValueDescriptor(Values.HLTEMA, "HighLow TEMA", null));
    desc.exportValue(new ValueDescriptor(Values.DCTEMA, "Delta Close TEMA", null));
    // desc.declarePath(Values.HLTEMA, Values.HLTEMA.toString());
    // desc.declarePath(Values.DCTEMA, Values.DCTEMA.toString());
    desc.declarePath(Values.CONVDIV, Values.CONVDIV.toString());
    // desc.declareBars(Values.CONVDIV, Values.CONVDIV.toString());
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
    float high = series.getHigh(index);
    float low = series.getLow(index);

    var r = series.getRange(index);

    float topBottomRangePerc = 50;
    var highQuantilePrice = high - (r * topBottomRangePerc);
    var lowQuantilePrice = low + (r * topBottomRangePerc);
    var minTickSize = series.getInstrument().getTickSize();

    DeltaCalculator dc = new DeltaCalculator(highQuantilePrice, lowQuantilePrice, high, low, minTickSize);
    series.getInstrument().forEachTick(sTime, eTime, false, dc);

    series.setInt(index, Values.DELTACLOSE, dc.deltaClose);

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
    double newDeltaEMA1 = dc.deltaClose;
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

    // convergence/divergence is based on value of TEMA
    boolean upConvergence = hlTEMA > 0 && deltaTEMA > 0;
    boolean downConvergence = hlTEMA < 0 && deltaTEMA < 0;
    int value = upConvergence ? 10 : (downConvergence ? -10 : 0);
    series.setInt(index, Values.CONVDIV, value);

    // // as an alternative, con-div is based on change in TEMA (or maybe even slope - though adds more complexity)
    // Double priorHLTEMAx = series.getDouble(index-1, Values.HLTEMA);
    // Double priorDCTEMAx = series.getDouble(index-1, Values.DCTEMA);
    // if (priorHLTEMAx != null && priorDCTEMAx != null) {
    //   boolean hltemaUp = hlTEMA > priorHLTEMAx;
    //   boolean dctemaUp = deltaTEMA > priorDCTEMAx;
    //   boolean upConvergence = hltemaUp && dctemaUp;
    //   boolean downConvergence = !hltemaUp && !dctemaUp; // todo consider flat as well though unlikely
    //   int value = upConvergence ? 10 : (downConvergence ? -10 : 0);
    //   series.setInt(index, Values.CONVDIV, value);
    // }

    series.setComplete(index);
  }

  public double getEMA(double newValue, Double oldValue, double alpha) {
    double returnValue = newValue;
    if (oldValue != null) {
      returnValue = oldValue + (alpha * (returnValue - oldValue));
    }
    return returnValue;
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

// add(double x) {
//     total += x;
//     addToQueue(x);
//     if (queueSize > 50) {
//         total -= removeLastFromQueue();
//     } else {
//         count++;
//     }
// }
// double getAverage() {
//     return total / count;
// }

/*
mv VGF_MotiveWave_Studies.jar /Users/ryangaraygay/MotiveWave\ Extensions/
touch /Users/ryangaraygay/MotiveWave\ Extensions/.last_updated 
*/
