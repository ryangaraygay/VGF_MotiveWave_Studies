package study_examples;

import java.awt.Color;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.Enums.StackPolicy;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.draw.*;
import com.motivewave.platform.sdk.study.*;

@StudyHeader(
 namespace="org.vgf", 
 id="VGF_DELTA", 
 name="VGF Delta Extras",
 label="VGF Delta Extras",
 desc="",
 menu="VGF Studies",
 overlay=true,
 studyOverlay=false)
public class VGFDeltaExtras extends Study
{
  enum Values { DELTAMIN, DELTAMAX, DELTACLOSE, LQVOL, HQVOL, BHVOL, BLVOL, AHVOL, ALVOL };

  enum Names 
  { 
    UPBID, 
    DOWNASK, 
    MAXBARS, 
    BPVOL, 
    BPVOLTHRESH, 
    MINRANGE, 
    BPVOLRANGE, 
    BPVOLOFFSET, 
    ZPRNT, 
    ZPRINTOFFST, 
    ENABLED, 
    RISEFALLBARS, 
    DDIVLOOKBACK, 
    FLIPTHRESH, 
    DOJIUP, 
    DOJIDOWN, 
    DOJIBODYMAXPERC, 
    DOJIWICKMINPERC, 
    STOPVRATIO, 
    EXHRRATIO, 
    EXHMAX 
  };

  enum Signals {
    RISE,
    FALL,
    TRAP,
    FLIPX, // extreme flip
    FLIP,
    FL, // threshold based flip
    EXH,
    ZR,
    DIV,
    STOPV // stopping volume
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
    general.addRow(new IntegerDescriptor(Names.MINRANGE.toString(), "Minimum Bar Range (ticks)", 8, 1, 10, 1));
    tab.addGroup(general);

    SettingGroup divergence = new SettingGroup("Divergence");
    divergence.addRow(new ColorDescriptor(Names.UPBID.toString(), "Up/@Bid Color", Color.CYAN));
    divergence.addRow(new ColorDescriptor(Names.DOWNASK.toString(), "Down/@Ask Color", Color.MAGENTA));
    divergence.addRow(new IntegerDescriptor(Names.DDIVLOOKBACK.toString(), "Lookback for Recent H/L", 5, 2, 50, 1));
    tab.addGroup(divergence);

    SettingGroup doji = new SettingGroup("Doji");
    doji.addRow(new ColorDescriptor(Names.DOJIUP.toString(), "Doji Up Color", new Color(178, 255, 178)));
    doji.addRow(new ColorDescriptor(Names.DOJIDOWN.toString(), "Doji Down Color", new Color(255, 178, 178)));
    doji.addRow(new IntegerDescriptor(Names.DOJIBODYMAXPERC.toString(), "Doji Body Max %", 40, 1, 100, 1));
    doji.addRow(new IntegerDescriptor(Names.DOJIWICKMINPERC.toString(), "Doji Wick Min %", 10, 1, 100, 1));
    tab.addGroup(doji);

    SettingGroup bp = new SettingGroup("B vs P Volume Profile");
    bp.addRow
      (new MarkerDescriptor(Names.BPVOL.toString(), "Marker", Enums.MarkerType.SQUARE, Enums.Size.MEDIUM, Color.GRAY, Color.GRAY, true, true)
      ,(new IntegerDescriptor(Names.BPVOLOFFSET.toString(), "Offset", 0, 0, 10, 1)));
    
    bp.addRow(new IntegerDescriptor(Names.BPVOLTHRESH.toString(), "Volume %", 62, 1, 100, 1));
    bp.addRow(new IntegerDescriptor(Names.BPVOLRANGE.toString(), "Within Upper/Lower %", 50, 1, 100, 1));
    tab.addGroup(bp);

