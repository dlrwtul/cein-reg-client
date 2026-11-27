package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.cein.CeinSettings;
import io.mosip.registration.cein.ThermalReceiptPrinter;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Class for showing the Acknowledgement Receipt
 *
 * @author Himaja Dhanyamraju
 *
 */
@Controller
public class AckReceiptController extends BaseController implements Initializable {

	private static final Logger LOGGER = AppConfig.getLogger(AckReceiptController.class);

	@Autowired
	private PacketHandlerController packetController;

	private Writer stringWriter;

	@FXML
	protected GridPane rootPane;

	@FXML
	private WebView webView;

	@FXML
	private Button newRegistration;

	@FXML
	private Button print;

	// @FXML
	// private Button sendNotification;

	@FXML
	private ImageView newRegistrationBtnImgVw;
	@FXML
	private ImageView printImgVw;	
	@FXML
	private ImageView SendEmailImageView;
;

	@Autowired
	private SendNotificationController sendNotificationController;

	// CEIN (design "Confirmation")
	@FXML
	private javafx.scene.control.Label ceinAckRid;
	@FXML
	private javafx.scene.control.Label ceinAckApplicant;
	@FXML
	private javafx.scene.control.Label ceinAckApplicantTerm;
	@FXML
	private javafx.scene.control.Label ceinAckPrintNote;
	@FXML
	private javafx.scene.control.Label ceinAckPreviewTitle;
	@FXML
	private javafx.scene.layout.StackPane ceinAckPaper;

	public void setStringWriter(Writer stringWriter) {
		this.stringWriter = stringWriter;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOGGER.info("REGISTRATION - UI - ACK_RECEIPT_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
				"Page loading has been started");
		setImage(newRegistrationBtnImgVw, RegistrationConstants.NEW_REGISTRATION_IMG);
		setImage(printImgVw, RegistrationConstants.PRINTER_IMG);
		setImage(SendEmailImageView, RegistrationConstants.SEND_EMAIL_IMG);
		
		// setImagesOnHover();
		String notificationType = getValueFromApplicationContext(RegistrationConstants.MODE_OF_COMMUNICATION);
		/*
		 * if (notificationType != null && !notificationType.trim().isEmpty() &&
		 * !notificationType.equals("NONE")) {
		 *
		 * sendNotification.setVisible(false); } else {
		 * sendNotification.setVisible(false); }
		 */

		fillCeinDetails();

		WebEngine engine = webView.getEngine();
		// loads the generated HTML template content into webview
		engine.loadContent(stringWriter.toString());
		LOGGER.info("REGISTRATION - UI - ACK-RECEIPT_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
				"Acknowledgement template has been loaded to webview");
	}

	/*
	 * private void setImagesOnHover() { Image sendEmailInWhite = new Image(
	 * getClass().getResourceAsStream(RegistrationConstants.
	 * SEND_EMAIL_FOCUSED_IMAGE_PATH)); Image sendEmailImage = new
	 * Image(getClass().getResourceAsStream(RegistrationConstants.
	 * SEND_EMAIL_IMAGE_PATH));
	 *
	 * sendNotification.hoverProperty().addListener((ov, oldValue, newValue) -> { if
	 * (newValue) { sendNotificationImageView.setImage(sendEmailInWhite); } else {
	 * sendNotificationImageView.setImage(sendEmailImage); } }); }
	 */

