package study_examples;

import java.awt.Color;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.Enums.Position;
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
  enum Values { DELTAMIN, DELTAMAX, DELTACLOSE, LQVOL, HQVOL, BHVOL, BLVOL, AHVOL, ALVOL, POC };

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
    DOJIALPHA, 
    DOJIBODYMAXPERC, 
    DOJIWICKMINPERC, 
    STOPVRATIO, 
    EXHRRATIO, 
    EXHMAX,
    POCGU,
    POCGD,
    DTRAP,
    UPTREND_UP,
    UPTREND_DOWN,
    DOWNTREND_UP, 
    DOWNTREND_DOWN,
    UPMARKERS,
    DOWNMARKERS,
    EMA1_PERIOD,
    EMA2_PERIOD
  };

  enum Signals {
    RISE,
    FALL,
    TRAP,
    FLIPX, // extreme flip
    FLIP,
    EXH,
    ZR,
    DIV,
    STOPV, // stopping volume
    POCG
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

    SettingGroup barColor = new SettingGroup("Trend based Bar Color");
    barColor.addRow(new IntegerDescriptor(Names.EMA1_PERIOD.toString(), "Fast EMA Period", 9, 5, 50, 1));
    barColor.addRow(new IntegerDescriptor(Names.EMA2_PERIOD.toString(), "Slow EMA Period", 45, 13, 200, 1));
    barColor.addRow(new ColorDescriptor(Names.UPTREND_UP.toString(), "Uptrend Up", new Color(0, 255, 0)));
    barColor.addRow(new ColorDescriptor(Names.UPTREND_DOWN.toString(), "Uptrend Down", new Color(0, 128, 128)));
    barColor.addRow(new ColorDescriptor(Names.DOWNTREND_UP.toString(), "Downtrend Up", new Color(165, 42, 42)));
    barColor.addRow(new ColorDescriptor(Names.DOWNTREND_DOWN.toString(), "Downtrend Down", new Color(255, 0, 0)));
    tab.addGroup(barColor);
    
    SettingGroup doji = new SettingGroup("Doji");
    doji.addRow(new IntegerDescriptor(Names.DOJIALPHA.toString(), "Color Alpha", 100, 0, 255, 1));
    doji.addRow(new IntegerDescriptor(Names.DOJIBODYMAXPERC.toString(), "Body Max %", 40, 1, 100, 1));
    doji.addRow(new IntegerDescriptor(Names.DOJIWICKMINPERC.toString(), "Wick Min %", 10, 1, 100, 1));
    tab.addGroup(doji);

    SettingGroup divergence = new SettingGroup("Divergence");
    divergence.addRow(new ColorDescriptor(Names.UPBID.toString(), "Up/@Bid Color", Color.CYAN));
    divergence.addRow(new ColorDescriptor(Names.DOWNASK.toString(), "Down/@Ask Color", Color.MAGENTA));
    divergence.addRow(new IntegerDescriptor(Names.DDIVLOOKBACK.toString(), "Lookback for Recent H/L", 5, 2, 50, 1));
    tab.addGroup(divergence);

    SettingGroup bp = new SettingGroup("B vs P Volume Profile");
    bp.addRow
      (new MarkerDescriptor(Names.BPVOL.toString(), "Marker", Enums.MarkerType.SQUARE, Enums.Size.LARGE, Color.GRAY, Color.GRAY, true, true)
      ,(new IntegerDescriptor(Names.BPVOLOFFSET.toString(), "Offset", 0, 0, 10, 1)));
    
    bp.addRow(new IntegerDescriptor(Names.BPVOLTHRESH.toString(), "Volume %", 62, 1, 100, 1));
    bp.addRow(new IntegerDescriptor(Names.BPVOLRANGE.toString(), "Within Upper/Lower %", 50, 1, 100, 1));
    tab.addGroup(bp);

    SettingGroup others = new SettingGroup("Others");
    others.addRow(new IntegerDescriptor(Names.RISEFALLBARS.toString(), "N Bars for Rise/Fall", 4, 3, 10, 1));
    others.addRow(new DoubleDescriptor(Names.STOPVRATIO.toString(), "Stopping Volume Ratio", 0.7, 0, 1, .01));
    others.addRow(new IntegerDescriptor(Names.EXHRRATIO.toString(), "Exhaustion Ratio", 30, 2, 100, 1));
    others.addRow(new IntegerDescriptor(Names.EXHMAX.toString(), "Exhaustion Max", 10, 2, 100, 1));
    others.addRow(new IntegerDescriptor(Names.DTRAP.toString(), "Delta Trap", 200, 100, 10000, 100));
    others.addRow(new MarkerDescriptor(Names.POCGU.toString(), "POC Gap Up", Enums.MarkerType.CIRCLE, Enums.Size.MEDIUM, Color.GREEN.darker(), Color.GREEN.darker(), true, true));
    others.addRow(new MarkerDescriptor(Names.POCGD.toString(), "POC Gap Down", Enums.MarkerType.CIRCLE, Enums.Size.MEDIUM, Color.RED.darker(), Color.RED.darker(), true, true));
    others.addRow(new MarkerDescriptor(Names.UPMARKERS.toString(), "Up Markers", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, Color.GREEN.darker(), Color.GREEN.darker(), true, true));
    others.addRow(new MarkerDescriptor(Names.DOWNMARKERS.toString(), "Down Markers", Enums.MarkerType.TRIANGLE, Enums.Size.SMALL, Color.RED.darker(), Color.RED.darker(), true, true));
    

    tab.addGroup(others);

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.exportValue(new ValueDescriptor(Values.DELTACLOSE, "Delta Close", null));
    desc.exportValue(new ValueDescriptor(Values.DELTAMIN, "Delta Min", null));
    desc.exportValue(new ValueDescriptor(Values.DELTAMAX, "Delta Max", null));
    desc.exportValue(new ValueDescriptor(Values.POC, "POC", null));
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

    var r = series.getRange(index);
    var body = Math.abs(close - open);
    var tail = Math.min(open, close) - low;
    var nose = high - Math.max(open, close);
    boolean doji = false;
    if (body != 0) {
      var dojiBodyThresholdPerc = getSettings().getInteger(Names.DOJIBODYMAXPERC.toString()) / 100.0;
      var dojiWickThresholdPerc = getSettings().getInteger(Names.DOJIWICKMINPERC.toString()) / 100.0;
      doji = body < (r * dojiBodyThresholdPerc) && tail > r * dojiWickThresholdPerc && nose > r * dojiWickThresholdPerc;
    }

    int barColorAlpha = doji ? getSettings().getInteger(Names.DOJIALPHA.toString(), index) : 255;
    int ema1Period = getSettings().getInteger(Names.EMA1_PERIOD.toString());
    int ema2Period = getSettings().getInteger(Names.EMA2_PERIOD.toString());
    if (index > Math.max(ema1Period, ema2Period)) {
    // if (index > smaPeriod) {
      double ema1 = series.ema(index, ema1Period, Enums.BarInput.CLOSE);
      double ema2 = series.ema(index, ema2Period, Enums.BarInput.CLOSE);
      // double trendSMA = series.sma(index, smaPeriod, Enums.BarInput.CLOSE);
      if (ema1 > ema2) {
        Color uptrendColor = upBar ? getSettings().getColor(Names.UPTREND_UP.toString()) : getSettings().getColor(Names.UPTREND_DOWN.toString());
        series.setPriceBarColor(index, new Color(uptrendColor.getRed(), uptrendColor.getGreen(), uptrendColor.getBlue(), barColorAlpha));
      } else {
        Color downtrendColor = upBar ? getSettings().getColor(Names.DOWNTREND_UP.toString()) : getSettings().getColor(Names.DOWNTREND_DOWN.toString());
        series.setPriceBarColor(index, new Color(downtrendColor.getRed(), downtrendColor.getGreen(), downtrendColor.getBlue(), barColorAlpha));
      }
    }

    float topBottomRangePerc = getSettings().getInteger(Names.BPVOLRANGE.toString())/100.0f;
    var highQuantilePrice = high - (r * topBottomRangePerc);
    var lowQuantilePrice = low + (r * topBottomRangePerc);
    var minTickSize = series.getInstrument().getTickSize();

    DeltaCalculator dc = new DeltaCalculator(highQuantilePrice, lowQuantilePrice, high, low, minTickSize);
    series.getInstrument().forEachTick(sTime, eTime, true, dc);

    series.setInt(index, Values.DELTACLOSE, dc.deltaClose);
    series.setInt(index, Values.DELTAMIN, dc.deltaMin);
    series.setInt(index, Values.DELTAMAX, dc.deltaMax);

    float poc = dc.getPOC();
    series.setFloat(index, Values.POC, poc);

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
      if ((priorDeltaClose > 0 && dc.deltaClose < 0) || (priorDeltaClose < 0 && dc.deltaClose > 0)) {
        if ((dc.deltaClose == dc.deltaMin) && priorDeltaClose == priorDeltaMax) {
          aboveSignals.append("\n");
          aboveSignals.append(priorDeltaMin == 0 ? Signals.FLIPX : Signals.FLIP);
        } else if ((dc.deltaClose == dc.deltaMax) && priorDeltaClose == priorDeltaMin) {
          belowSignals.append("\n");
          belowSignals.append(priorDeltaMax == 0 ? Signals.FLIPX : Signals.FLIP);
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
      if (index > newXLookback + 1) {
        double recentHigh = series.highest(index-1, newXLookback, Enums.BarInput.HIGH);
        double recentLow = series.lowest(index-1, newXLookback, Enums.BarInput.LOW);

        // delta divergence
        if (recentLow > low && upBar && dc.deltaClose > 0) {
          belowSignals.append("\n+");
          belowSignals.append(Signals.DIV.toString());
        } else if (recentHigh < high && downBar && dc.deltaClose < 0) {
          aboveSignals.append("\n-");
          aboveSignals.append(Signals.DIV.toString());
        }
      }

      if (upBar && poc > series.getHigh(index - 1)) {
        addFigure(new Marker(new Coordinate(sTime, poc), Position.CENTER, getSettings().getMarker(Names.POCGU.toString())));
      } else if (downBar && poc < series.getLow(index - 1)) {
        addFigure(new Marker(new Coordinate(sTime, poc), Position.CENTER, getSettings().getMarker(Names.POCGD.toString())));
      }

      double requiredGapUpOrDown = 4 * minTickSize;
      Float priorPOC = series.getFloat(index-1, Values.POC);
      if (priorPOC != null) {
        int deltaTrapMin = getSettings().getInteger(Names.DTRAP.toString());
        var anteDelta = series.getInt(index-2, Values.DELTACLOSE);
        if (anteDelta < (-1 * deltaTrapMin) && (priorDeltaClose + dc.deltaClose + anteDelta) > 0 &&
            series.getClose(index-2) < series.getOpen(index-2) &&
            series.getClose(index-1) > series.getOpen(index-1) && priorDeltaClose > 0 && 
            upBar && dc.deltaClose > 0 &&
            poc > (priorPOC + requiredGapUpOrDown)) {
          belowSignals.append("\n");
          belowSignals.append(Signals.TRAP.toString());
        } else if (anteDelta > deltaTrapMin && 
            (priorDeltaClose + dc.deltaClose + anteDelta) < 0 &&
            series.getClose(index-2) > series.getOpen(index-2) &&
            series.getClose(index-1) < series.getOpen(index-1) && priorDeltaClose < 0 &&
            downBar && dc.deltaClose < 0 &&
            poc < (priorPOC - requiredGapUpOrDown)) {
          aboveSignals.append("\n");
          aboveSignals.append(Signals.TRAP.toString());
        }
        
        if (aboveSignals.length() > 0) {
          Coordinate c = new Coordinate(sTime, high + (3 * minTickSize));
          addFigure(new Marker(c, Position.TOP, getSettings().getMarker(Names.DOWNMARKERS.toString())));
          Coordinate c2 = new Coordinate(c.getTime(), c.getValue() + (2 * minTickSize));
          Label l = new Label(c2, aboveSignals.toString());
          l.setStackPolicy(StackPolicy.HCENTER);
          addFigure(l);
        }

        if (belowSignals.length() > 0) {
          Coordinate c = new Coordinate(sTime, low - (3 * minTickSize));
          addFigure(new Marker(c, Position.BOTTOM, getSettings().getMarker(Names.UPMARKERS.toString())));
          Coordinate c2 = new Coordinate(c.getTime(), c.getValue() - minTickSize);
          Label l = new Label(c2, belowSignals.toString());
          l.setStackPolicy(StackPolicy.HCENTER);
          addFigure(l);
        }
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
