package study_examples;

import java.awt.Color;
import java.awt.Font;

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

  enum Names { ENABLED, MAXBARS, CONVDIVPERIOD, CORRPERIOD };

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
    general.addRow(new IntegerDescriptor(Names.MAXBARS.toString(), "Limit to Last N Bars", 60, 1, 10000, 1));
    tab.addGroup(general);

    SettingGroup convdiv = new SettingGroup("Convergence / Divergence");
    convdiv.addRow(new IntegerDescriptor(Names.CONVDIVPERIOD.toString(), "Period", 21, 5, 200, 1));
    convdiv.addRow(new PathDescriptor(Values.CONVDIV.toString(), "Path", Color.GRAY, 0.25f, null, true, true, false));
    var mg = new GuideDescriptor(Inputs.MIDDLE_GUIDE, get("MIDDLE_GUIDE"), CONVDIV.DIVERGENCE.getValue(), 0, 999.1, .1, true);
    mg.setDash(new float[] {3, 3});
    mg.setTextColor(Color.LIGHT_GRAY);
    mg.setWidth(0.25f);
    convdiv.addRow(mg);
    convdiv.addRow(new ShadeDescriptor(Inputs.TOP_FILL, "Positive Fill", Inputs.MIDDLE_GUIDE, Values.CONVDIV.toString(), Enums.ShadeType.ABOVE, defaults.getTopFillColor(), true, true));
    convdiv.addRow(new ShadeDescriptor(Inputs.BOTTOM_FILL, "Negative Fill", Inputs.MIDDLE_GUIDE, Values.CONVDIV.toString(), Enums.ShadeType.BELOW, defaults.getBottomFillColor(), true, true));
    convdiv.addRow(new IndicatorDescriptor(Inputs.IND, "Scale Marker", Color.WHITE, Color.GRAY, new Font("Courier", Font.BOLD, 16), true, null, 0.25f, null, false, true, true));
    tab.addGroup(convdiv);

    SettingGroup corr = new SettingGroup("Correlation / Strength");
    corr.addRow(new IntegerDescriptor(Names.CORRPERIOD.toString(), "Period", 5, 3, 200, 1));
    corr.addRow(new PathDescriptor(Values.CORR.toString(), "Path", Color.BLUE, 2.0f, new float[] {3, 3}, true, true, false));
    corr.addRow(new IndicatorDescriptor(Inputs.STRENGTH, "Scale Marker", Color.WHITE, Color.BLUE, new Font("Courier", Font.BOLD, 16), true, null, 0.25f, null, false, true, true));
    tab.addGroup(corr);

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.exportValue(new ValueDescriptor(Values.DELTACLOSE, "Delta Close", null));
    desc.exportValue(new ValueDescriptor(Values.HLTEMA, "HighLow TEMA", null));
    desc.exportValue(new ValueDescriptor(Values.DCTEMA, "Delta Close TEMA", null));
    desc.exportValue(new ValueDescriptor(Values.CORR, "Correlation", null));
    desc.declareIndicator(Values.CONVDIV, Inputs.IND);
    desc.declareIndicator(Values.CORR, Inputs.STRENGTH);
    desc.declarePath(Values.CONVDIV, Values.CONVDIV.toString());
    desc.declarePath(Values.CORR, Values.CORR.toString());
    desc.setFixedTopValue(CONVDIV.UPCONV.getValue());
    desc.setFixedBottomValue(CONVDIV.DOWNCONV.getValue());
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

    double corrFactor = 1;
    int corrPeriod = getSettings().getInteger(Names.CORRPERIOD.toString());
    if (index - convdivperiod > startingIndex) { // we haven't computed prior to barlimit
      double[] hltemas = Utils.getDoubleValues(series, Values.HLTEMA, index, corrPeriod);
      double[] dctemas = Utils.getDoubleValues(series, Values.DCTEMA, index, corrPeriod);
      if (hltemas != null & dctemas != null) {
        corrFactor = Utils.Correlation(hltemas, dctemas);
      }
    }
    series.setDouble(index, Values.CORR, corrFactor * CONVDIV.UPCONV.getValue());

    // convergence/divergence is based on value of TEMA
    boolean upConvergence = hlTEMA > 0 && deltaTEMA > 0;
    boolean downConvergence = hlTEMA < 0 && deltaTEMA < 0;
    double value = upConvergence ? CONVDIV.UPCONV.getValue() : (downConvergence ? CONVDIV.DOWNCONV.getValue() : CONVDIV.DIVERGENCE.getValue());
    series.setDouble(index, Values.CONVDIV, value);

    series.setComplete(index);
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
