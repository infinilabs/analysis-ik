package org.wltea.analyzer.help;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

import java.util.WeakHashMap;

public class PrefixPluginLogger extends ExtendedLoggerWrapper {
    private static final WeakHashMap<String, Marker> markers = new WeakHashMap();
    private final Marker marker;

    static int markersSize() {
        return markers.size();
    }

    public String prefix() {
        return this.marker.getName();
    }

    PrefixPluginLogger(ExtendedLogger logger, String name, String prefix) {
        super(logger, name, (MessageFactory) null);
        String actualPrefix = prefix == null ? "" : prefix;
        WeakHashMap var6 = markers;
        MarkerManager.Log4jMarker actualMarker;
        synchronized (markers) {
            MarkerManager.Log4jMarker maybeMarker = (MarkerManager.Log4jMarker) markers.get(actualPrefix);
            if (maybeMarker == null) {
                actualMarker = new MarkerManager.Log4jMarker(actualPrefix);
                markers.put(new String(actualPrefix), actualMarker);
            } else {
                actualMarker = maybeMarker;
            }
        }

        this.marker = (Marker) actualMarker;
    }

    public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t) {
        assert marker == null;

        super.logMessage(fqcn, level, this.marker, message, t);
    }
}