    SettingGroup others = new SettingGroup("Others");
    others.addRow(new IntegerDescriptor(Names.RISEFALLBARS.toString(), "N Bars for Rise/Fall", 4, 3, 10, 1));
    others.addRow(new IntegerDescriptor(Names.FLIPTHRESH.toString(), "Flip Tolerance %", 5, 1, 100, 1));
    others.addRow(new DoubleDescriptor(Names.STOPVRATIO.toString(), "Stopping Volume Ratio", 0.7, 0, 1, .01));
    others.addRow(new IntegerDescriptor(Names.EXHRRATIO.toString(), "Exhaustion Ratio", 30, 2, 100, 1));
    others.addRow(new IntegerDescriptor(Names.EXHMAX.toString(), "Exhaustion Max", 10, 2, 100, 1));
    tab.addGroup(others);
    // colors.addRow(new FontDescriptor(Names.PDELTA.toString(), "Positive Delta", new Font("Courier New", Font.BOLD, 12), Color.GREEN, true));
    // colors.addRow(new FontDescriptor(Names.NDELTA.toString(), "Positive Delta", new Font("Courier New", Font.BOLD, 12), Color.RED, true));

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.exportValue(new ValueDescriptor(Values.DELTACLOSE, "Delta Close", null));
    desc.exportValue(new ValueDescriptor(Values.DELTAMIN, "Delta Min", null));
    desc.exportValue(new ValueDescriptor(Values.DELTAMAX, "Delta Max", null));
    // desc.exportValue(new ValueDescriptor(Values.HQVOL, "Upper Vol %", null));
    // desc.exportValue(new ValueDescriptor(Values.LQVOL, "Lower Vol %", null));
    // desc.exportValue(new ValueDescriptor(Values.BHVOL, "Bid High Vol", null));
    // desc.exportValue(new ValueDescriptor(Values.AHVOL, "Ask High Vol", null));
    // desc.exportValue(new ValueDescriptor(Values.BLVOL, "Bid Low Vol", null));
    // desc.exportValue(new ValueDescriptor(Values.ALVOL, "Ask Low Vol", null));
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
    float close = series.getClose(index);
    float open = series.getOpen(index);
    boolean upBar = close > open;
    boolean downBar = close < open;

    var body = Math.abs(close - open);
    var tail = Math.min(open, close) - low;
    var nose = high - Math.max(open, close);
    var r = series.getRange(index);

    if (body != 0) {
      var dojiBodyThresholdPerc = getSettings().getInteger(Names.DOJIBODYMAXPERC.toString()) / 100.0;
      var dojiWickThresholdPerc = getSettings().getInteger(Names.DOJIWICKMINPERC.toString()) / 100.0;
      if (body < (r * dojiBodyThresholdPerc) && tail > r * dojiWickThresholdPerc && nose > r * dojiWickThresholdPerc) {
        if (upBar) {
          series.setPriceBarColor(index, getSettings().getColor(Names.DOJIUP.toString()));
        } else if (downBar) {
          series.setPriceBarColor(index, getSettings().getColor(Names.DOJIDOWN.toString()));
        }
      }
    }
    
    float topBottomRangePerc = getSettings().getInteger(Names.BPVOLRANGE.toString())/100.0f;
    var highQuantilePrice = high - (r * topBottomRangePerc);
    var lowQuantilePrice = low + (r * topBottomRangePerc);
    var minTickSize = series.getInstrument().getTickSize();

    DeltaCalculator dc = new DeltaCalculator(highQuantilePrice, lowQuantilePrice, high, low, minTickSize);
    series.getInstrument().forEachTick(sTime, eTime, true, dc);
    series.getInstrument().forEachTick(ctx.getDataSeries().getStartTime(index), ctx.getDataSeries().getEndTime(index), true, myTickOperator);

    series.setInt(index, Values.DELTACLOSE, dc.deltaClose);
    series.setInt(index, Values.DELTAMIN, dc.deltaMin);
    series.setInt(index, Values.DELTAMAX, dc.deltaMax);

    // series.setInt(index, Values.AHVOL, asksAtHigh);
    // series.setInt(index, Values.ALVOL, asksAtLow);
    // series.setInt(index, Values.BHVOL, bidsAtHigh);
    // series.setInt(index, Values.BLVOL, bidsAtLow);

