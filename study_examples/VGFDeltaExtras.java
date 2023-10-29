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
  enum Values { DELTAMIN, DELTAMAX, DELTACLOSE, DELTA_EXTREME, LQVOL, HQVOL, BHVOL, BLVOL, AHVOL, ALVOL };

  enum Names { UPBID, DOWNASK, MAXBARS, BPVOL, BPVOLTHRESH, MINRANGE, BPVOLRANGE, BPVOLOFFSET, ZPRNT, ZPRINTOFFST };

  @Override
  public void initialize(Defaults defaults)
  {
    SettingsDescriptor sd = new SettingsDescriptor();
    setSettingsDescriptor(sd);

    SettingTab tab = new SettingTab("General");
    sd.addTab(tab);

    SettingGroup colors = new SettingGroup("Display");
    colors.addRow(new IntegerDescriptor(Names.MAXBARS.toString(), "Limit to Last N Bars", 20, 1, 10000, 1));
    colors.addRow(new IntegerDescriptor(Names.MINRANGE.toString(), "Minimum Bar Range (ticks)", 8, 1, 10, 1));
    
    colors.addRow(new ColorDescriptor(Names.UPBID.toString(), "Up/@Bid Color", Color.CYAN));
    colors.addRow(new ColorDescriptor(Names.DOWNASK.toString(), "Down/@Ask Color", Color.MAGENTA));
    
    colors.addRow
      (new MarkerDescriptor(Names.BPVOL.toString(), "B/P Volume", Enums.MarkerType.SQUARE, Enums.Size.MEDIUM, Color.GRAY, Color.GRAY, true, true)
      ,(new IntegerDescriptor(Names.BPVOLOFFSET.toString(), "Offset", 0, 0, 10, 1)));
    
    colors.addRow(new IntegerDescriptor(Names.BPVOLTHRESH.toString(), "B/P Volume %", 62, 1, 100, 1));
    colors.addRow(new IntegerDescriptor(Names.BPVOLRANGE.toString(), "B/P Range %", 50, 1, 100, 1));
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
    desc.exportValue(new ValueDescriptor(Values.BHVOL, "Bid High Vol", null));
    desc.exportValue(new ValueDescriptor(Values.AHVOL, "Ask High Vol", null));
    desc.exportValue(new ValueDescriptor(Values.BLVOL, "Bid Low Vol", null));
    desc.exportValue(new ValueDescriptor(Values.ALVOL, "Ask Low Vol", null));
  }

  @Override
  protected void calculate(int index, DataContext ctx)
  {
    DataSeries series = ctx.getDataSeries();
    int seriesSize = series.size();
    if (seriesSize == 0) return;

    int barLimit = getSettings().getInteger(Names.MAXBARS.toString(), index);
    int startingIndex = seriesSize - barLimit - 1;

    if (index < startingIndex) return;

    var r = series.getRange(index);
    var minimumBarRange = getSettings().getInteger(Names.MINRANGE.toString(), index);
    var minTickSize = series.getInstrument().getTickSize();
    var largeEnoughBar = (r/minTickSize) >= minimumBarRange;

    if (largeEnoughBar) {
      long sTime = series.getStartTime(index);
      long eTime = series.getEndTime(index);

      float high = series.getHigh(index);
      float low = series.getLow(index);
      float topBottomRangePerc = getSettings().getInteger(Names.BPVOLRANGE.toString(), index)/100.0f;
      var highQuantilePrice = high - (r * topBottomRangePerc);
      var lowQuantilePrice = low + (r * topBottomRangePerc);

      long lowQuantileVolume = 0;
      long highQuantileVolume = 0;
      int minDelta = 0;
      int maxDelta = 0;      
      int deltaVolume = 0;
      int bidsAtHigh = 0;
      int bidsAtLow = 0;
      int asksAtHigh = 0;
      int asksAtLow = 0;
      
      List<Tick> ts = series.getInstrument().getTicks(sTime, eTime);
      for (int i = 0; i < ts.size(); i++) {
        Tick t = ts.get(i);
        boolean isAsk = t.isAskTick();
        deltaVolume += t.getVolume() * (isAsk ? 1 : -1);
        highQuantileVolume += t.getPrice() > highQuantilePrice ? t.getVolume() : 0;
        lowQuantileVolume += t.getPrice() < lowQuantilePrice ? t.getVolume() : 0;
        minDelta = Math.min(deltaVolume, minDelta);      
        maxDelta = Math.max(deltaVolume, maxDelta);
        bidsAtHigh += t.getPrice() == high && !isAsk ? t.getVolume() : 0;
        bidsAtLow += t.getPrice() == low && !isAsk ? t.getVolume() : 0;
        asksAtHigh += t.getPrice() == high && isAsk ? t.getVolume() : 0;
        asksAtLow += t.getPrice() == low && isAsk ? t.getVolume() : 0; 
      }

      var totalVolume = series.getVolume(index);
      int hqvolperc = (int)(highQuantileVolume * 100.0 / totalVolume + 0.5);
      int lqvolperc = (int)(lowQuantileVolume * 100.0 / totalVolume + 0.5);

      int volThreshold = getSettings().getInteger(Names.BPVOLTHRESH.toString(), index);
      if (hqvolperc > volThreshold || lqvolperc > volThreshold) {
        MarkerInfo bpMarker = getSettings().getMarker(Names.BPVOL.toString());
        double tickOffset = minTickSize * getSettings().getInteger(Names.BPVOLOFFSET.toString());
        double p = lqvolperc > volThreshold ? low - tickOffset : high + tickOffset;
        var pos = lqvolperc > volThreshold ? Enums.Position.BOTTOM : Enums.Position.TOP;
        Coordinate c = new Coordinate(sTime, p);
        Marker m = new Marker(c, pos, bpMarker);
        addFigure(m);
      }

      series.setInt(index, Values.DELTACLOSE, deltaVolume);
      series.setInt(index, Values.DELTAMIN, minDelta);
      series.setInt(index, Values.DELTAMAX, maxDelta);
      series.setInt(index, Values.HQVOL, hqvolperc);
      series.setInt(index, Values.LQVOL, lqvolperc);

      series.setInt(index, Values.AHVOL, asksAtHigh);
      series.setInt(index, Values.ALVOL, asksAtLow);
      series.setInt(index, Values.BHVOL, bidsAtHigh);
      series.setInt(index, Values.BLVOL, bidsAtLow);

      float close = series.getClose(index);
      float open = series.getOpen(index);

      Color upBidColor = getSettings().getColor(Names.UPBID.toString());
      Color downAskColor = getSettings().getColor(Names.DOWNASK.toString());
      
      if (close > open && deltaVolume < 0) {
        series.setPriceBarColor(index, upBidColor);
      } else if (close < open && deltaVolume > 0) {
        series.setPriceBarColor(index, downAskColor);
      }

      StringBuilder aboveSignals = new StringBuilder();
      StringBuilder belowSignals = new StringBuilder();

      // zero prints
      // N-0 on highs
      // 0-N on lows
      if (bidsAtHigh > 0 && asksAtHigh == 0) {
        aboveSignals.append("\nZPD");
      }

      if (bidsAtLow == 0 && asksAtLow > 0) {
        belowSignals.append("\nZPU");
      }

      // delta flip (uncommon so defer for now - refine it to be within thresholds rather than actual extremes)
      // maybe abs(d_close-close[1]) is > 90% of lowest(d_min,d_min[1]) to highest(d_max, d_max[1])
      boolean closeAtMaxDelta = (deltaVolume == maxDelta);
      boolean closeAtMinDelta = (deltaVolume == minDelta);
      if(closeAtMaxDelta || closeAtMinDelta) {
        series.setInt(index, Values.DELTA_EXTREME, closeAtMaxDelta ? 2 : 1);
        int priorDeltaExtreme = series.getInt(index-1, Values.DELTA_EXTREME);
        if ((closeAtMaxDelta && priorDeltaExtreme == 1) || (closeAtMinDelta && priorDeltaExtreme == 2)) {    
          aboveSignals.append("\nDEF");
        }
      }

      int exhaustionUpperBound = 10;
      if (close < open && asksAtHigh <= exhaustionUpperBound) {
        aboveSignals.append("\nEPD");
      }

      if (open > close && bidsAtLow <= exhaustionUpperBound) {
        belowSignals.append("\nEPU");
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
