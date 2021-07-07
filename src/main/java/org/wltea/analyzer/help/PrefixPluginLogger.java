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
	private static final WeakHashMap<String, Marker> MARKERS = new WeakHashMap<>();
	private final Marker marker;

	PrefixPluginLogger(ExtendedLogger logger, String name, String prefix) {
		super(logger, name, null);
		String actualPrefix = prefix == null ? "" : prefix;
		WeakHashMap<String, Marker> var6 = MARKERS;
		MarkerManager.Log4jMarker actualMarker;
		synchronized (MARKERS) {
			MarkerManager.Log4jMarker maybeMarker = (MarkerManager.Log4jMarker) MARKERS.get(actualPrefix);
			if (maybeMarker == null) {
				actualMarker = new MarkerManager.Log4jMarker(actualPrefix);
				MARKERS.put(new String(actualPrefix), actualMarker);
			} else {
				actualMarker = maybeMarker;
			}
		}

		this.marker = actualMarker;
	}

	static int markersSize() {
		return MARKERS.size();
	}

	public String prefix() {
		return this.marker.getName();
	}

	@Override
	public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t) {
		assert marker == null;

		super.logMessage(fqcn, level, this.marker, message, t);
	}
}