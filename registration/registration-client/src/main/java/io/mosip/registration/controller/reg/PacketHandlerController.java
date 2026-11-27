package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.LoggerConstants.PACKET_HANDLER;
import static io.mosip.registration.constants.RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE_CODE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder;
import io.mosip.registration.cein.CeinSettings;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationApprovalDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SyncDataProcessDTO;
import io.mosip.registration.dto.schema.ProcessSpecDto;
import io.mosip.registration.entity.PreRegistrationList;
import io.mosip.registration.entity.ProcessSpec;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.enums.FlowType;
import io.mosip.registration.enums.Role;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.config.JobConfigurationService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.service.packet.PacketHandlerService;
import io.mosip.registration.service.packet.ReRegistrationService;
import io.mosip.registration.service.packet.RegistrationApprovalService;
import io.mosip.registration.service.sync.PreRegistrationDataSyncService;
import io.mosip.registration.service.template.TemplateService;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.acktemplate.TemplateGenerator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import lombok.NonNull;

/**
 * Class for Registration Packet operations
 * 
 * @author Sravya Surampalli
 * @since 1.0.0
 *
 */
@Controller
public class PacketHandlerController extends BaseController implements Initializable {

	private static final Logger LOGGER = AppConfig.getLogger(PacketHandlerController.class);

	@FXML
	private GridPane uploadRoot;

	@FXML
	private Label pendingApprovalCountLbl;

	@FXML
	private Label reRegistrationCountLbl;

	// CEIN home (design "Accueil")
	@FXML
	private Label ceinGreeting;
	@FXML
	private Label ceinUploadCount;
	@FXML
	private Label ceinApprovalCount;
	@FXML
	private Label ceinReRegCount;

	@FXML
	private Label lastBiometricTime;

	@FXML
	private Label lastPreRegPacketDownloadedTime;
	
	@FXML
	private ImageView inProgressImage;

	@FXML
	private Label lastSyncTime;

	@FXML
	private GridPane eodProcessGridPane;

	@FXML
	private VBox vHolder;

	@FXML
	public GridPane uinUpdateGridPane;

	@FXML
	public HBox userOnboardMessage;
	@FXML
	public ProgressIndicator progressIndicator;
	@FXML
	public GridPane progressPane;
	@FXML
	public ProgressBar syncProgressBar;
	@FXML
	private Label eodLabel;
	@FXML
	private GridPane syncDataPane;
	@FXML
	private ImageView syncDataImageView;
	@FXML
	private GridPane downloadPreRegDataPane;
	@FXML
	private ImageView downloadPreRegDataImageView;
	@FXML
	private GridPane updateOperatorBiometricsPane;
	@FXML
	private ImageView updateOperatorBiometricsImageView;
	@FXML
	private GridPane eodApprovalPane;
	@FXML
	private ImageView eodApprovalImageView;
	@FXML
	private GridPane reRegistrationPane;
	@FXML
	private ImageView reRegistrationImageView;
	@FXML
	private GridPane dashBoardPane;
	@FXML
	private GridPane uploadPacketPane;
	@FXML
	private GridPane centerRemapPane;
	@FXML
	private GridPane checkUpdatesPane;
	@FXML
	private ImageView viewReportsImageView;

	@FXML
	private Label versionValueLabel;

	@FXML
	private ImageView uploadPacketImageView;

	@FXML
	private ImageView remapImageView;

	@FXML
	private ImageView checkUpdatesImageView;

	@FXML
	private ImageView tickMarkImageView;

	@FXML
	private GridPane registrationGridPane;

	@Autowired
	private AckReceiptController ackReceiptController;

	@Autowired
	private TemplateService templateService;

	@Autowired
	private TemplateManagerBuilder templateManagerBuilder;

	@Autowired
	private TemplateGenerator templateGenerator;

	@Autowired
	private PreRegistrationDataSyncService preRegistrationDataSyncService;

	@Autowired
	private UserOnboardController userOnboardController;

	@Autowired
	private PacketHandlerService packetHandlerService;

	@Autowired
	private DashBoardController dashBoardController;

	@Autowired
	private RegistrationApprovalService registrationApprovalService;

	@Autowired
	private ReRegistrationService reRegistrationService;

