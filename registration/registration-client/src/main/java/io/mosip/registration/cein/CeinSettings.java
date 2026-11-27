package io.mosip.registration.cein;

import io.mosip.registration.context.ApplicationContext;

/**
 * Single entry point for every CEIN-specific setting of the registration client.
 *
 * <p>Values are read from the MOSIP application map (global parameters synced from the
 * config server, overridden by local preferences), so each key can be set in
 * {@code registration-default.properties} on the config server. When a key is absent the
 * default below applies; the defaults reproduce the behaviour of the CEIN build.</p>
 */
public final class CeinSettings {

	/** Language code forced for the UI and for registrations. Blank = standard MOSIP behaviour. */
	public static final String FORCED_LANGUAGE = "mosip.registration.cein.forced-language";
	public static final String DEFAULT_FORCED_LANGUAGE = "fra";

	/** When true, operators without the DEFAULT role are not sent to the onboarding screen. */
	public static final String SKIP_ONBOARDING_NON_DEFAULT = "mosip.registration.cein.skip-onboarding-for-non-default-users";

	/** Enables the "Parcourir" (load from disk) button of the document scan pop-up. */
	public static final String DOC_BROWSE_ENABLED = "mosip.registration.cein.doc-browse.enabled";
	/** Largest file accepted by the "Parcourir" button, in kilobytes. */
	public static final String DOC_BROWSE_MAX_SIZE_KB = "mosip.registration.cein.doc-browse.max-size-kb";
	/** Longest side, in pixels, an imported image is scaled down to. */
	public static final String DOC_BROWSE_MAX_DIMENSION_PX = "mosip.registration.cein.doc-browse.max-dimension-px";

	/** Acknowledgement receipt paper: "A4" (standard MOSIP) or the roll width in mm (58 or 80). */
	public static final String RECEIPT_PAPER = "mosip.registration.cein.receipt.paper";
	public static final String DEFAULT_RECEIPT_PAPER = "80";
	/** Printable width of the thermal roll in mm (72 for 80 mm rolls, 48 for 58 mm rolls). */
	public static final String RECEIPT_PRINTABLE_WIDTH_MM = "mosip.registration.cein.receipt.printable-width-mm";

	private CeinSettings() {
	}

	public static String getString(String key, String defaultValue) {
		String value = ApplicationContext.getStringValueFromApplicationMap(key);
		return value == null || "null".equals(value) ? defaultValue : value.trim();
	}

	public static boolean getBoolean(String key, boolean defaultValue) {
		String value = getString(key, null);
		return value == null || value.isEmpty() ? defaultValue : Boolean.parseBoolean(value);
	}

	public static int getInt(String key, int defaultValue) {
		String value = getString(key, null);
		if (value == null || value.isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * @return the forced language code (lower case) or {@code null} when the standard MOSIP
	 *         language handling must be used.
	 */
	public static String getForcedLanguage() {
		String lang = getString(FORCED_LANGUAGE, DEFAULT_FORCED_LANGUAGE);
		return lang == null || lang.isEmpty() ? null : lang.toLowerCase();
	}

	/**
	 * @return the forced language only if its resource bundles were loaded, i.e. the language is
	 *         part of mosip.mandatory-languages / mosip.optional-languages; {@code null} otherwise.
	 */
	public static String getUsableForcedLanguage(String bundleType) {
		String lang = getForcedLanguage();
		return lang != null && ApplicationContext.getBundle(lang, bundleType) != null ? lang : null;
	}

	public static boolean skipOnboardingForNonDefaultUsers() {
		return getBoolean(SKIP_ONBOARDING_NON_DEFAULT, true);
	}

	public static boolean isDocBrowseEnabled() {
		return getBoolean(DOC_BROWSE_ENABLED, true);
	}

	public static long getDocBrowseMaxSizeBytes() {
		return getInt(DOC_BROWSE_MAX_SIZE_KB, 5120) * 1024L;
	}

	public static int getDocBrowseMaxDimensionPx() {
		return getInt(DOC_BROWSE_MAX_DIMENSION_PX, 2480);
	}

	/** @return roll width in mm, or 0 when the standard A4 receipt must be printed. */
	public static int getReceiptRollWidthMm() {
		String paper = getString(RECEIPT_PAPER, DEFAULT_RECEIPT_PAPER);
		if (paper == null || paper.isEmpty() || "A4".equalsIgnoreCase(paper)) {
			return 0;
		}
		try {
			return Integer.parseInt(paper.toLowerCase().replace("mm", "").trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static int getReceiptPrintableWidthMm() {
		int roll = getReceiptRollWidthMm();
		return getInt(RECEIPT_PRINTABLE_WIDTH_MM, roll <= 58 ? 48 : 72);
	}
}
