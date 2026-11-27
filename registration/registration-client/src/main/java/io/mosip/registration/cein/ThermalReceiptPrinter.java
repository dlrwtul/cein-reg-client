package io.mosip.registration.cein;

import java.util.Comparator;
import java.util.Optional;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.web.WebEngine;

/**
 * Prints the acknowledgement receipt on a thermal roll printer (58 mm or 80 mm).
 *
 * <ol>
 * <li>Printer: the one whose name contains {@code mosip.registration.cein.receipt.printer-name},
 * otherwise the system default printer.</li>
 * <li>Paper: among the papers exposed by the driver, the one whose width is closest to the
 * roll width (within {@value #PAPER_WIDTH_TOLERANCE_MM} mm), the longest one first so that the
 * receipt is not split in several pages; hardware minimum margins.</li>
 * <li>Layout: a print-only stylesheet is injected in the receipt page so that the HTML template
 * (designed for A4) reflows on the narrow printable width: fluid tables, scaled images, compact
 * fonts. The on-screen preview is not affected ({@code @media print}).</li>
 * </ol>
 */
public final class ThermalReceiptPrinter {

	private static final Logger LOGGER = AppConfig.getLogger(ThermalReceiptPrinter.class);

	public static final String PRINTER_NAME = "mosip.registration.cein.receipt.printer-name";

	private static final double PAPER_WIDTH_TOLERANCE_MM = 6;
	private static final double POINTS_PER_MM = 72d / 25.4d;
	private static final String STYLE_ID = "cein-thermal-print";

	private ThermalReceiptPrinter() {
	}

	/**
	 * @return true when the job was sent to the printer, false when no printer is available.
	 */
	public static boolean print(WebEngine engine, String jobName) {
		int rollWidthMm = CeinSettings.getReceiptRollWidthMm();
		Printer printer = findPrinter();
		if (printer == null) {
			LOGGER.error("CEIN receipt: no printer available");
			return false;
		}

		PrinterJob job = PrinterJob.createPrinterJob(printer);
		if (job == null) {
			LOGGER.error("CEIN receipt: unable to create a print job on {}", printer.getName());
			return false;
		}

		Paper paper = findRollPaper(printer, rollWidthMm).orElseGet(() -> {
			LOGGER.warn("CEIN receipt: no {} mm paper exposed by driver of {}, using its default paper",
					rollWidthMm, printer.getName());
			return printer.getDefaultPageLayout().getPaper();
		});
		PageLayout layout = printer.createPageLayout(paper, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);

		job.getJobSettings().setJobName(jobName);
		job.getJobSettings().setPageLayout(layout);

		injectThermalStyle(engine, CeinSettings.getReceiptPrintableWidthMm(), rollWidthMm);

		LOGGER.info("CEIN receipt: printing on {} - paper {} ({} x {} mm), printable width {} mm",
				printer.getName(), paper.getName(), Math.round(paper.getWidth() / POINTS_PER_MM),
				Math.round(paper.getHeight() / POINTS_PER_MM), Math.round(layout.getPrintableWidth() / POINTS_PER_MM));

		engine.print(job);
		return job.endJob();
	}

	private static Printer findPrinter() {
		String wanted = CeinSettings.getString(PRINTER_NAME, "");
		if (!wanted.isEmpty()) {
			for (Printer printer : Printer.getAllPrinters()) {
				if (printer.getName().toLowerCase().contains(wanted.toLowerCase())) {
					return printer;
				}
			}
			LOGGER.warn("CEIN receipt: printer \"{}\" not found, using the default printer", wanted);
		}
		return Printer.getDefaultPrinter();
	}

	static Optional<Paper> findRollPaper(Printer printer, int rollWidthMm) {
		return printer.getPrinterAttributes().getSupportedPapers().stream()
				.filter(p -> Math.abs(p.getWidth() / POINTS_PER_MM - rollWidthMm) <= PAPER_WIDTH_TOLERANCE_MM)
				.min(Comparator.<Paper>comparingDouble(p -> Math.abs(p.getWidth() / POINTS_PER_MM - rollWidthMm))
						.thenComparing(Comparator.comparingDouble(Paper::getHeight).reversed()));
	}

	private static void injectThermalStyle(WebEngine engine, int printableWidthMm, int rollWidthMm) {
		String baseFont = rollWidthMm <= 58 ? "7.5pt" : "9pt";
		// WebEngine.print() lays the page out with 1 CSS px = 1 printer point, so the printable
		// width is expressed in px. This also keeps the receipt inside the roll when the driver
		// only exposes a wider (A4) paper.
		long bodyWidthPx = Math.round(printableWidthMm * POINTS_PER_MM);
		String css = "@media print {"
				+ "@page{margin:0;}"
				+ "html{margin:0!important;padding:0!important;}"
				+ "body{margin:0!important;padding:0!important;min-width:0!important;width:" + bodyWidthPx + "px!important;}"
				+ "body{font-family:Arial,Helvetica,sans-serif;font-size:" + baseFont + ";line-height:1.25;color:#000;}"
				+ "*{max-width:100%!important;box-sizing:border-box;}"
				+ "table{width:100%!important;table-layout:fixed;border-collapse:collapse;}"
				+ "td,th{word-wrap:break-word;overflow-wrap:break-word;padding:1px 2px!important;vertical-align:top;}"
				+ "img{max-width:100%!important;height:auto!important;}"
				+ "h1,h2,h3{font-size:1.15em!important;margin:2px 0!important;}"
				+ "hr{border:0;border-top:1px dashed #000;}"
				+ "}";
		String script = "(function(){var s=document.getElementById('" + STYLE_ID + "');"
				+ "if(!s){s=document.createElement('style');s.id='" + STYLE_ID + "';"
				+ "(document.head||document.documentElement).appendChild(s);}"
				+ "s.textContent=" + jsString(css) + ";})();";
		try {
			engine.executeScript(script);
		} catch (RuntimeException e) {
			LOGGER.error("CEIN receipt: unable to inject the thermal print stylesheet (printable width {} mm)",
					printableWidthMm, e);
		}
	}

	private static String jsString(String value) {
		return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
	}
}
