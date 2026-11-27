package io.mosip.registration.cein;

import java.io.InputStream;

import javafx.scene.text.Font;

/**
 * Registers the fonts of the CEIN theme (Inter, Space Grotesk - SIL Open Font License, see
 * /fonts/OFL-*.txt) so that they are available to every scene, the preloader included.
 */
public final class CeinFonts {

	private static final String[] FILES = { "Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf",
			"Inter-Bold.ttf", "SpaceGrotesk-SemiBold.ttf", "SpaceGrotesk-Bold.ttf" };

	private static boolean loaded;

	private CeinFonts() {
	}

	public static synchronized void load() {
		if (loaded) {
			return;
		}
		for (String file : FILES) {
			try (InputStream in = CeinFonts.class.getResourceAsStream("/fonts/" + file)) {
				if (in != null) {
					Font.loadFont(in, 14);
				}
			} catch (Exception ignored) {
				// the theme falls back to the system font
			}
		}
		loaded = true;
	}
}
