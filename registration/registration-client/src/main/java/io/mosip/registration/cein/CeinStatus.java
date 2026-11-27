package io.mosip.registration.cein;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Values shown in the CEIN top bar that are computed elsewhere (e.g. the last synchronisation
 * time computed by the home page). The top bar binds to them.
 */
public final class CeinStatus {

	private static final StringProperty LAST_SYNC = new SimpleStringProperty("");

	private CeinStatus() {
	}

	public static StringProperty lastSyncProperty() {
		return LAST_SYNC;
	}

	public static void setLastSync(String value) {
		if (javafx.application.Platform.isFxApplicationThread()) {
			LAST_SYNC.set(value == null ? "" : value);
		} else {
			javafx.application.Platform.runLater(() -> LAST_SYNC.set(value == null ? "" : value));
		}
	}

	/** Two-letter initials of a person's name ("Awa Ndiaye" -> "AN"). */
	public static String initials(String name) {
		if (name == null || name.isBlank()) {
			return "";
		}
		String[] parts = name.trim().split("\\s+");
		String first = parts[0].substring(0, 1);
		String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : (parts[0].length() > 1 ? parts[0].substring(1, 2) : "");
		return (first + last).toUpperCase();
	}

	/** First given name, for the greeting of the home page. */
	public static String firstName(String name) {
		if (name == null || name.isBlank()) {
			return "";
		}
		return name.trim().split("\\s+")[0];
	}
}