    // even if basic information above is collected bar-by-bar
    // we compute signals only if bar is large enough
    var minimumBarRange = getSettings().getInteger(Names.MINRANGE.toString());
    var largeEnoughBar = (r/minTickSize) >= minimumBarRange;
    if (largeEnoughBar) {
      var totalVolume = series.getVolume(index);
      int hqvolperc = (int)(dc.highQuantileVolume * 100.0 / totalVolume + 0.5);
      int lqvolperc = (int)(dc.lowQuantileVolume * 100.0 / totalVolume + 0.5);
      // series.setInt(index, Values.HQVOL, hqvolperc);
      // series.setInt(index, Values.LQVOL, lqvolperc);

      int volThreshold = getSettings().getInteger(Names.BPVOLTHRESH.toString());
      if (hqvolperc > volThreshold || lqvolperc > volThreshold) {
        MarkerInfo bpMarker = getSettings().getMarker(Names.BPVOL.toString());
        double tickOffset = minTickSize * getSettings().getInteger(Names.BPVOLOFFSET.toString());
        double p = lqvolperc > volThreshold ? low - tickOffset : high + tickOffset;
        var pos = lqvolperc > volThreshold ? Enums.Position.BOTTOM : Enums.Position.TOP;
        Coordinate c = new Coordinate(sTime, p);
        Marker m = new Marker(c, pos, bpMarker);
        addFigure(m);
      }

      // delta divergence (bar level)
      Color upBidColor = getSettings().getColor(Names.UPBID.toString());
      Color downAskColor = getSettings().getColor(Names.DOWNASK.toString());
      if (upBar && dc.deltaClose < 0) {
        series.setPriceBarColor(index, upBidColor);
      } else if (downBar && dc.deltaClose > 0) {
        series.setPriceBarColor(index, downAskColor);
      }

      StringBuilder aboveSignals = new StringBuilder();
      StringBuilder belowSignals = new StringBuilder();

      // zero prints
      // N-0 on highs
      // 0-N on lows
      if (dc.bidsAtHigh > 0 && dc.asksAtHigh == 0) {
        aboveSignals.append("\n");
        aboveSignals.append(Signals.ZR.toString());
      }

      if (dc.bidsAtLow == 0 && dc.asksAtLow > 0) {
        belowSignals.append("\n");
        belowSignals.append(Signals.ZR.toString());
      }

      int priorDeltaClose = series.getInt(index - 1, Values.DELTACLOSE);
      int priorDeltaMin = series.getInt(index - 1, Values.DELTAMIN);
      int priorDeltaMax = series.getInt(index - 1, Values.DELTAMAX);
      
      // delta flip
      // todo: better calculation of percentage so we can record actual value for fine tuning during backtest
      // ((max-close)/range)*100 upper percentage
      // (1-((min+close)/range))*100 lower percentage
      if ((priorDeltaClose > 0 && dc.deltaClose < 0) || (priorDeltaClose < 0 && dc.deltaClose > 0)) {
        int flipThreshold = getSettings().getInteger(Names.FLIPTHRESH.toString());
        var boundRatio = flipThreshold / 100.0;
        var priorBoundSize = Math.abs(priorDeltaMax - priorDeltaMin) * boundRatio;
        var currentBoundSize = Math.abs(dc.deltaMax - dc.deltaMin) * boundRatio;
        if (priorDeltaClose > (priorDeltaMax - priorBoundSize) && dc.deltaClose < (dc.deltaMin + currentBoundSize)) {
          aboveSignals.append("\n");
          var flipType = (priorDeltaMin == 0 ? Signals.FLIPX : ((dc.deltaClose == dc.deltaMin) && priorDeltaClose == priorDeltaMax) ? Signals.FLIP : Signals.FL).toString();
          aboveSignals.append(flipType);
        } else if (priorDeltaClose < (priorDeltaMin + priorBoundSize) && dc.deltaClose > (dc.deltaMax - currentBoundSize)) {
          belowSignals.append("\n");
          var flipType = (priorDeltaMax == 0 ? Signals.FLIPX : ((dc.deltaClose == dc.deltaMax) && priorDeltaClose == priorDeltaMin) ? Signals.FLIP : Signals.FL).toString();
          belowSignals.append(flipType);
        }
      }

      // exhaustion print and stopping volume
      var stoppingVolumeRatio = getSettings().getDouble(Names.STOPVRATIO.toString());
      var exhaustionRatio = getSettings().getInteger(Names.EXHRRATIO.toString());
      int exhaustionUpperBound = getSettings().getInteger(Names.EXHMAX.toString());
      if (downBar) {
        if (dc.asksAtHigh <= exhaustionUpperBound) {
          aboveSignals.append("\n");
          aboveSignals.append(Signals.EXH.toString());
        } else if (dc.asksAtHigh > 0) {
          double barRatioHigh = (dc.asksAtPenultimateHigh * 1.0)/(dc.asksAtHigh * 1.0);
          // debug(sTime, String.format("%,.2f", barRatioHigh));
          if (barRatioHigh < stoppingVolumeRatio) {
            aboveSignals.append("\n");
            aboveSignals.append(Signals.STOPV.toString());
          } else if (barRatioHigh > exhaustionRatio) {
            aboveSignals.append("\n");
            aboveSignals.append(Signals.EXH.toString());
          }
        }
      } else if (upBar) {
        if (dc.bidsAtLow <= exhaustionUpperBound) {
          belowSignals.append("\n");
          belowSignals.append(Signals.EXH.toString());
        } else if (dc.bidsAtLow > 0) {
          double barRatioLow = (dc.bidsAtPenultimateLow * 1.0)/(dc.bidsAtLow * 1.0);
          if (barRatioLow < stoppingVolumeRatio) {
            belowSignals.append("\n");
            belowSignals.append(Signals.STOPV.toString());
          } else if (barRatioLow > exhaustionRatio) {
            belowSignals.append("\n");
            belowSignals.append(Signals.EXH.toString());
          }
        }
      }

      // delta rising/falling
      int deltaSequenceCount = getSettings().getInteger(Names.RISEFALLBARS.toString());
      int[] deltas = new int[deltaSequenceCount];
      for (int i = 1; i <= deltaSequenceCount; i++) {
        deltas[i-1] = series.getInt(index - (deltaSequenceCount - i), Values.DELTACLOSE);
      }

      Direction d = evaluateDirection(deltas);
      if (d == Direction.Rise) {
        belowSignals.append("\n");
        belowSignals.append(Signals.RISE.toString());
      } else if (d == Direction.Fall) {
        aboveSignals.append("\n");
        aboveSignals.append(Signals.FALL.toString());
      }

      int newXLookback = getSettings().getInteger(Names.DDIVLOOKBACK.toString());
      double recentHigh = series.highest(index-1, newXLookback, Enums.BarInput.HIGH);
      double recentLow = series.lowest(index-1, newXLookback, Enums.BarInput.LOW);

      // delta divergence
      // todo: define large enough deltaVolume (maybe based on average and stdev but caution about extra processing so maybe static number based on analysis)
      // todo: maybe consider only if prior bar is down as well (needs testing)
      if (recentLow > low && upBar && dc.deltaClose > 0) {
        belowSignals.append("\n+");
        belowSignals.append(Signals.DIV.toString());
      } else if (recentHigh < high && downBar && dc.deltaClose < 0) {
        aboveSignals.append("\n-");
        aboveSignals.append(Signals.DIV.toString());
      }
      
      if (aboveSignals.length() > 0) {
        Coordinate c = new Coordinate(sTime, high + (3 * minTickSize));
        Label l = new Label(c, aboveSignals.toString());
        l.setStackPolicy(StackPolicy.HCENTER);
        addFigure(l);
      }

      if (belowSignals.length() > 0) {
        Coordinate c = new Coordinate(sTime, low - (3 * minTickSize));
        Label l = new Label(c, belowSignals.toString());
        l.setStackPolicy(StackPolicy.HCENTER);
        addFigure(l);
      }
    }

