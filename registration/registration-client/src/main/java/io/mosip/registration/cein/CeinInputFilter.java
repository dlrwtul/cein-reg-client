package io.mosip.registration.cein;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.dto.schema.Validator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * Typing guard derived from the UI spec: when a field's regex validator only accepts digits
 * (e.g. a national identification number "^[0-9]{13}$"), non-digit characters and characters
 * beyond the maximum length are refused while typing. The MOSIP regex validation and its error
 * message are unchanged; this only prevents typing mistakes.
 *
 * Multi-character insertions (paste, values set by the application such as pre-registration
 * data) only lose their separators and are never truncated: the regular validation flags them.
 */
public final class CeinInputFilter {

	/** ^[0-9]{13}$, ^\d{10,13}$, ^[0-9]+$, ^\d*$ ... (optional anchors) */
	private static final Pattern DIGITS_ONLY = Pattern
			.compile("^\\^?(?:\\[0-9\\]|\\\\d)(?:\\{(\\d+)(?:,(\\d*))?\\}|(\\+|\\*))\\$?$");

	private CeinInputFilter() {
	}

	public static void apply(TextField textField, UiFieldDTO field, String langCode) {
		if (textField == null || field == null || field.getValidators() == null) {
			return;
		}
		Integer max = null;
		boolean digitsOnly = false;
		for (Validator validator : field.getValidators()) {
			if (!isApplicable(validator, langCode)) {
				continue;
			}
			Matcher m = DIGITS_ONLY.matcher(validator.getValidator().trim());
			if (!m.matches()) {
				return; // another kind of rule: keep the standard behaviour only
			}
			digitsOnly = true;
			if (m.group(1) != null) {
				// {n} -> n ; {n,m} -> m ; {n,} -> unbounded
				Integer bound = m.group(2) == null ? Integer.valueOf(m.group(1))
						: (m.group(2).isEmpty() ? null : Integer.valueOf(m.group(2)));
				if (bound != null) {
					max = max == null ? bound : Math.min(max, bound);
				}
			}
		}
		if (!digitsOnly) {
			return;
		}
		final Integer maxLength = max;
		UnaryOperator<TextFormatter.Change> filter = change -> {
			String added = change.getText();
			if (added.length() > 1) {
				// paste or value set by the application: drop separators (spaces, dashes, dots...)
				// but never truncate - a value that is too long stays visible and is flagged by
				// the regular validation
				String digits = added.replaceAll("\\D", "");
				if (!digits.equals(added)) {
					change.setText(digits);
				}
				return change;
			}
			if (!added.isEmpty() && !Character.isDigit(added.charAt(0))) {
				return null;
			}
			if (maxLength != null && !added.isEmpty() && change.getControlNewText().length() > maxLength) {
				return null;
			}
			return change;
		};
		textField.setTextFormatter(new TextFormatter<>(filter));
	}

	private static boolean isApplicable(Validator validator, String langCode) {
		if (validator == null || validator.getValidator() == null) {
			return false;
		}
		if (validator.getType() != null && !"regex".equalsIgnoreCase(validator.getType())) {
			return false;
		}
		String lang = validator.getLangCode();
		return lang == null || lang.isBlank() || lang.equalsIgnoreCase(langCode);
	}

	/** Visible for tests. */
	static List<Object> parse(String regex) {
		Matcher m = DIGITS_ONLY.matcher(regex);
		return m.matches() ? List.of(true, String.valueOf(m.group(1)), String.valueOf(m.group(2))) : List.of(false);
	}
}
