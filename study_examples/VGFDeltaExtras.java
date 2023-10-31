package study_examples;

import java.awt.Color;
import java.util.List;

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

  enum Names { UPBID, DOWNASK, MAXBARS, BPVOL, BPVOLTHRESH, MINRANGE, BPVOLRANGE, BPVOLOFFSET, ZPRNT, ZPRINTOFFST, ENABLED, RISEFALLBARS, DDIVLOOKBACK, FLIPTHRESH };

  enum Signals {
    RISE,
    FALL,
    TRAP,
    FLIPX, // extreme flip
    FLIP,
    FL, // threshold based flip
    EXH,
    ZR,
    DIV
  }

  @Override
  public void initialize(Defaults defaults)
  {
    SettingsDescriptor sd = new SettingsDescriptor();
    setSettingsDescriptor(sd);

    SettingTab tab = new SettingTab("General");
    sd.addTab(tab);

    SettingGroup colors = new SettingGroup("Display");
    colors.addRow(new BooleanDescriptor(Names.ENABLED.toString(), "Enable Operations", true));

    colors.addRow(new IntegerDescriptor(Names.MAXBARS.toString(), "Limit to Last N Bars", 20, 1, 10000, 1));
    colors.addRow(new IntegerDescriptor(Names.MINRANGE.toString(), "Minimum Bar Range (ticks)", 8, 1, 10, 1));
    
    colors.addRow(new ColorDescriptor(Names.UPBID.toString(), "Up/@Bid Color", Color.CYAN));
    colors.addRow(new ColorDescriptor(Names.DOWNASK.toString(), "Down/@Ask Color", Color.MAGENTA));
    
    colors.addRow
      (new MarkerDescriptor(Names.BPVOL.toString(), "B/P Volume", Enums.MarkerType.SQUARE, Enums.Size.MEDIUM, Color.GRAY, Color.GRAY, true, true)
      ,(new IntegerDescriptor(Names.BPVOLOFFSET.toString(), "Offset", 0, 0, 10, 1)));
    
    colors.addRow(new IntegerDescriptor(Names.BPVOLTHRESH.toString(), "B/P Volume %", 62, 1, 100, 1));
    colors.addRow(new IntegerDescriptor(Names.BPVOLRANGE.toString(), "B/P Range %", 50, 1, 100, 1));
    colors.addRow(new IntegerDescriptor(Names.RISEFALLBARS.toString(), "N Bars for Rise/Fall", 4, 3, 10, 1));
    colors.addRow(new IntegerDescriptor(Names.DDIVLOOKBACK.toString(), "Lookback for Recent H/L", 5, 2, 50, 1));
    colors.addRow(new IntegerDescriptor(Names.FLIPTHRESH.toString(), "Flip Tolerance %", 25, 1, 100, 1));

    // colors.addRow(new FontDescriptor(Names.PDELTA.toString(), "Positive Delta", new Font("Courier New", Font.BOLD, 12), Color.GREEN, true));
    // colors.addRow(new FontDescriptor(Names.NDELTA.toString(), "Positive Delta", new Font("Courier New", Font.BOLD, 12), Color.RED, true));

    tab.addGroup(colors);

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.exportValue(new ValueDescriptor(Values.DELTACLOSE, "Delta Close", null));
    desc.exportValue(new ValueDescriptor(Values.DELTAMIN, "Delta Min", null));
    desc.exportValue(new ValueDescriptor(Values.DELTAMAX, "Delta Max", null));
    desc.exportValue(new ValueDescriptor(Values.HQVOL, "Upper Vol %", null));
    desc.exportValue(new ValueDescriptor(Values.LQVOL, "Lower Vol %", null));
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

    long lowQuantileVolume = 0;
    long highQuantileVolume = 0;
    int minDelta = 0;
    int maxDelta = 0;      
    int deltaVolume = 0;
    int bidsAtHigh = 0;
    int bidsAtLow = 0;
    int asksAtHigh = 0;
    int asksAtLow = 0;

    var r = series.getRange(index);
    float topBottomRangePerc = getSettings().getInteger(Names.BPVOLRANGE.toString())/100.0f;
    var highQuantilePrice = high - (r * topBottomRangePerc);
    var lowQuantilePrice = low + (r * topBottomRangePerc);

    List<Tick> ts = series.getInstrument().getTicks(sTime, eTime);
    for (int i = 0; i < ts.size(); i++) {
      Tick t = ts.get(i);
      int tv = t.getVolume();
      boolean isAsk = t.isAskTick();
      deltaVolume += tv * (isAsk ? 1 : -1);
      highQuantileVolume += t.getPrice() > highQuantilePrice ? tv : 0;
      lowQuantileVolume += t.getPrice() < lowQuantilePrice ? tv : 0;
      minDelta = Math.min(deltaVolume, minDelta);      
      maxDelta = Math.max(deltaVolume, maxDelta);
      bidsAtHigh += t.getPrice() == high && !isAsk ? tv : 0;
      bidsAtLow += t.getPrice() == low && !isAsk ? tv : 0;
      asksAtHigh += t.getPrice() == high && isAsk ? tv : 0;
      asksAtLow += t.getPrice() == low && isAsk ? tv : 0; 
    }

    series.setInt(index, Values.DELTACLOSE, deltaVolume);
    series.setInt(index, Values.DELTAMIN, minDelta);
    series.setInt(index, Values.DELTAMAX, maxDelta);

    // series.setInt(index, Values.AHVOL, asksAtHigh);
    // series.setInt(index, Values.ALVOL, asksAtLow);
    // series.setInt(index, Values.BHVOL, bidsAtHigh);
    // series.setInt(index, Values.BLVOL, bidsAtLow);

    // even if basic information above is collected bar-by-bar
    // we compute signals only if bar is large enough
    var minimumBarRange = getSettings().getInteger(Names.MINRANGE.toString());
    var minTickSize = series.getInstrument().getTickSize();
    var largeEnoughBar = (r/minTickSize) >= minimumBarRange;
    if (largeEnoughBar) {
      var totalVolume = series.getVolume(index);
      int hqvolperc = (int)(highQuantileVolume * 100.0 / totalVolume + 0.5);
      int lqvolperc = (int)(lowQuantileVolume * 100.0 / totalVolume + 0.5);
      series.setInt(index, Values.HQVOL, hqvolperc);
      series.setInt(index, Values.LQVOL, lqvolperc);

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

      float close = series.getClose(index);
      float open = series.getOpen(index);

      Color upBidColor = getSettings().getColor(Names.UPBID.toString());
      Color downAskColor = getSettings().getColor(Names.DOWNASK.toString());
      
      boolean upBar = close > open;
      boolean downBar = close < open;

      if (upBar && deltaVolume < 0) {
        series.setPriceBarColor(index, upBidColor);
      } else if (downBar && deltaVolume > 0) {
        series.setPriceBarColor(index, downAskColor);
      }

      StringBuilder aboveSignals = new StringBuilder();
      StringBuilder belowSignals = new StringBuilder();

      // zero prints
      // N-0 on highs
      // 0-N on lows
      if (bidsAtHigh > 0 && asksAtHigh == 0) {
        aboveSignals.append("\n");
        aboveSignals.append(Signals.ZR.toString());
      }

      if (bidsAtLow == 0 && asksAtLow > 0) {
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
      if ((priorDeltaClose > 0 && deltaVolume < 0) || (priorDeltaClose < 0 && deltaVolume > 0)) {
        int flipThreshold = getSettings().getInteger(Names.FLIPTHRESH.toString());
        var boundRatio = flipThreshold / 100.0;
        var priorBoundSize = Math.abs(priorDeltaMax - priorDeltaMin) * boundRatio;
        var currentBoundSize = Math.abs(maxDelta - minDelta) * boundRatio;
        if (priorDeltaClose > (priorDeltaMax - priorBoundSize) && deltaVolume < (minDelta + currentBoundSize)) {
          aboveSignals.append("\n");
          var flipType = (priorDeltaMin == 0 ? Signals.FLIPX : ((deltaVolume == minDelta) && priorDeltaClose == priorDeltaMax) ? Signals.FLIP : Signals.FL).toString();
          aboveSignals.append(flipType);
        } else if (priorDeltaClose < (priorDeltaMin + priorBoundSize) && deltaVolume > (maxDelta - currentBoundSize)) {
          belowSignals.append("\n");
          var flipType = (priorDeltaMax == 0 ? Signals.FLIPX : ((deltaVolume == maxDelta) && priorDeltaClose == priorDeltaMin) ? Signals.FLIP : Signals.FL).toString();
          belowSignals.append(flipType);
        }
      }

      int exhaustionUpperBound = 10;
      if (close < open && asksAtHigh <= exhaustionUpperBound) {
        aboveSignals.append("\n");
        aboveSignals.append(Signals.EXH.toString());
      }

      if (open > close && bidsAtLow <= exhaustionUpperBound) {
        belowSignals.append("\n");
        belowSignals.append(Signals.EXH.toString());
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
      if (recentLow > low && upBar && deltaVolume > 0) {
        belowSignals.append("\n+");
        belowSignals.append(Signals.DIV.toString());
      } else if (recentHigh < high && downBar && deltaVolume < 0) {
        aboveSignals.append("\n-");
        aboveSignals.append(Signals.DIV.toString());
      }
      
      if (aboveSignals.length() > 0) {
        Coordinate c = new Coordinate(sTime, high + (2 * minTickSize));
        Label l = new Label(c, aboveSignals.toString());
        l.setStackPolicy(StackPolicy.HCENTER);
        addFigure(l);
      }

      if (belowSignals.length() > 0) {
        Coordinate c = new Coordinate(sTime, low - (2 * minTickSize));
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
