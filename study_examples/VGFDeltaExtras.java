package study_examples;

import java.awt.Color;
import java.util.TimeZone;

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
  enum Values { DELTAMIN, DELTAMAX, DELTACLOSE, LQVOL, HQVOL, BHVOL, BLVOL, AHVOL, ALVOL, POC, AVGDMIN, AVGDMAX };

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
    STOPVRATIO, 
    EXHRRATIO, 
    EXHMAX,
    POCGU,
    POCGD,
    DTRAP,
    UPMARKERS,
    DOWNMARKERS,
    EMA1_PERIOD,
    EMA2_PERIOD,
    DCLOSERATIOUP,
    DCLOSERATIODOWN,
    EXHU,
    EXHD,
    STOPVU,
    STOPVD
  };

  enum Signals {
    RISE,
    FALL,
    TRAP,
    FLIPX, // extreme flip
    FLIP,
    EXH,
    // ZEROP,
    DIV,
    STOPV, // stopping volume
    PGAP,
    VSEQ,
    HDMAX,
    HDMIN
  }

  @Override
  public void initialize(Defaults defaults)
  {
    SettingsDescriptor sd = new SettingsDescriptor();
    setSettingsDescriptor(sd);

    SettingTab tab = new SettingTab("General");
    sd.addTab(tab);

    SettingGroup general = new SettingGroup("General");
    general.addRow(new IntegerDescriptor(Names.MAXBARS.toString(), "Limit to Last N Bars", 60, 1, 10000, 1));
    general.addRow(new IntegerDescriptor(Names.MINRANGE.toString(), "Minimum Bar Range (ticks)", 6, 1, 10, 1));
    tab.addGroup(general);

    SettingGroup divergence = new SettingGroup("Single Bar Divergence");
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
    others.addRow(new MarkerDescriptor(Names.POCGU.toString(), "POC Gap Up", Enums.MarkerType.CIRCLE, Enums.Size.LARGE, Color.GREEN.darker(), Color.GREEN.darker(), true, true));
    others.addRow(new MarkerDescriptor(Names.POCGD.toString(), "POC Gap Down", Enums.MarkerType.CIRCLE, Enums.Size.LARGE, Color.RED.darker(), Color.RED.darker(), true, true));
    others.addRow(new MarkerDescriptor(Names.EXHU.toString(), "Exhaustion Up", Enums.MarkerType.LINE_ARROW, Enums.Size.SMALL, Color.GREEN.darker(), Color.GREEN.darker(), true, true));
    others.addRow(new MarkerDescriptor(Names.EXHD.toString(), "Exhaustion Down", Enums.MarkerType.LINE_ARROW, Enums.Size.SMALL, Color.RED.darker(), Color.RED.darker(), true, true));
    others.addRow(new MarkerDescriptor(Names.STOPVU.toString(), "Stop Volume Up", Enums.MarkerType.ARROW, Enums.Size.VERY_SMALL, Color.GREEN.darker(), Color.GREEN.darker(), true, true));
    others.addRow(new MarkerDescriptor(Names.STOPVD.toString(), "Stop Volume Down", Enums.MarkerType.ARROW, Enums.Size.VERY_SMALL, Color.RED.darker(), Color.RED.darker(), true, true));
    others.addRow(new MarkerDescriptor(Names.UPMARKERS.toString(), "Up Markers", Enums.MarkerType.TRIANGLE, Enums.Size.MEDIUM, new Color(0, 255, 0), new Color(0, 255, 0), true, true));
    others.addRow(new MarkerDescriptor(Names.DOWNMARKERS.toString(), "Down Markers", Enums.MarkerType.TRIANGLE, Enums.Size.MEDIUM, new Color(255, 0, 0), new Color(255, 0, 0), true, true));
    others.addRow(new MarkerDescriptor(Names.DCLOSERATIOUP.toString(), "Delta Close Near Max", Enums.MarkerType.DIAMOND, Enums.Size.LARGE, Color.GREEN.darker(), Color.GREEN.darker(), true, true));
    others.addRow(new MarkerDescriptor(Names.DCLOSERATIODOWN.toString(), "Delta Close Near Min", Enums.MarkerType.DIAMOND, Enums.Size.LARGE, Color.RED.darker(), Color.RED.darker(), true, true));
    
    tab.addGroup(others);

    RuntimeDescriptor desc = new RuntimeDescriptor();
    setRuntimeDescriptor(desc);
    desc.exportValue(new ValueDescriptor(Values.DELTACLOSE, "Delta Close", null));
    desc.exportValue(new ValueDescriptor(Values.DELTAMIN, "Delta Min", null));
    desc.exportValue(new ValueDescriptor(Values.DELTAMAX, "Delta Max", null));
    desc.exportValue(new ValueDescriptor(Values.POC, "POC", null));
  }

  DeltaCalculator dc = null;
  long realtimeStartTime = Long.MAX_VALUE;

  @Override
  public void onTick(DataContext ctx, Tick t) {
    if (t.getTime() > realtimeStartTime) {
      dc.onTick(t);
    }
  }

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
    float high = series.getHigh(index);
    float low = series.getLow(index);
    float close = series.getClose(index);
    float open = series.getOpen(index);
    boolean upBar = close > open;
    boolean downBar = close < open;

    var r = series.getRange(index);

    if (dc == null) {
      dc = new DeltaCalculator();
    }

    BarInfo bi = dc.getBarInfo(sTime);
    if (bi == null) {
      // means we haven't computed on realtime (ontick)
      series.getInstrument().forEachTick(sTime, eTime, false, dc);
      bi = dc.getBarInfo(sTime);
      if (realtimeStartTime == Long.MAX_VALUE) {
        realtimeStartTime = series.getEndTime(series.getEndIndex()-1) + Util.MILLIS_IN_MINUTE; // start RT only a minute after last batch compute (to minimize RT partials)
      }
    }
    
    series.setInt(index, Values.DELTACLOSE, bi.deltaClose);
    series.setInt(index, Values.DELTAMIN, bi.deltaMin);
    series.setInt(index, Values.DELTAMAX, bi.deltaMax);

    float poc = bi.getPOC();
    series.setFloat(index, Values.POC, poc);

    // even if basic information above is collected bar-by-bar
    // we compute signals only if bar is large enough
    var minimumBarRange = getSettings().getInteger(Names.MINRANGE.toString());
    var minTickSize = series.getInstrument().getTickSize();
    var largeEnoughBar = (r/minTickSize) >= minimumBarRange;
    if (largeEnoughBar) {
      var totalVolume = series.getVolume(index);
      float topBottomRangePerc = getSettings().getInteger(Names.BPVOLRANGE.toString())/100.0f;
      var highQuantilePrice = high - (r * topBottomRangePerc);
      var lowQuantilePrice = low + (r * topBottomRangePerc);
      int hqvolperc = (int)(bi.getAboveVolume(highQuantilePrice) * 100.0 / totalVolume + 0.5);
      int lqvolperc = (int)(bi.getBelowVolume(lowQuantilePrice) * 100.0 / totalVolume + 0.5);

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
      if (upBar && bi.deltaClose < 0) {
        series.setPriceBarColor(index, upBidColor);
      } else if (downBar && bi.deltaClose > 0) {
        series.setPriceBarColor(index, downAskColor);
      }

      StringBuilder aboveSignals = new StringBuilder();
      StringBuilder belowSignals = new StringBuilder();

      // zero prints
      // N-0 on highs
      // 0-N on lows
      // int bidsAtHigh = bi.getBidVolume(high);
      int asksAtHigh = bi.getAskVolume(high);
      int bidsAtLow = bi.getBidVolume(low);
      // int asksAtLow = bi.getAskVolume(low);
      int bidsAtPenultimateLow = bi.getBidVolume(low + (float)minTickSize);
      int asksAtPenultimateHigh = bi.getAskVolume(high - (float)minTickSize);

      // if (bidsAtHigh > 0 && asksAtHigh == 0) {
      //   aboveSignals.append("\n");
      //   aboveSignals.append(Signals.ZEROP.toString());
      // }

      // if (bidsAtLow == 0 && asksAtLow > 0) {
      //   belowSignals.append("\n");
      //   belowSignals.append(Signals.ZEROP.toString());
      // }

      int priorDeltaClose = series.getInt(index - 1, Values.DELTACLOSE);
      int priorDeltaMin = series.getInt(index - 1, Values.DELTAMIN);
      int priorDeltaMax = series.getInt(index - 1, Values.DELTAMAX);
      
      // delta flip
      if ((priorDeltaClose > 0 && bi.deltaClose < 0) || (priorDeltaClose < 0 && bi.deltaClose > 0)) {
        if ((bi.deltaClose == bi.deltaMin) && priorDeltaClose == priorDeltaMax) {
          aboveSignals.append("\n");
          aboveSignals.append(priorDeltaMin == 0 ? Signals.FLIPX : Signals.FLIP);
        } else if ((bi.deltaClose == bi.deltaMax) && priorDeltaClose == priorDeltaMin) {
          belowSignals.append("\n");
          belowSignals.append(priorDeltaMax == 0 ? Signals.FLIPX : Signals.FLIP);
        }
      }

      // exhaustion print and stopping volume
      var stoppingVolumeRatio = getSettings().getDouble(Names.STOPVRATIO.toString());
      var exhaustionRatio = getSettings().getInteger(Names.EXHRRATIO.toString());
      int exhaustionUpperBound = getSettings().getInteger(Names.EXHMAX.toString());
      if (downBar) {
        if (asksAtHigh <= exhaustionUpperBound) {
          addFigure(new Marker(new Coordinate(sTime, high), Position.RIGHT, getSettings().getMarker(Names.EXHD.toString())));
        } else if (asksAtHigh > 0) {
          double barRatioHigh = (asksAtPenultimateHigh * 1.0)/(asksAtHigh * 1.0);
          if (barRatioHigh < stoppingVolumeRatio) {
            addFigure(new Marker(new Coordinate(sTime, high), Position.RIGHT, getSettings().getMarker(Names.STOPVD.toString())));
          } else if (barRatioHigh > exhaustionRatio) {
            addFigure(new Marker(new Coordinate(sTime, high), Position.RIGHT, getSettings().getMarker(Names.EXHD.toString())));
          }
        }
      } else if (upBar) {
        if (bidsAtLow <= exhaustionUpperBound) {
          addFigure(new Marker(new Coordinate(sTime, low), Position.LEFT, getSettings().getMarker(Names.EXHU.toString())));
        } else if (bidsAtLow > 0) {
          double barRatioLow = (bidsAtPenultimateLow * 1.0)/(bidsAtLow * 1.0);
          if (barRatioLow < stoppingVolumeRatio) {
            addFigure(new Marker(new Coordinate(sTime, low), Position.LEFT, getSettings().getMarker(Names.STOPVU.toString())));
          } else if (barRatioLow > exhaustionRatio) {
            addFigure(new Marker(new Coordinate(sTime, low), Position.LEFT, getSettings().getMarker(Names.EXHU.toString())));
          }
        }
      }

      // delta rising/falling
      int deltaSequenceCount = getSettings().getInteger(Names.RISEFALLBARS.toString());
      int[] deltas = Utils.getIntValues(series, Values.DELTACLOSE, index, deltaSequenceCount);

      Direction d = Utils.evaluateDirection(deltas);
      if (d == Direction.Rise && upBar) {
        belowSignals.append("\n");
        belowSignals.append(Signals.RISE.toString());
      } else if (d == Direction.Fall && downBar) {
        aboveSignals.append("\n");
        aboveSignals.append(Signals.FALL.toString());
      }

      int newXLookback = getSettings().getInteger(Names.DDIVLOOKBACK.toString());
      if (index > newXLookback + 1) {
        double localHigh = series.highest(index-1, newXLookback, Enums.BarInput.HIGH);
        double localLow = series.lowest(index-1, newXLookback, Enums.BarInput.LOW);

        // delta divergence
        if (localLow > low && upBar && bi.deltaClose > 0) {
          belowSignals.append("\n+");
          belowSignals.append(Signals.DIV.toString());
        } else if (localHigh < high && downBar && bi.deltaClose < 0) {
          aboveSignals.append("\n-");
          aboveSignals.append(Signals.DIV.toString());
        }
      }

      if (upBar && poc > series.getHigh(index - 1)) {
        addFigure(new Marker(new Coordinate(sTime, poc), Position.CENTER, getSettings().getMarker(Names.POCGU.toString())));
      } else if (downBar && poc < series.getLow(index - 1)) {
        addFigure(new Marker(new Coordinate(sTime, poc), Position.CENTER, getSettings().getMarker(Names.POCGD.toString())));
      }

      double requiredGapUpOrDown = 4 * minTickSize; // todo configurable poc gap in ticks required for delta trap
      Float priorPOC = series.getFloat(index-1, Values.POC);
      if (priorPOC != null) {
        int deltaTrapMin = getSettings().getInteger(Names.DTRAP.toString());
        var anteDelta = series.getInt(index-2, Values.DELTACLOSE);
        if (anteDelta < (-1 * deltaTrapMin) && (priorDeltaClose + bi.deltaClose + anteDelta) > 0 &&
            series.getClose(index-2) < series.getOpen(index-2) &&
            series.getClose(index-1) > series.getOpen(index-1) && priorDeltaClose > 0 && 
            upBar && bi.deltaClose > 0 &&
            poc > (priorPOC + requiredGapUpOrDown)) {
          belowSignals.append("\n");
          belowSignals.append(Signals.TRAP.toString());
        } else if (anteDelta > deltaTrapMin && 
            (priorDeltaClose + bi.deltaClose + anteDelta) < 0 &&
            series.getClose(index-2) > series.getOpen(index-2) &&
            series.getClose(index-1) < series.getOpen(index-1) && priorDeltaClose < 0 &&
            downBar && bi.deltaClose < 0 &&
            poc < (priorPOC - requiredGapUpOrDown)) {
          aboveSignals.append("\n");
          aboveSignals.append(Signals.TRAP.toString());
        }
      }

      // int requiredVolumeSequence = 5; // todo user configurable volume sequence (default 5)
      // VolumeSequence volumeSeq = bi.hasVolumeSequence(requiredVolumeSequence, requiredVolumeSequence);
      // if (volumeSeq == VolumeSequence.Bullish) {
      //     belowSignals.append("\n");
      //     belowSignals.append(Signals.VSEQ.toString());
      // } else if (volumeSeq == VolumeSequence.Bearish) {
      //     aboveSignals.append("\n");
      //     aboveSignals.append(Signals.VSEQ.toString());
      // }

      int deltaCloseStrength = 95; // todo make user configurable deltaclose strength (default 95)
      int deltaClosePerc = bi.getCloseRangePerc();
      if (deltaClosePerc > deltaCloseStrength) {
        Coordinate c = new Coordinate(sTime, high);
        Marker m = new Marker(c, Position.TOP_RIGHT, getSettings().getMarker(Names.DCLOSERATIOUP.toString()));
        m.setStackPolicy(StackPolicy.HCENTER);
        addFigure(m);
      } else if (deltaClosePerc < -deltaCloseStrength) {
        Coordinate c = new Coordinate(sTime, low);
        Marker m = new Marker(c, Position.BOTTOM_LEFT, getSettings().getMarker(Names.DCLOSERATIODOWN.toString()));
        m.setStackPolicy(StackPolicy.HCENTER);
        addFigure(m);
      }

      if (aboveSignals.length() > 0) {
        Coordinate c = new Coordinate(sTime, high + (2 * minTickSize));
        addFigure(new Marker(c, Position.TOP, getSettings().getMarker(Names.DOWNMARKERS.toString())));
        Coordinate c2 = new Coordinate(c.getTime(), c.getValue() + (2 * minTickSize));
        Label l = new Label(c2, aboveSignals.toString());
        l.setStackPolicy(StackPolicy.HCENTER);
        addFigure(l);
      }

      if (belowSignals.length() > 0) {
        Coordinate c = new Coordinate(sTime, low - (2 * minTickSize));
        addFigure(new Marker(c, Position.BOTTOM, getSettings().getMarker(Names.UPMARKERS.toString())));
        Coordinate c2 = new Coordinate(c.getTime(), c.getValue() - minTickSize);
        Label l = new Label(c2, belowSignals.toString());
        l.setStackPolicy(StackPolicy.HCENTER);
        addFigure(l);
      }
    }

    series.setComplete(index);

    dc.remove(sTime); // we have and will use info so we can clear for memory optimization
  }

  private void debug(long millisTime, Object... args) {
    StringBuilder sb = new StringBuilder();
    sb.append(Util.formatMMDDYYYYHHMM(millisTime, TimeZone.getTimeZone("US/Pacific")));
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
