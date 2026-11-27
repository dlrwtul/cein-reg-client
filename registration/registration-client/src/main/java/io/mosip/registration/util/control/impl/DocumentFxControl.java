package io.mosip.registration.util.control.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;


import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.api.docscanner.DocScannerUtil;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.enums.FlowType;
import io.mosip.registration.service.doc.category.ValidDocumentService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.common.ComboBoxAutoComplete;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class DocumentFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DocumentFxControl.class);
	private static final String SCRIPT_NAME = "applicanttype.mvel";
	private static String loggerClassName = " Text Field Control Type Class";

	private DocumentScanController documentScanController;

	private MasterSyncService masterSyncService;

	private ValidDocumentService validDocumentService;

	private String PREVIEW_ICON = "previewIcon";

	private String CLEAR_ID = "clear";

	public DocumentFxControl() {
		org.springframework.context.ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		auditFactory = applicationContext.getBean(AuditManagerService.class);
		documentScanController = applicationContext.getBean(DocumentScanController.class);
		masterSyncService = applicationContext.getBean(MasterSyncService.class);
		validDocumentService = applicationContext.getBean(ValidDocumentService.class);
		this.requiredFieldValidator = applicationContext.getBean(RequiredFieldValidator.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;

		// CEIN (design "Documents"): category | document type | reference number | file actions
		GridPane row = new GridPane();
		row.setHgap(16);
		row.getStyleClass().addAll("ceinCard", "ceinDocRow");
		row.setMaxWidth(Double.MAX_VALUE);
		javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints();
		c1.setPercentWidth(22);
		javafx.scene.layout.ColumnConstraints c2 = new javafx.scene.layout.ColumnConstraints();
		c2.setPercentWidth(27);
		javafx.scene.layout.ColumnConstraints c3 = new javafx.scene.layout.ColumnConstraints();
		c3.setPercentWidth(20);
		javafx.scene.layout.ColumnConstraints c4 = new javafx.scene.layout.ColumnConstraints();
		c4.setPercentWidth(31);
		c4.setHalignment(javafx.geometry.HPos.RIGHT);
		row.getColumnConstraints().addAll(c1, c2, c3, c4);
		RowConstraints rc = new RowConstraints();
		rc.setValignment(javafx.geometry.VPos.CENTER);
		row.getRowConstraints().add(rc);

		// DROP-DOWN (its title label becomes the category column)
		VBox typeBox = create(uiFieldDTO);
		Node categoryTitle = typeBox.getChildren().remove(0);
		categoryTitle.setVisible(true);
		categoryTitle.getStyleClass().add("ceinDocCategory");
		if (categoryTitle instanceof Label) {
			((Label) categoryTitle).setWrapText(true);
		}
		row.add(categoryTitle, 0, 0);
		row.add(typeBox, 1, 0);

		// REF-FIELD (the floating label is not used: the column header names it)
		VBox refBox = createDocRef(uiFieldDTO.getId());
		Node refLabel = refBox.getChildren().get(0);
		refLabel.setManaged(false);
		refLabel.setOpacity(0);
		row.add(refBox, 2, 0);

		// FILE: captured document (icon, info, view, delete) or scan / import buttons
		VBox captured = getImageGridPane(PREVIEW_ICON, null);
		Button viewButton = ceinButton(uiFieldDTO.getId() + "CeinView", "ceinDocView", "ceinSecondary",
				"M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0");
		viewButton.setOnAction(event -> scanDocument(true));
		HBox capturedRow = new HBox(12, ceinFileIcon(), ceinFileInfo(), viewButton);
		capturedRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
		captured.getChildren().setAll(capturedRow);

		VBox clear = getImageGridPane(CLEAR_ID, null);
		Button deleteButton = ceinButton(uiFieldDTO.getId() + "CeinDelete", null, "ceinDanger",
				"M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3");
		deleteButton.setOnAction(event -> {
			ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());
			comboBox.getSelectionModel().clearSelection();
			clearCapturedDocuments();
		});
		clear.getChildren().setAll(deleteButton);

		HBox empty = new HBox(8);
		empty.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
		empty.getChildren().add(createScanButton(uiFieldDTO));
		if (io.mosip.registration.cein.CeinSettings.isDocBrowseEnabled()) {
			Button importButton = ceinButton(uiFieldDTO.getId() + "CeinImport", "ceinDocImport", "ceinSecondary",
					"M12 16V4M7 9l5-5 5 5M4 16v4h16v-4");
			importButton.setOnAction(event -> importDocument());
			empty.getChildren().add(importButton);
		}
		empty.visibleProperty().bind(captured.visibleProperty().not());
		empty.managedProperty().bind(empty.visibleProperty());

		HBox file = new HBox(8, captured, clear, empty);
		file.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
		HBox.setHgrow(captured, javafx.scene.layout.Priority.ALWAYS);
		row.add(file, 3, 0);

		this.node = row;

		setListener(getField(uiFieldDTO.getId() + RegistrationConstants.BUTTON));

		changeNodeOrientation(row, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		fillData(getDocumentCategories());

		return this.control;
	}

	/** CEIN: label of a button of the document row. */
	private String ceinLabel(String key) {
		ResourceBundle rb = ApplicationContext.getInstance()
				.getBundle(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0), RegistrationConstants.LABELS);
		return rb.containsKey(key) ? rb.getString(key) : key;
	}

	private Button ceinButton(String id, String labelKey, String styleClass, String svg) {
		Button button = new Button(labelKey == null ? null : ceinLabel(labelKey));
		button.setId(id);
		button.setMnemonicParsing(false);
		button.getStyleClass().addAll(styleClass, "ceinDocButton");
		javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
		icon.setContent(svg);
		icon.getStyleClass().add("ceinIconStroke");
		if ("ceinDanger".equals(styleClass)) {
			icon.getStyleClass().add("ceinIconDanger");
		}
		button.setGraphic(icon);
		button.setGraphicTextGap(8);
		button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
		return button;
	}

	private Node ceinFileIcon() {
		javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
		icon.setContent("M14 3H6v18h12V7z M14 3v4h4");
		icon.getStyleClass().add("ceinIconStroke");
		javafx.scene.layout.StackPane box = new javafx.scene.layout.StackPane(icon);
		box.getStyleClass().add("ceinDocFileIcon");
		return box;
	}

	private Label ceinInfoLabel;
	private Label ceinSourceLabel;

	private Node ceinFileInfo() {
		ceinInfoLabel = new Label();
		ceinInfoLabel.getStyleClass().add("ceinDocInfo");
		ceinSourceLabel = new Label();
		ceinSourceLabel.getStyleClass().add("ceinDocSource");
		VBox info = new VBox(2, ceinInfoLabel, ceinSourceLabel);
		HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
		info.setMinWidth(0);
		updateCeinInfo(null);
		return info;
	}

	/** CEIN: "2 pages · PDF" (page count known when the document was captured in this session). */
	private void updateCeinInfo(Integer pages) {
		if (ceinInfoLabel == null) {
			return;
		}
		DocumentDto documentDto = getRegistrationDTo().getDocuments().get(uiFieldDTO.getId());
		String format = documentDto != null && documentDto.getFormat() != null ? documentDto.getFormat().toUpperCase() : "";
		String info = pages == null ? ceinLabel("ceinDocAttached")
				: java.text.MessageFormat.format(ceinLabel("ceinDocPages"), pages);
		ceinInfoLabel.setText(format.isEmpty() ? info : info + " \u00B7 " + format);
		ceinSourceLabel.setText(documentDto != null && documentDto.getRefNumber() != null && !documentDto.getRefNumber().isBlank()
				? documentDto.getRefNumber() : "");
	}

	/** CEIN: title of the scan window, "category - document type". */
	public String getScanTitle() {
		String category = String.join(RegistrationConstants.SLASH, uiFieldDTO.getLabel().values());
		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lang -> {
			if (uiFieldDTO.getLabel().get(lang) != null) labels.add(uiFieldDTO.getLabel().get(lang));
		});
		if (!labels.isEmpty()) {
			category = labels.get(0);
		}
		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());
		if (comboBox != null && comboBox.getValue() != null && comboBox.getValue().getName() != null) {
			return category + " \u2014 " + comboBox.getValue().getName();
		}
		return category;
	}

	private void importDocument() {
		if (!isValid()) {
			documentScanController.generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PLEASE_SELECT)
					+ RegistrationConstants.SPACE + uiFieldDTO.getSubType() + " " + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.DOCUMENT));
			return;
		}
		documentScanController.importDocument(uiFieldDTO.getId(), this);
	}

	private void clearCapturedDocuments() {
		AuditEvent auditEvent = null;
		try {
			auditEvent = AuditEvent.valueOf(String.format("REG_DOC_%S_DELETE", uiFieldDTO.getSubType()));
		} catch (Exception exception) {
			LOGGER.error("Unable to find audit event for button : " + uiFieldDTO.getSubType());

			auditEvent = AuditEvent.REG_DOC_DELETE;
		}
		auditFactory.audit(auditEvent, Components.REG_DOCUMENTS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		
		getRegistrationDTo().removeDocument(this.uiFieldDTO.getId());
		
		TextField textField = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);
		textField.setText(RegistrationConstants.EMPTY);
		
		getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(false);
		getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(false);
		getField(uiFieldDTO.getId() + PREVIEW_ICON).setManaged(true);
		getField(uiFieldDTO.getId() + CLEAR_ID).setManaged(true);
	}

	private VBox getImageGridPane(String id, String imagePath) {
		VBox box = new VBox();
		box.setId(uiFieldDTO.getId() + id);
		box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
		box.setVisible(getData() != null);
		box.setManaged(box.isVisible());
		// the MOSIP code toggles visible and sets managed=true: an invisible part takes no space
		box.visibleProperty().addListener((o, oldValue, visible) -> box.setManaged(visible));
		box.managedProperty().addListener((o, oldValue, managed) -> {
			if (managed && !box.isVisible()) {
				box.setManaged(false);
			}
		});
		return box;
	}

	private Button createScanButton(UiFieldDTO uiFieldDTO) {
		Button scanButton = ceinButton(uiFieldDTO.getId() + RegistrationConstants.BUTTON, "ceinScanCapture", "ceinOutline",
				"M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3M4 12h16");
		scanButton.getStyleClass().add(RegistrationConstants.DOCUMENT_CONTENT_BUTTON);
		return scanButton;
	}

	private void scanDocument(boolean isPreviewOnly) {

		if(!isValid()) {
			documentScanController.generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PLEASE_SELECT)
					+ RegistrationConstants.SPACE + uiFieldDTO.getSubType() + " " + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.DOCUMENT));
			return;
		}

		documentScanController.scanDocument(uiFieldDTO.getId(), this,	isPreviewOnly);
	}

	private VBox createDocRef(String id) {
		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(id + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);
		simpleTypeVBox.getStyleClass().add(RegistrationConstants.SCAN_VBOX);

		double prefWidth = simpleTypeVBox.getPrefWidth();

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {

			ResourceBundle rb = ApplicationContext.getInstance().getBundle(lCode, RegistrationConstants.LABELS);
			labels.add(rb.getString(RegistrationConstants.REF_NUMBER));
		});

		String titleText = String.join(RegistrationConstants.SLASH, labels);
		ResourceBundle rb = ApplicationContext.getInstance()
				.getBundle(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0), RegistrationConstants.LABELS);

		/** Title label */
		Label fieldTitle = getLabel(id + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.LABEL, titleText,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth);

		simpleTypeVBox.getChildren().add(fieldTitle);

		/** Text Field */
		TextField textField = getTextField(id + RegistrationConstants.DOC_TEXT_FIELD, titleText,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false);

		textField.textProperty().addListener((observable, oldValue, newValue) -> {
			Label label = (Label) getField(
					uiFieldDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.LABEL);
			if (textField.getText().isEmpty()) {
				label.setVisible(false);
			} else {
				label.setVisible(true);
			}

			if(newValue != null &&
					getRegistrationDTo().getDocuments().containsKey(uiFieldDTO.getId())) {
				getRegistrationDTo().getDocuments().get(uiFieldDTO.getId()).setRefNumber(newValue);
			}
		});

		simpleTypeVBox.getChildren().add(textField);
		return simpleTypeVBox;
	}

	private VBox create(UiFieldDTO uiFieldDTO) {

		String fieldName = uiFieldDTO.getId();

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		double prefWidth = 300;

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiFieldDTO.getLabel().get(lCode));
		});
		String titleText = String.join(RegistrationConstants.SLASH, labels) + getMandatorySuffix(uiFieldDTO);

		/** Title label */
		Label fieldTitle = getLabel(fieldName + RegistrationConstants.LABEL, titleText,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth);
		simpleTypeVBox.getChildren().add(fieldTitle);

		/** comboBox Field */
		ComboBox<DocumentCategoryDto> comboBox = getComboBox(fieldName, titleText,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false);
		simpleTypeVBox.getChildren().add(comboBox);

		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			if (comboBox.getSelectionModel().getSelectedItem() != null) {
				String selectedCode = comboBox.getSelectionModel().getSelectedItem().getCode();

				if(getRegistrationDTo().getDocuments().containsKey(uiFieldDTO.getId()) &&
						!selectedCode.equals(getRegistrationDTo().getDocuments().get(uiFieldDTO.getId()).getType())) {
					LOGGER.error("As selected document is not part of applicantype based filtered doc categories, clearing previously captured document : {}",
							uiFieldDTO.getId());
					clearCapturedDocuments();
				}

				List<String> toolTipTextList = new ArrayList<>();

				for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
					DocumentType documentType = masterSyncService.getDocumentType(selectedCode, langCode);
					if (documentType != null) {
						toolTipTextList.add(documentType.getName());
					}
				}
				Label messageLabel = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE);
				messageLabel.setText(String.join(RegistrationConstants.SLASH, toolTipTextList));
				fieldTitle.setVisible(true);
			} else {
				Label messageLabel = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE);
				messageLabel.setText(RegistrationConstants.EMPTY);
			}
		});

		comboBox.setOnMouseExited(event -> {
			if (comboBox.getTooltip() != null) {
				comboBox.getTooltip().hide();
			}

			Label messageLabel = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE);
			messageLabel.setVisible(false);
			messageLabel.setManaged(false);
		});

		comboBox.setOnMouseEntered((event -> {
			Label messageLabel = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE);
			if (messageLabel.getText()!=null && !messageLabel.getText().isEmpty()) {
				messageLabel.setVisible(true);
				messageLabel.setManaged(true);
			}
		}));

		Label messageLabel = getLabel(uiFieldDTO.getId() + RegistrationConstants.MESSAGE, null,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, simpleTypeVBox.getPrefWidth());
		messageLabel.setWrapText(true);
		messageLabel.setPrefWidth(prefWidth);
		messageLabel.setManaged(false);
		simpleTypeVBox.getChildren().add(messageLabel);

		return simpleTypeVBox;
	}

	@Override
	public void setData(Object data) {

		try {

			if (data == null) {
				getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(false);
				getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(false);
			} else {
				List<BufferedImage> bufferedImages = (List<BufferedImage>) data;
				if (bufferedImages == null || bufferedImages.isEmpty()) {
					documentScanController.generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_EMPTY));
					return;
				}

				String configuredDocType = ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.DOC_TYPE);
				byte[] byteArray =  ("pdf".equalsIgnoreCase(configuredDocType)) ?
						DocScannerUtil.asPDF(bufferedImages,
								ApplicationContext.getFloatValueFromApplicationMap(RegistrationConstants.JPG_COMPRESSION_QUALITY)) :
						DocScannerUtil.asImage(bufferedImages);

				if (byteArray == null) {
					documentScanController.generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_CONVERTION_ERR));
					return;
				}

				int docSize = (int) Math.ceil(Integer.parseInt(documentScanController
						.getValueFromApplicationContext(RegistrationConstants.DOC_SIZE)) / (double)(1024 * 1024));
				if (docSize <= (byteArray.length / (1024 * 1024))) {
					bufferedImages.clear();
					documentScanController.generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOC_SIZE).replace("1", Integer.toString(docSize)));
					return;
				}

				ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());
				DocumentDto documentDto = getRegistrationDTo().getDocuments().get(uiFieldDTO.getId());
				if (documentDto == null) {
					documentDto = new DocumentDto();
					documentDto.setFormat(configuredDocType);
					documentDto.setCategory(uiFieldDTO.getSubType());
					documentDto.setOwner(RegistrationConstants.APPLICANT);
				}

				documentDto.setType(comboBox.getValue().getCode());
				documentDto.setValue(uiFieldDTO.getSubType().concat(RegistrationConstants.UNDER_SCORE)
						.concat(comboBox.getValue().getCode()));

				documentDto.setDocument(byteArray);
				TextField textField = (TextField) getField(
						uiFieldDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);
				documentDto.setRefNumber(textField.getText());
				getRegistrationDTo().addDocument(uiFieldDTO.getId(), documentDto);
				updateCeinInfo(bufferedImages.size());

				getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(true);
				getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(true);

				getField(uiFieldDTO.getId() + PREVIEW_ICON).setManaged(true);
				getField(uiFieldDTO.getId() + CLEAR_ID).setManaged(true);

				Label label = (Label) getField(uiFieldDTO.getId()+RegistrationConstants.LABEL);
				label.getStyleClass().clear();
				label.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
			}
		} catch (IOException exception) {
			LOGGER.error("Unable to parse the buffered images to byte array ", exception);
			getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(false);
			getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(false);
			documentScanController.generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_REG_PAGE));
		}
		//refreshFields();
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDocuments().get(uiFieldDTO.getId());
	}

	@Override
	public boolean isValid() {
		String poeDocValue = documentScanController
				.getValueFromApplicationContext(RegistrationConstants.POE_DOCUMENT_VALUE);

		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());
		if (comboBox.getValue() == null) {
			comboBox.requestFocus();
			return false;
		} else if (comboBox.getValue().getCode().equalsIgnoreCase(poeDocValue)) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean isEmpty() {
		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());
		return (comboBox.getValue() == null);
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}

	@Override
	public void setListener(Node node) {
		Button scanButton = (Button) node;
		scanButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				AuditEvent auditEvent = null;
				try {
					auditEvent = AuditEvent
							.valueOf(String.format("REG_DOC_%S_SCAN", uiFieldDTO.getSubType()));
				} catch (Exception exception) {
					auditEvent = AuditEvent.REG_DOC_SCAN;
				}
				auditFactory.audit(auditEvent, Components.REG_DOCUMENTS, SessionContext.userId(),
						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

				scanDocument(false);
			}
		});
	}

	private <T> ComboBox<DocumentCategoryDto> getComboBox(String id, String titleText, String styleClass,
			double prefWidth, boolean isDisable) {
		ComboBox<DocumentCategoryDto> field = new ComboBox<DocumentCategoryDto>();
		StringConverter<T> uiRenderForComboBox = FXUtils.getInstance().getStringConverterForComboBox();
		//VBox vbox = new VBox();
		field.setId(id);
		field.setPrefWidth(prefWidth);
		field.setPromptText(titleText);
		field.setDisable(isDisable);
		field.setConverter((StringConverter<DocumentCategoryDto>) uiRenderForComboBox);
		field.getStyleClass().add(styleClass);
		field.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			getField(uiFieldDTO.getId() + RegistrationConstants.LABEL).setVisible(true);
		});

		changeNodeOrientation(field, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		return field;
	}

	private TextField getTextField(String id, String titleText, String demographicTextfield, double prefWidth,
			boolean isDisable) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(titleText);
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		textField.setPrefWidth(prefWidth);
		textField.setDisable(isDisable);

		return textField;
	}

	@Override
	public void fillData(Object data) {
		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());

		if (data != null) {
			List<DocumentCategoryDto> vals = (List<DocumentCategoryDto>) data;
			comboBox.getItems().addAll(vals);
		}

	}

	public boolean canContinue() {

		if (requiredFieldValidator == null) {
			requiredFieldValidator = ClientApplication.getApplicationContext().getBean(RequiredFieldValidator.class);
		}

		boolean isRequired = requiredFieldValidator.isRequiredField(this.uiFieldDTO, getRegistrationDTo());
		if (isRequired && getRegistrationDTo().getDocuments().get(this.uiFieldDTO.getId()) == null) {
			
			Label label = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.LABEL);
			label.getStyleClass().clear();
			label.getStyleClass().add(RegistrationConstants.DemoGraphicFieldMessageLabel);
			label.setVisible(true);
			return false;
		}

		return true;
	}

	@Override
	public void selectAndSet(Object data) {
		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());

		if(data == null) {
			comboBox.getSelectionModel().clearSelection();
			return;
		}

		if (comboBox != null) {
			DocumentDto documentDto = (DocumentDto) data;
			Optional<DocumentCategoryDto> selected = comboBox.getItems()
					.stream()
					.filter(doc -> doc.getCode().equals(documentDto.getType()))
					.findFirst();

			if(selected.isPresent()) {
				comboBox.getSelectionModel().select(selected.get());
				TextField textField = (TextField) getField(uiFieldDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);
				textField.setText(documentDto.getRefNumber());
				updateCeinInfo(null);
				getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(true);
				getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(true);
			}
			else {
				LOGGER.error("Unable to find doc from pre-reg sync, field: {}, value: {}", uiFieldDTO.getId(), documentDto.getValue());
				getRegistrationDTo().removeDocument(uiFieldDTO.getId());
				auditFactory.audit(AuditEvent.REG_DOC_DELETE, Components.REG_DOCUMENTS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
			}
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());
		List<DocumentCategoryDto> list = getDocumentCategories();
		if(list != null) {
			comboBox.getItems().clear();
			comboBox.getItems().addAll(list);

			Optional<DocumentCategoryDto> savedValue = list.stream()
					.filter( d -> getRegistrationDTo().getDocuments().containsKey(uiFieldDTO.getId())
							&& d.getCode().equals(getRegistrationDTo().getDocuments().get(uiFieldDTO.getId()).getType()))
							.findFirst();

			if(savedValue.isPresent())
				comboBox.getSelectionModel().select(savedValue.get());
		}


		if(getRegistrationDTo().getDocuments().containsKey(uiFieldDTO.getId())) {
			getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(true);
			getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(true);
		}
	}

	private List<DocumentCategoryDto> getDocumentCategories() {
		Object applicantTypeCode = requiredFieldValidator.evaluateMvelScript((String) ApplicationContext.map().getOrDefault(
				RegistrationConstants.APPLICANT_TYPE_MVEL_SCRIPT, SCRIPT_NAME), getRegistrationDTo());
		LOGGER.info("Document field {}, for applicantType : {}", uiFieldDTO.getId(), applicantTypeCode);
		if(applicantTypeCode != null) {
			return validDocumentService.getDocumentCategories((String) applicantTypeCode,
					this.uiFieldDTO.getSubType(),
					getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
		}
		return Collections.EMPTY_LIST;
	}
}