	@Autowired
	private UserOnboardParentController userOnboardParentController;

	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private UserOnboardService userOnboardService;

	@Autowired
	private JobConfigurationService jobConfigurationService;

	@Autowired
	private IdentitySchemaDao identitySchemaDao;

	@Autowired
	private SoftwareUpdateHandler softwareUpdateHandler;

	@Autowired
	private HeaderController headerController;

	@Autowired
	private GenericController genericController;
	
	@Autowired
	private LanguageSelectionController languageSelectionController;

	@SuppressWarnings("unchecked")
	public void setLastUpdateTime() {
		try {

			ResponseDTO responseDTO = jobConfigurationService.getLastCompletedSyncJobs();
			if (responseDTO.getSuccessResponseDTO() != null) {

				List<SyncDataProcessDTO> dataProcessDTOs = ((List<SyncDataProcessDTO>) responseDTO
						.getSuccessResponseDTO().getOtherAttributes().get(RegistrationConstants.SYNC_DATA_DTO));

				LinkedList<String> timestamps = new LinkedList<>();
				dataProcessDTOs.forEach(syncDataProcessDTO -> {

					if (!(jobConfigurationService.getUnTaggedJobs().contains(syncDataProcessDTO.getJobId())
							|| jobConfigurationService.getOfflineJobs().contains(syncDataProcessDTO.getJobId()))) {
						timestamps.add(syncDataProcessDTO.getLastUpdatedTimes());
					}
				});

				Optional<String> latestUpdateTime = timestamps.stream().sorted((timestamp1, timestamp2) -> Timestamp
						.valueOf(timestamp2).compareTo(Timestamp.valueOf(timestamp1))).findFirst();

				lastSyncTime.setText(getLocalZoneTime(latestUpdateTime.isPresent() ? latestUpdateTime.get() : null));
				io.mosip.registration.cein.CeinStatus.setLastSync(lastSyncTime.getText());

				setLastPreRegPacketDownloadedTime();
			}
		} catch (RuntimeException exception) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
			lastSyncTime.setText("---");
		}
	}



	/**
	 * @return the userOnboardMsg
	 */
	public HBox getUserOnboardMessage() {
		return userOnboardMessage;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		versionValueLabel.setText(softwareUpdateHandler.getCurrentVersion());

		try {
			// CEIN: tiles keep their icon on hover (light hover background)

			setImage(syncDataImageView, RegistrationConstants.SYNC_IMG);	
			setImage(downloadPreRegDataImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			setImage(uploadPacketImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);		
			setImage(remapImageView, RegistrationConstants.SYNC_IMG);
			setImage(checkUpdatesImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			setImage(eodApprovalImageView, RegistrationConstants.PENDING_APPROVAL_IMG);
			setImage(reRegistrationImageView, RegistrationConstants.RE_REGISTRATION_IMG);		
			setImage(viewReportsImageView, RegistrationConstants.VIEW_REPORTS_IMG);
			setImage(tickMarkImageView, RegistrationConstants.TICK_IMG);
			setImage(updateOperatorBiometricsImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);

			if (!Role.hasSupervisorRole(SessionContext.userContext().getRoles())) {
				eodProcessGridPane.setVisible(false);
				eodLabel.setVisible(false);
			}
			setLastUpdateTime();
			pendingApprovalCountLbl.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_PENDING_APPLICATIONS));
			reRegistrationCountLbl.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_RE_REGISTER_APPLICATIONS));

			List<RegistrationApprovalDTO> pendingApprovalRegistrations = registrationApprovalService
					.getEnrollmentByStatus(RegistrationClientStatusCode.CREATED.getCode());

			List<PacketStatusDTO> reRegisterRegistrations = reRegistrationService.getAllReRegistrationPackets();

			// CEIN: greeting and counters
			if (ceinGreeting != null && SessionContext.userContext() != null) {
				String firstName = io.mosip.registration.cein.CeinStatus.firstName(SessionContext.userContext().getName());
				if (!firstName.isEmpty()) {
					ceinGreeting.setText(java.text.MessageFormat.format(
							ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("ceinGreeting"), firstName));
				}
			}
			if (ceinApprovalCount != null) {
				ceinApprovalCount.setText(String.valueOf(pendingApprovalRegistrations.size()));
				ceinReRegCount.setText(String.valueOf(reRegisterRegistrations.size()));
			}
			if (ceinUploadCount != null) {
				try {
					java.util.List<?> toUpload = io.mosip.registration.controller.ClientApplication.getApplicationContext()
							.getBean(io.mosip.registration.service.sync.PacketSynchService.class).fetchPacketsToBeSynched();
					ceinUploadCount.setText(String.valueOf(toUpload == null ? 0 : toUpload.size()));
				} catch (RuntimeException exception) {
					LOGGER.error("Unable to count the packets to upload", exception);
					ceinUploadCount.setText("");
				}
			}

			if (!pendingApprovalRegistrations.isEmpty()) {
				pendingApprovalCountLbl
						.setText(pendingApprovalRegistrations.size() + " " + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.APPLICATIONS));
			}
			if (!reRegisterRegistrations.isEmpty()) {
				reRegistrationCountLbl
						.setText(reRegisterRegistrations.size() + " " + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.APPLICATIONS));
			}

			Timestamp ts = userOnboardService.getLastUpdatedTime(SessionContext.userId());
			if (ts != null) {
				lastBiometricTime
						.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.LAST_DOWNLOADED) + " " + getLocalZoneTime(ts.toString()));
			}

			loadRegistrationProcesses();

		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error("REGISTRATION - UI- Home Page Loading", APPLICATION_NAME, APPLICATION_ID,
					regBaseCheckedException.getMessage() + ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
	}

	private void setImagesOnHover() {
		syncDataPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				
				setImage(syncDataImageView, RegistrationConstants.SYNC_DATA_FOCUSED_IMG);
			} else {
				
				setImage(syncDataImageView, RegistrationConstants.SYNC_IMG);
			}
		});
		downloadPreRegDataPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(downloadPreRegDataImageView, RegistrationConstants.DOWNLOAD_PREREG_FOCUSED_IMG);
			} else {

				setImage(downloadPreRegDataImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			}
		});
		updateOperatorBiometricsPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(updateOperatorBiometricsImageView, RegistrationConstants.UPDATE_OP_BIOMETRICS_FOCUSED_IMG);
			} else {
				setImage(updateOperatorBiometricsImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);
			}
		});
		eodApprovalPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(eodApprovalImageView, RegistrationConstants.PENDING_APPROVAL_FOCUSED_IMG);
			} else {
				setImage(eodApprovalImageView, RegistrationConstants.PENDING_APPROVAL_IMG);
			}
		});
		reRegistrationPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(reRegistrationImageView, RegistrationConstants.RE_REGISTRATION_FOCUSED_IMG);	
			} else {
				setImage(reRegistrationImageView, RegistrationConstants.RE_REGISTRATION_IMG);	
			}
		});
		dashBoardPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(viewReportsImageView, RegistrationConstants.VIEW_REPORTS_FOCUSED_IMG);
			} else {
				setImage(viewReportsImageView, RegistrationConstants.VIEW_REPORTS_IMG);
			}
		});
		uploadPacketPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(uploadPacketImageView, RegistrationConstants.UPDATE_OP_BIOMETRICS_FOCUSED_IMG);
			} else {

				setImage(uploadPacketImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);
			}
		});
		centerRemapPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(remapImageView, RegistrationConstants.SYNC_DATA_FOCUSED_IMG);
			} else {

				setImage(remapImageView, RegistrationConstants.SYNC_IMG);
			}
		});
		checkUpdatesPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(checkUpdatesImageView, RegistrationConstants.DOWNLOAD_PREREG_FOCUSED_IMG);
			} else {
				setImage(checkUpdatesImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			}
		});
	}

	public void startRegistration(@NonNull String processId) {
		LOGGER.info("Creation of Registration Starting : {}", processId);
		try {
			double version = identitySchemaDao.getLatestEffectiveSchemaVersion();
			ProcessSpecDto processSpecDto = identitySchemaDao.getProcessSpec(processId, version);
			FlowType flowType = FlowType.valueOf(processSpecDto.getFlow());
			auditFactory.audit(flowType.getAuditEvent(), Components.NAVIGATION, SessionContext.userContext().getUserId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			if (!validateScreenAuthorization(flowType.getScreenId())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
				return;
			}

			StringBuilder errorMessage = new StringBuilder();
			ResponseDTO responseDTO = validateSyncStatus();
			List<ErrorResponseDTO> errorResponseDTOs = responseDTO.getErrorResponseDTOs();
			if (errorResponseDTOs != null && !errorResponseDTOs.isEmpty()) {
				for (ErrorResponseDTO errorResponseDTO : errorResponseDTOs) {
					errorMessage.append(
							RegistrationUIConstants.getMessageLanguageSpecific(errorResponseDTO.getMessage())
									+ "\n\n");
				}
				generateAlert(RegistrationConstants.ERROR, errorMessage.toString().trim());
				return;
			}

			switch (flowType) {
				case NEW:
				case LOST:
				case CORRECTION:
					Parent createRoot = getRoot(RegistrationConstants.CREATE_PACKET_PAGE);
					getScene(createRoot);
					getScene(createRoot).getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
					if(registrationController.createRegistrationDTOObject(processId)) {
						genericController.populateScreens();
						return;
					}
					break;
				case UPDATE:
					if(registrationController.createRegistrationDTOObject(processId)) {
						Parent root = BaseController.load(getClass().getResource(RegistrationConstants.UIN_UPDATE),
								applicationContext.getBundle(registrationController.getSelectedLangList().get(0), RegistrationConstants.LABELS));
						getScene(root);
						LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading Update UIN screen ended.");
						return;
					}
			}

		} catch (Exception e) {
			LOGGER.error("Failed to start registration : {}", processId, e);
		}
		clearRegistrationData();
		generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_REG_PAGE));
	}



	public void showReciept() {
		try {
			RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
			LOGGER.info("Showing receipt Started for process", registrationDTO.getProcessId());
			String platformLanguageCode = ApplicationContext.applicationLanguage();
			String ackTemplateText = templateService.getHtmlTemplate(ACKNOWLEDGEMENT_TEMPLATE_CODE,
					platformLanguageCode);

			if (ackTemplateText != null && !ackTemplateText.isEmpty()) {

				ResponseDTO templateResponse = templateGenerator.generateTemplate(ackTemplateText, registrationDTO,
						templateManagerBuilder, RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE, getImagePath(RegistrationConstants.CROSS_IMG, true));
				if (templateResponse != null && templateResponse.getSuccessResponseDTO() != null) {
					Writer stringWriter = (Writer) templateResponse.getSuccessResponseDTO().getOtherAttributes()
							.get(RegistrationConstants.TEMPLATE_NAME);
					ackReceiptController.setStringWriter(stringWriter);
					ResponseDTO packetCreationResponse = savePacket(stringWriter, registrationDTO);
					if (packetCreationResponse.getSuccessResponseDTO() != null) {
						Parent createRoot = getRoot(RegistrationConstants.ACK_RECEIPT_PATH);
						getScene(createRoot);
						setIsAckOpened(true);
						LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Showing receipt ended.");
						return;
					}
				}
			}
		} catch (IOException | RegBaseCheckedException e) {
			LOGGER.error("Failed to load registration acknowledgment", e);
		}
		generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_ACKNOWLEDGEMENT_PAGE));
		clearRegistrationData();
		goToHomePageFromRegistration();
	}

	/**
	 * Validating screen authorization and Approve, Reject and Hold packets
	 */
	public void approvePacket() {
		LOGGER.info("Loading Pending Approval screen started.");
		try {
			auditFactory.audit(AuditEvent.NAV_APPROVE_REG, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
			GridPane root = BaseController.load(getClass().getResource(RegistrationConstants.PENDING_APPROVAL_PAGE));

			LOGGER.info("Validating Approve Packet screen for specific role");
			if (!validateScreenAuthorization(root.getId())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
				return;
			}

			getScene(root);

		} catch (IOException ioException) {
			LOGGER.error(ioException.getMessage(), ioException);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_APPROVAL_PAGE));
		}
		LOGGER.info("Loading Pending Approval screen ended.");
	}

	/**
	 * Validating screen authorization and Uploading packets to FTP server
	 */
	public void uploadPacket() {
		LOGGER.info("Loading Packet Upload screen started.");
		try {
			auditFactory.audit(AuditEvent.NAV_UPLOAD_PACKETS, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
			uploadRoot = BaseController.load(getClass().getResource(RegistrationConstants.FTP_UPLOAD_PAGE));
			LOGGER.info("Validating Upload Packet screen for specific role");

			if (!validateScreenAuthorization(uploadRoot.getId())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
				return;
			}

			getScene(uploadRoot);
			clearRegistrationData();
			SessionContext.setAutoLogout(true);

		} catch (IOException ioException) {
			LOGGER.error("Failed to load upload packet page", ioException);
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading Packet Upload screen ended.");
	}

	/**
	 * Sync data through batch jobs.
	 */
	public void syncData() {
		headerController.syncData(null);
	}

	/**
	 * This method is to trigger the Pre registration sync service
	 */
	@FXML
	public void downloadPreRegData() {

		headerController.downloadPreRegData(null);
	}

	/**
	 * change On-Board user Perspective
	 */
	public void onBoardUser() {
		if (!proceedOnAction("OU"))
			return;

		auditFactory.audit(AuditEvent.NAV_ON_BOARD_USER, Components.NAVIGATION, APPLICATION_NAME,
				AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

		SessionContext.map().put(RegistrationConstants.ONBOARD_USER, true);
		SessionContext.map().put(RegistrationConstants.ONBOARD_USER_UPDATE, true);

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading User Onboard Update page");

		try {
			GridPane headerRoot = BaseController.load(getClass().getResource(RegistrationConstants.USER_ONBOARD));
			getScene(headerRoot);
			userOnboardParentController.userOnboardId.lookup("#onboardUser").setVisible(false);
		} catch (IOException ioException) {
			LOGGER.error("Failed to load user onboard page", ioException);
		}
		userOnboardController.initUserOnboard();
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "User Onboard Update page is loaded");
	}

	/**
	 * To save the acknowledgement receipt along with the registration data and
	 * create packet
	 */
	private ResponseDTO savePacket(Writer stringWriter, RegistrationDTO registrationDTO) {
		LOGGER.info("Packet save has been started");
		byte[] ackInBytes = null;
		try {
			ackInBytes = stringWriter.toString().getBytes(RegistrationConstants.TEMPLATE_ENCODING);
		} catch (java.io.IOException ioException) {
			LOGGER.error("",  ioException);
		}

		// packet creation
		ResponseDTO response = packetHandlerService.handle(registrationDTO);

		if (response.getSuccessResponseDTO() != null
				&& response.getSuccessResponseDTO().getMessage().equals(RegistrationConstants.SUCCESS)) {
			try {
				// Deletes the pre registration Data after creation of registration Packet.
				if (getRegistrationDTOFromSession().getPreRegistrationId() != null
						&& !getRegistrationDTOFromSession().getPreRegistrationId().trim().isEmpty()) {

					ResponseDTO responseDTO = new ResponseDTO();
					List<PreRegistrationList> preRegistrationLists = new ArrayList<>();
					PreRegistrationList preRegistrationList = preRegistrationDataSyncService
							.getPreRegistrationRecordForDeletion(
									getRegistrationDTOFromSession().getPreRegistrationId());
					preRegistrationLists.add(preRegistrationList);
					preRegistrationDataSyncService.deletePreRegRecords(responseDTO, preRegistrationLists);

				}

				packetHandlerService.createAcknowledgmentReceipt(registrationDTO.getPacketId(), ackInBytes,
						RegistrationConstants.ACKNOWLEDGEMENT_FORMAT);

				// Sync and Uploads Packet when EOD Process Configuration is set to OFF
				String supervisorApproval = getValueFromApplicationContext(RegistrationConstants.SUPERVISOR_APPROVAL_CONFIG_FLAG);
				if (supervisorApproval != null && !getValueFromApplicationContext(RegistrationConstants.SUPERVISOR_APPROVAL_CONFIG_FLAG)
						.equalsIgnoreCase(RegistrationConstants.ENABLE)) {
					updatePacketStatus();//auto-approve
				}

				LOGGER.info("Registration's Acknowledgement Receipt saved");
			} catch (io.mosip.kernel.core.exception.IOException ioException) {
				LOGGER.error("",ioException);
			} catch (RegBaseCheckedException regBaseCheckedException) {
				LOGGER.error("", regBaseCheckedException);

				if (regBaseCheckedException.getErrorCode()
						.equals(RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorCode())) {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTH_ADVICE_FAILURE));
				}
			} catch (RuntimeException runtimeException) {
				LOGGER.error("", runtimeException);
			}
		} else {
			if (response.getErrorResponseDTOs() != null && response.getErrorResponseDTOs().get(0).getCode()
					.equals(RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorCode())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTH_ADVICE_FAILURE));
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PACKET_CREATION_FAILURE));
			}
		}
		return response;
	}


	/**
	 * Load re registration screen.
	 */
	public void loadReRegistrationScreen() {
		if (!proceedOnReRegistrationAction()) {
			return;
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading re-registration screen sarted.");

		try {
			auditFactory.audit(AuditEvent.NAV_RE_REGISTRATION, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			Parent root = BaseController.load(getClass().getResource(RegistrationConstants.REREGISTRATION_PAGE));

			LOGGER.info("REGISTRATION - LOAD_REREGISTRATION_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
					APPLICATION_NAME, APPLICATION_ID, "Loading reregistration screen");

			getScene(root);
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - LOAD_REREGISTRATION_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
					APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_APPROVAL_PAGE));
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading re-registration screen ended.");
	}

	public void viewDashBoard() {

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading dashboard screen sarted.");

		try {
			auditFactory.audit(AuditEvent.NAV_DASHBOARD, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			String dashboardTemplateText = templateService.getHtmlTemplate(
					RegistrationConstants.DASHBOARD_TEMPLATE_CODE, ApplicationContext.applicationLanguage());

			ResponseDTO templateResponse = templateGenerator.generateDashboardTemplate(dashboardTemplateText,
					templateManagerBuilder, RegistrationConstants.DASHBOARD_TEMPLATE,
					ClientApplication.getApplicationStartTime());

			if (templateResponse != null && templateResponse.getSuccessResponseDTO() != null) {
				Writer stringWriter = (Writer) templateResponse.getSuccessResponseDTO().getOtherAttributes()
						.get(RegistrationConstants.DASHBOARD_TEMPLATE);
				dashBoardController.setStringWriter(stringWriter);
				Parent root = BaseController.load(getClass().getResource(RegistrationConstants.DASHBOARD_PAGE));

				LOGGER.info("REGISTRATION - LOAD_DASHBOARD_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
						APPLICATION_NAME, APPLICATION_ID, "Loading dashboard screen");

				getScene(root);
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_DASHBOARD_PAGE));
			}
		} catch (IOException | RegBaseCheckedException exception) {
			LOGGER.error("REGISTRATION - LOAD_DASHBOARD_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
					APPLICATION_NAME, APPLICATION_ID, exception.getMessage() + ExceptionUtils.getStackTrace(exception));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_DASHBOARD_PAGE));
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading dashboard screen ended.");
	}

	/**
	 * Update packet status.
	 * 
	 * @throws RegBaseCheckedException
	 */
	private void updatePacketStatus() throws RegBaseCheckedException {
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID,
				"Auto Approval of Packet when EOD process disabled started");

		registrationApprovalService.updateRegistration((getRegistrationDTOFromSession().getPacketId()),
				RegistrationConstants.EMPTY, RegistrationClientStatusCode.APPROVED.getCode());

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID,
				"Auto Approval of Packet when EOD process disabled ended");

	}


	public ProgressIndicator getProgressIndicator() {
		return progressIndicator;
	}
	
	public GridPane getPreRegDataPane() {
		return downloadPreRegDataPane;
	}
	
	public void setInProgressImage(Image image) {
		inProgressImage.setImage(image);
	}

	public void setLastPreRegPacketDownloadedTime() {

		SyncControl syncControl = jobConfigurationService
				.getSyncControlOfJob(RegistrationConstants.OPT_TO_REG_PDS_J00003);

		if (syncControl != null) {
			Timestamp lastPreRegPacketDownloaded = syncControl.getLastSyncDtimes();

			if (lastPreRegPacketDownloaded != null) {
				lastPreRegPacketDownloadedTime.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.LAST_DOWNLOADED) + " "
						+ getLocalZoneTime(lastPreRegPacketDownloaded.toString()));
			}
		}
	}

	@FXML
	public void uploadPacketToServer() {
		auditFactory.audit(AuditEvent.SYNC_PRE_REGISTRATION_PACKET, Components.SYNC_SERVER_TO_CLIENT,
				SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		uploadPacket();
	}

	@FXML
	public void intiateRemapProcess() {
		headerController.intiateRemapProcess();
	}

	@FXML
	public void hasUpdate() {
		headerController.hasUpdate(null);
	}


	private void loadRegistrationProcesses()  {
		try {
			double version = identitySchemaDao.getLatestEffectiveSchemaVersion();
			List<ProcessSpec> processSpecs = identitySchemaDao.getAllActiveProcessSpecs(version);
			addParentRowConstraints(processSpecs == null ? 0 : processSpecs.size());
			AtomicInteger i = new AtomicInteger();
			Objects.requireNonNull(processSpecs).forEach(processSpec -> {
				try {
					FlowType flowType = FlowType.valueOf(processSpec.getFlow());
					if(flowType == null) {
						LOGGER.error("Invalid registration flow type {}", processSpec.getFlow());
						generateAlert(RegistrationConstants.ERROR,
								RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.INVALID_FLOW_TYPE));
						return;
					}
					ProcessSpecDto processSpecDto = identitySchemaDao.getProcessSpec(processSpec.getId(), version);
					GridPane gridPane = buildRegistrationProcessPane(processSpecDto);
					addProcessTile(gridPane, i.getAndIncrement());
				} catch (RegBaseCheckedException e) {
					LOGGER.error("Failed to build process pane {}", processSpec, e);
				}
			});
		} catch (RegBaseCheckedException e) {
			LOGGER.error("Failed to get configured processes", e);
		}
	}

	// CEIN: process tiles are laid out in a grid of PROCESS_TILE_COLUMNS columns
	private static final int PROCESS_TILE_COLUMNS = 4;

	private void addParentRowConstraints(int size) {
		registrationGridPane.getRowConstraints().clear();
		registrationGridPane.getColumnConstraints().clear();
		for (int c = 0; c < PROCESS_TILE_COLUMNS; c++) {
			javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
			column.setPercentWidth(100.0 / PROCESS_TILE_COLUMNS);
			column.setFillWidth(true);
			registrationGridPane.getColumnConstraints().add(column);
		}
	}

	private void addProcessTile(GridPane tile, int index) {
		registrationGridPane.add(tile, index % PROCESS_TILE_COLUMNS, index / PROCESS_TILE_COLUMNS);
	}

	private GridPane buildRegistrationProcessPane(ProcessSpecDto processSpecDto) {
		// CEIN (design "Accueil"): icon, process label and caption; the first tile is the main action
		GridPane gridPane = new GridPane();
		gridPane.getStyleClass().addAll("operationalPaneDetailsSync", "ceinProcessTile");
		if (registrationGridPane.getChildren().isEmpty()) {
			gridPane.getStyleClass().add("ceinProcessTilePrimary");
		}
		gridPane.setMaxWidth(Double.MAX_VALUE);
		gridPane.setMaxHeight(Double.MAX_VALUE);
		gridPane.setId(processSpecDto.getId());
		ColumnConstraints columnConstraint = new ColumnConstraints();
		columnConstraint.setHgrow(javafx.scene.layout.Priority.ALWAYS);
		gridPane.getColumnConstraints().add(columnConstraint);
		gridPane.setOnMouseClicked(event -> {
			selectLanguage(event);
		});
		VBox vBox = new VBox(14);
		vBox.setAlignment(Pos.TOP_LEFT);

		javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
		icon.setContent(processIcon(processSpecDto.getFlow()));
		icon.getStyleClass().addAll("ceinIconStroke", "ceinProcessIcon");
		// MOSIP icon of the process, kept for the node id used by the client (not displayed)
		ImageView imageView = new ImageView();
		imageView.setId(processSpecDto.getId()+"_img");
		imageView.setManaged(false);
		imageView.setVisible(false);
		vBox.getChildren().addAll(new javafx.scene.layout.StackPane(icon, imageView));

		String lang = ApplicationContext.applicationLanguage();
		Label label = new Label();
		label.setText(processSpecDto.getLabel().get(lang));
		label.setWrapText(true);
		label.getStyleClass().addAll("operationalTitle", "ceinProcessTileTitle");
		String caption = processSpecDto.getCaption() == null ? null : processSpecDto.getCaption().get(lang);
		if (caption == null || caption.isBlank()) {
			ResourceBundle labels = ApplicationContext.getInstance().getApplicationLanguageLabelBundle();
			String key = "ceinProcessHint_" + processSpecDto.getFlow();
			caption = labels.containsKey(key) ? labels.getString(key) : null;
		}
		VBox texts = new VBox(4, label);
		if (caption != null && !caption.isBlank()) {
			Label captionLabel = new Label(caption);
			captionLabel.setWrapText(true);
			captionLabel.getStyleClass().add("ceinProcessTileCaption");
			texts.getChildren().add(captionLabel);
		}
		vBox.getChildren().add(texts);
		gridPane.addColumn(0, vBox);
		GridPane.setHgrow(vBox, javafx.scene.layout.Priority.ALWAYS);
		changeNodeOrientation(gridPane);
		return gridPane;
	}

	private static String processIcon(String flow) {
		if (flow == null) {
			flow = "";
		}
		switch (flow) {
		case "UPDATE":
			return "M3 5h18v14H3z M11 11a2 2 0 1 1-4 0 2 2 0 0 1 4 0 M6 16c.6-1.4 1.7-2 3-2s2.4.6 3 2M15 10h3M15 13h3";
		case "LOST":
			return "M18 11a7 7 0 1 1-14 0 7 7 0 0 1 14 0 M21 21l-4.3-4.3";
		case "CORRECTION":
			return "M12 11v2.5c0 3-1 5.5-2.5 7.5M8 5.6A7 7 0 0 1 19 11v2M5 9a7 7 0 0 0-.4 2.3v2M15.5 13c0 3-.6 5.5-1.8 7.6M8.6 11a3.5 3.5 0 0 1 6.9 0";
		default:
			return "M13 8a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M2 21c0-3.9 3.1-7 7-7s7 3.1 7 7M19 8v6M16 11h6";
		}
	}

	public void selectLanguage(MouseEvent event) {
		startRegistrationProcess(((GridPane) event.getSource()).getId());
	}

	/**
	 * CEIN: starts a registration of the given process (same checks and language handling as a
	 * click on the process tile); used by the tiles and by "New registration" on the
	 * acknowledgement screen.
	 */
	public void startRegistrationProcess(String processId) {
		if (processId == null || !proceedOnRegistrationAction())
			return;

		StringBuilder errorMessage = new StringBuilder();
		ResponseDTO responseDTO;
		responseDTO = validateSyncStatus();
		List<ErrorResponseDTO> errorResponseDTOs = responseDTO.getErrorResponseDTOs();
		if (errorResponseDTOs != null && !errorResponseDTOs.isEmpty()) {
			for (ErrorResponseDTO errorResponseDTO : errorResponseDTOs) {
				errorMessage.append(
						RegistrationUIConstants.getMessageLanguageSpecific(errorResponseDTO.getMessage())
								+ "\n\n");
			}
			generateAlert(RegistrationConstants.ERROR, errorMessage.toString().trim());
		} else {
			try {
				languageSelectionController.setProcessId(processId);
				String forcedLang = CeinSettings.getUsableForcedLanguage(RegistrationConstants.LABELS);
				if (forcedLang != null) {
					// CEIN: registrations are captured in the forced language only, no selection pop-up
					languageSelectionController.submitLanguagesAndProceed(List.of(forcedLang));
				}
				else if(isLanguageSelectionRequired()) {
					getStage().getScene().getRoot().setDisable(true);
					languageSelectionController.init();
				}
				else {
					languageSelectionController.submitLanguagesAndProceed(baseService.getMandatoryLanguages());
				}
			} catch (PreConditionCheckException e) {
				generateAlert(RegistrationConstants.ERROR, e.getErrorCode());
			}
		}
	}

	private boolean isLanguageSelectionRequired() throws PreConditionCheckException {
		return ( baseService.getMinLanguagesCount() >= 1 && baseService.getMaxLanguagesCount() > 1 );
	}
}