    series.setComplete(index);
  }

  private static Direction evaluateDirection(int[] arr) {
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

  public class DeltaCalculator implements TickOperation {

    long lowQuantileVolume = 0;
    long highQuantileVolume = 0;
    int deltaMin = 0;
    int deltaMax = 0;      
    int deltaClose = 0;
    int bidsAtHigh = 0;
    int bidsAtPenultimateLow = 0;
    int bidsAtLow = 0;
    int asksAtHigh = 0;
    int asksAtLow = 0;
    int asksAtPenultimateHigh = 0;

    private float _highQuantilePrice = 0;
    private float _lowQuantilePrice = 0;
    private float _high = 0;
    private float _low = 0;
    private double _minTickSize = 0;

    public DeltaCalculator(float hqPrice, float lqPrice, float high, float low, double minTickSize) {
      _highQuantilePrice = hqPrice;
      _lowQuantilePrice = lqPrice;
      _high = high;
      _low = low;
      _minTickSize = minTickSize;
    }

    @Override
    public void onTick(Tick t) {
      int tv = t.getVolume();
      boolean isAsk = t.isAskTick();
      deltaClose += tv * (isAsk ? 1 : -1);
      highQuantileVolume += t.getPrice() > _highQuantilePrice ? tv : 0;
      lowQuantileVolume += t.getPrice() < _lowQuantilePrice ? tv : 0;
      deltaMin = Math.min(deltaClose, deltaMin);      
      deltaMax = Math.max(deltaClose, deltaMax);
      bidsAtHigh += t.getPrice() == _high && !isAsk ? tv : 0;
      bidsAtLow += t.getPrice() == _low && !isAsk ? tv : 0;
      bidsAtPenultimateLow += t.getPrice() == (_low + _minTickSize) && !isAsk ? tv : 0;
      asksAtHigh += t.getPrice() == _high && isAsk ? tv : 0;
      asksAtPenultimateHigh += t.getPrice() == (_high - _minTickSize) && isAsk ? tv : 0; 
      asksAtLow += t.getPrice() == _low && isAsk ? tv : 0; 
    }
  }

  enum Direction { Rise, Fall, Inconsistent }

  private void debug(long millisTime, Object... args) {
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
