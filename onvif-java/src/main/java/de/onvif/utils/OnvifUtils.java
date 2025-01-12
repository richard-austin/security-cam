package de.onvif.utils;

import org.onvif.ver10.schema.PTZPreset;
import org.onvif.ver10.schema.PTZStatus;
import org.onvif.ver10.schema.PTZVector;

public class OnvifUtils {

  public static String format(PTZVector vector) {
    String out = "";
    if (vector != null) {
      out += "[" + vector.getPanTilt().getX() + "," + vector.getPanTilt().getY();
      if (vector.getZoom() != null) out += "," + vector.getZoom().getX();
      out += "]";
    }
    return out;
  }

  public static String format(PTZPreset preset) {
    String out = "";
    if (preset != null) {
      out += preset.getToken() + "/" + preset.getName() + ":" + format(preset.getPTZPosition());
    }
    return out;
  }

  public static String format(PTZStatus status) {
    String out = "";
    if (status != null) {
      out +=
          "moveStatus="
              + format(status.getMoveStatus())
              + " position="
              + format(status.getPosition())
              + " time="
              + status.getUtcTime();
    }
    return out;
  }

  public static String format(Object o) {
    String out = "";
    if (o != null) {
      out = o.toString();
      for (; ; ) {
        int ch = out.indexOf("org.onvif.ver");
        if (ch == -1) break;
        int end = out.indexOf("[", ch);
        if (end == -1) {
          assert (false);
          break;
        } //
        int at = out.indexOf("@", ch);
        if (at == -1 || at > end) {
          assert (false);
          break;
        }

        out = out.substring(0, ch) + out.substring(end);
      }

      out = out.replaceAll("<null>", ""); // speed=<null>,foo=bar to just speed=,foo=bar

      // out += preset.getToken()+"/"+preset.getName()+":"+format(preset.getPTZPosition());
    }
    return out;
  }
}