	/**
	 * To print the acknowledgement receipt after packet creation when the user
	 * clicks on print button.
	 *
	 * @param event - the event that happens on click of print button
	 */
	@FXML
	public void printReceipt(ActionEvent event) {
		LOGGER.info("REGISTRATION - UI - ACK_RECEIPT_CONTROLLER", RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Printing the Acknowledgement Receipt");

		String jobName = getRegistrationDTOFromSession().getRegistrationId() + "_Ack";
		if (CeinSettings.getReceiptRollWidthMm() > 0) {
			// CEIN: thermal roll printer (58/80 mm), see mosip.registration.cein.receipt.*
			if (!ThermalReceiptPrinter.print(webView.getEngine(), jobName)) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PRINT_INITIATION_FAIL));
				return;
			}
		} else {
			PrinterJob job = PrinterJob.createPrinterJob();
			if (job != null) {
				job.getJobSettings().setJobName(jobName);
				webView.getEngine().print(job);
				job.endJob();
			}
		}
		generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PRINT_INITIATION_SUCCESS));
		//goToHomePageFromRegistration();
	}

	@FXML
	public void sendNotification(ActionEvent event) {
		LOGGER.debug("REGISTRATION - UI - ACK_RECEIPT_CONTROLLER", RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Going to Send Notification Popup Window");

		sendNotificationController.init();
	}

	@FXML
	public void goToNewRegistration(ActionEvent event) {
		LOGGER.info("REGISTRATION - UI - ACK_RECEIPT_CONTROLLER", RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Going to New Registration Page after packet creation");

		// CEIN: a new registration of the same process starts right away (MOSIP went back home)
		String processId = getRegistrationDTOFromSession() != null ? getRegistrationDTOFromSession().getProcessId() : null;
		clearRegistrationData();
		goToHomePageFromRegistration();
		if (processId != null) {
			packetController.startRegistrationProcess(processId);
		}
	}

	/** CEIN: "Back to home" link of the confirmation screen. */
	@FXML
	public void goToHome(ActionEvent event) {
		clearRegistrationData();
		goToHomePageFromRegistration();
	}

	/** CEIN: registration number, applicant name, printer note and receipt preview width. */
	private void fillCeinDetails() {
		try {
			ResourceBundle labels = applicationContext.getApplicationLanguageLabelBundle();
			io.mosip.registration.dto.RegistrationDTO registration = getRegistrationDTOFromSession();
			if (ceinAckRid != null) {
				ceinAckRid.setText(registration != null && registration.getRegistrationId() != null ? registration.getRegistrationId() : "");
			}
			String applicant = registration == null ? "" : applicantName(registration);
			if (ceinAckApplicant != null) {
				ceinAckApplicant.setText(applicant);
				ceinAckApplicant.setVisible(!applicant.isEmpty());
				ceinAckApplicantTerm.setVisible(!applicant.isEmpty());
			}
			int roll = CeinSettings.getReceiptRollWidthMm();
			String format = roll > 0 ? roll + " mm" : "A4";
			if (ceinAckPrintNote != null) {
				ceinAckPrintNote.setText(java.text.MessageFormat.format(labels.getString(roll > 0 ? "ceinAckPrintNoteRoll" : "ceinAckPrintNoteA4"), roll));
			}
			if (ceinAckPreviewTitle != null) {
				ceinAckPreviewTitle.setText(labels.getString("ceinAckPreview") + " \u00B7 " + format);
			}
			if (ceinAckPaper != null) {
				double width = roll <= 0 ? Double.MAX_VALUE : (roll <= 58 ? 219 : 302);
				ceinAckPaper.setPrefWidth(roll <= 0 ? javafx.scene.layout.Region.USE_COMPUTED_SIZE : width);
				ceinAckPaper.setMaxWidth(width);
			}
		} catch (RuntimeException exception) {
			LOGGER.error("Unable to fill the acknowledgement details", exception);
		}
	}

	private String applicantName(io.mosip.registration.dto.RegistrationDTO registration) {
		StringBuilder name = new StringBuilder();
		for (String field : CeinSettings.getAckNameFields()) {
			Object value = registration.getDemographics().get(field);
			String text = null;
			if (value instanceof String) {
				text = (String) value;
			} else if (value instanceof java.util.List && !((java.util.List<?>) value).isEmpty()) {
				Object first = ((java.util.List<?>) value).get(0);
				try {
					Object v = first.getClass().getMethod("getValue").invoke(first);
					text = v == null ? null : v.toString();
				} catch (ReflectiveOperationException exception) {
					text = null;
				}
			}
			if (text != null && !text.isBlank()) {
				name.append(name.length() > 0 ? " " : "").append(text.trim());
			}
		}
		return name.toString();
	}

	/**
	 * Go to home ack template.
	 */
	public void goToHomeAckTemplate() {
		try {
			BaseController.load(getClass().getResource(RegistrationConstants.HOME_PAGE));
			if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				clearOnboardData();
				clearRegistrationData();
			} else {
				SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ,
						RegistrationConstants.ENABLE);
			}
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - UI - ACK_RECEIPT_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_HOME_PAGE));
		} catch (RuntimeException runtimException) {
			LOGGER.error("REGISTRATION - UI - ACK_RECEIPT_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					runtimException.getMessage() + ExceptionUtils.getStackTrace(runtimException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_HOME_PAGE));
		}

	}

}