package study_examples;

import java.awt.Color;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.Enums.BarSizeType;
import com.motivewave.platform.sdk.common.Enums.IntervalType;
import com.motivewave.platform.sdk.common.desc.*;
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
  enum Values { DELTACLOSE2, HLEMA1, DCEMA1, HLEMA2, HLEMA3, DCEMA2, DCEMA3, HLTEMA, DCTEMA, CORR, PDTEMA, CONVDIV };

  enum Names { ENABLED, MAXBARS, CONVDIVPERIOD, PDBAR, UPCONV, DOWNCONV, UPDIV, DOWNDIV, MESSYUP, MESSYDOWN, MESSYTHRESHOLD };

  @Override
  public void initialize(Defaults defaults)
  {
    SettingsDescriptor sd = new SettingsDescriptor();
    setSettingsDescriptor(sd);

    SettingTab tab = new SettingTab("General");
    sd.addTab(tab);

    SettingGroup general = new SettingGroup("General");
    general.addRow(new IntegerDescriptor(Names.MAXBARS.toString(), "Limit to Last N Bars", 60, 1, 10000, 1));
    tab.addGroup(general);

    SettingGroup convdiv = new SettingGroup("Convergence / Divergence");
    convdiv.addRow(new IntegerDescriptor(Names.CONVDIVPERIOD.toString(), "Period", 21, 5, 200, 1));
    convdiv.addRow(new BarDescriptor(Names.PDBAR.toString(), "Bars", defaults.getBlue(), true, false));
    convdiv.addRow(new ColorDescriptor(Names.UPCONV.toString(), "Positive Convergence", new Color(0, 100, 0, 125)));
    convdiv.addRow(new ColorDescriptor(Names.DOWNCONV.toString(), "Negative Convergence", new Color(139, 0, 0, 125)));
    convdiv.addRow(new ColorDescriptor(Names.UPDIV.toString(), "Up Divergence", new Color(0, 255, 255, 125)));
    convdiv.addRow(new ColorDescriptor(Names.DOWNDIV.toString(), "Down Divergence", new Color(255, 0, 255, 125)));
    convdiv.addRow(new IntegerDescriptor(Names.MESSYTHRESHOLD.toString(), "Messy Threshold", 50, 1, 10000, 1));
    tab.addGroup(convdiv);

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.exportValue(new ValueDescriptor(Values.DELTACLOSE2, "Delta Close II", null));
    desc.exportValue(new ValueDescriptor(Values.HLTEMA, "HighLow TEMA", null));
    desc.exportValue(new ValueDescriptor(Values.DCTEMA, "Delta Close TEMA", null));
    desc.exportValue(new ValueDescriptor(Values.PDTEMA, "Price-Delta TEMA", null));
    desc.declareBars(Values.CONVDIV, Names.PDBAR.toString());
    desc.setFixedTopValue(10);
    desc.setFixedBottomValue(-10);
  }

  @Override
  public void onTick(DataContext ctx, Tick t) {
    if (t.getTime() > realtimeStartTime) {
      dcc.onTick(t);
    }
  }

  DeltaCloseCalculator dcc = null;
  long realtimeStartTime = Long.MAX_VALUE;
    
  @Override
  protected void calculate(int index, DataContext ctx)
  {
    DataSeries series = ctx.getDataSeries();
    int seriesSize = series.size();
    if (seriesSize < 2) return;
    if (!series.isBarComplete(index)) return;

    int barLimit = getSettings().getInteger(Names.MAXBARS.toString());
    int startingIndex = seriesSize - barLimit - 1;
    if (index < startingIndex) return;

    long sTime = series.getStartTime(index);
    long eTime = series.getEndTime(index);

    if (dcc == null) {
      int intervalMinutes = 1;
      BarSize barSize = ctx.getChartBarSize();
      if (barSize != null) {
        if (barSize.getType() == BarSizeType.LINEAR &&
            barSize.getIntervalType() == IntervalType.MINUTE) {
              intervalMinutes = barSize.getInterval();
        }
      }
      dcc = new DeltaCloseCalculator(intervalMinutes);
    }

    int currentDeltaClose = dcc.getDeltaClose(sTime);
    if (currentDeltaClose == 0) {
      // zero generally means we haven't computed on realtime (ontick)
      // there are cases where we did but delta is really zero but those are very rare
      // and should be ok to compute this way
      series.getInstrument().forEachTick(sTime, eTime, false, dcc);
      currentDeltaClose = dcc.getDeltaClose(sTime);
      if (realtimeStartTime == Long.MAX_VALUE) {
        realtimeStartTime = series.getEndTime(series.getEndIndex()-1) + Util.MILLIS_IN_MINUTE; // start RT only a minute after last batch compute (to minimize RT partials)
      }
    }
    
    series.setInt(index, Values.DELTACLOSE2, currentDeltaClose);
    series.setComplete(index, Values.DELTACLOSE2);

    int convdivperiod = getSettings().getInteger(Names.CONVDIVPERIOD.toString());
    double alpha = 2.0/((double)convdivperiod + 1);

    // Triple EMA of high-low
    Double oldHighLowEMA = series.getDouble(index - 1, Values.HLEMA1);
    double newHighLowEMA = series.getClose(index) - series.getOpen(index);
    double hlema1 = Utils.getEMA(newHighLowEMA, oldHighLowEMA, alpha);
    series.setDouble(index, Values.HLEMA1, hlema1);

    Double oldHLEMA2 = series.getDouble(index - 1, Values.HLEMA2);
    double newHLEMA2 = hlema1;
    double hlema2 = Utils.getEMA(newHLEMA2, oldHLEMA2, alpha);
    series.setDouble(index, Values.HLEMA2, hlema2);

    Double oldHLEMA3 = series.getDouble(index - 1, Values.HLEMA3);
    double newHLEMA3 = hlema2;
    double hlema3 = Utils.getEMA(newHLEMA3, oldHLEMA3, alpha);
    series.setDouble(index, Values.HLEMA3, hlema3);

    double hlTEMA = (3 * hlema1) - (3 * hlema2) + (hlema3);
    series.setDouble(index, Values.HLTEMA, hlTEMA);

    // Triple EMA of delta close
    Double oldDeltaEMA1 = series.getDouble(index - 1, Values.DCEMA1);
    double newDeltaEMA1 = currentDeltaClose;
    double deltaEMA1 = Utils.getEMA(newDeltaEMA1, oldDeltaEMA1, alpha);
    series.setDouble(index, Values.DCEMA1, deltaEMA1);

    Double oldDeltaEMA2 = series.getDouble(index - 1, Values.DCEMA2);
    double newDeltaEMA2 = deltaEMA1;
    double deltaEMA2 = Utils.getEMA(newDeltaEMA2, oldDeltaEMA2, alpha);
    series.setDouble(index, Values.DCEMA2, deltaEMA2);

    Double oldDeltaEMA3 = series.getDouble(index - 1, Values.DCEMA3);
    double newDeltaEMA3 = deltaEMA2;
    double deltaEMA3 = Utils.getEMA(newDeltaEMA3, oldDeltaEMA3, alpha);
    series.setDouble(index, Values.DCEMA3, deltaEMA3);

    double deltaTEMA = (3 * deltaEMA1) - (3 * deltaEMA2) + (deltaEMA3);
    series.setDouble(index, Values.DCTEMA, deltaTEMA);

    // convergence/divergence is based on magnitude and directions of TEMA
    boolean upConvergence = hlTEMA > 0 && deltaTEMA > 0;
    boolean downConvergence = hlTEMA < 0 && deltaTEMA < 0;
    int messyThreshold = getSettings().getInteger(Names.MESSYTHRESHOLD.toString());
    double pdTEMAUnsigned = Math.abs(hlTEMA * deltaTEMA);
    int sign = upConvergence ? 1 : (downConvergence ? -1 : (hlTEMA > 0 ? 1 : -1));  
    double pdTEMA = pdTEMAUnsigned * sign;
    boolean messy = pdTEMAUnsigned < messyThreshold;
    Color barColor = upConvergence ? getSettings().getColor(Names.UPCONV.toString()) 
        : downConvergence ? getSettings().getColor(Names.DOWNCONV.toString())
          : hlTEMA > 0 ? getSettings().getColor(Names.UPDIV.toString()) 
            : getSettings().getColor(Names.DOWNDIV.toString());

    boolean divergence = !upConvergence && !downConvergence;

    if (messy && !divergence) {
      barColor = barColor.darker().darker();
    }

    double value = 0;
    if (divergence) {
      if (hlTEMA > 0) value = 5;
      else value = -5;
    } else if (messy) {
      if (hlTEMA > 0) value = 2;
      else value = -2;
    } else if (upConvergence) {
      value = 10;
    } else if (downConvergence) {
      value = -10;
    }

    series.setDouble(index, Values.CONVDIV, value);
    series.setDouble(index, Values.PDTEMA, pdTEMA);
    series.setBarColor(index, Values.CONVDIV, barColor);

    series.setComplete(index);

    dcc.remove(sTime); // we have consumed the info so ok to clear for memory optimization
  }

  private void debug(long millisTime, Object... args) {
    StringBuilder sb = new StringBuilder();
    sb.append(Util.formatMMDDYYYYHHMMSS(millisTime));//, TimeZone.getTimeZone("US/Pacific")));
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
