package io.mosip.registration.cein;

import java.util.List;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.controller.reg.HeaderController;
import io.mosip.registration.controller.reg.PacketHandlerController;
import io.mosip.registration.enums.Role;
import io.mosip.registration.service.packet.ReRegistrationService;
import io.mosip.registration.service.packet.RegistrationApprovalService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * CEIN application shell: every screen shown after login is placed next to a collapsible side
 * navigation (see BaseController.getScene). The navigation only calls the existing MOSIP actions.
 */
public final class CeinShell {

	private static final Logger LOGGER = AppConfig.getLogger(CeinShell.class);

	private static final String LOGIN_SCREEN_ID = "loginScreen";
	private static boolean collapsed;

	private CeinShell() {
	}

	/** @return the node to set as scene root: the screen itself, or the shell around it. */
	public static Parent wrap(Parent screen) {
		try {
			if (screen == null || LOGIN_SCREEN_ID.equals(screen.getId()) || !SessionContext.isSessionContextAvailable()
					|| SessionContext.userContext() == null || SessionContext.userContext().getUserId() == null) {
				return screen;
			}
			if (screen.getStyleClass().contains("ceinShell")) {
				return screen;
			}
			BorderPane shell = new BorderPane();
			shell.getStyleClass().add("ceinShell");
			// code checks scene.getRoot().getId() (e.g. "mainBox" for the home page)
			shell.setId(screen.getId());
			shell.setLeft(buildNav(activeKey(screen.getId()), isSupervisor()));
			fill(screen);
			shell.setCenter(screen);
			setTitle(screen, titleFor(screen.getId()));
			return shell;
		} catch (RuntimeException e) {
			LOGGER.error("CEIN shell not applied", e);
			return screen;
		}
	}

	/** Page title shown in the top bar (Header.fxml) of the given screen. */
	public static void setTitle(Node screen, String title) {
		if (screen == null || title == null) {
			return;
		}
		Node label = screen.lookup("#ceinPageTitle");
		if (label instanceof Label) {
			((Label) label).setText(title);
		}
	}

	private static String titleFor(String id) {
		if (id == null) return null;
		switch (id) {
		case "mainBox": return label("ceinHomeTitle", "Accueil");
		case "uploadPacketRoot": return label("ceinTitlePackets", "Envoi des paquets");
		case "eodRoot": return label("ceinTitleApproval", "Approbation");
		case "reRegistrationRootPane": return label("ceinTitleReregistration", "Réinscription");
		case "syncDataPaneId": return label("ceinTitleSync", "Synchronisation");
		case "dashBoardRootPane": return label("ceinTitleDashboard", "Tableau de bord");
		case "onBoardRoot": return label("ceinTitleOnboard", "Enrôlement de l'opérateur");
		case "deviceSettingsRoot": case "configSettingsRoot": case "jobSettingsRoot": return label("ceinTitleSettings", "Paramètres");
		default: return null;
		}
	}

	private static String activeKey(String id) {
		if (id == null) return "home";
		switch (id) {
		case "uploadPacketRoot": return "packets";
		case "eodRoot": return "approval";
		case "reRegistrationRootPane": return "reregistration";
		case "syncDataPaneId": return "sync";
		default: return "home";
		}
	}

	/** Shell without session checks, used by the off-screen preview tool only. */
	public static Parent preview(Parent screen, String active, boolean supervisor, boolean collapsedState) {
		collapsed = collapsedState;
		BorderPane shell = new BorderPane();
		shell.getStyleClass().add("ceinShell");
		shell.setLeft(buildNav(active, supervisor));
		fill(screen);
		shell.setCenter(screen);
		return shell;
	}

	/** MOSIP screens declare max size = pref size: let them take all the space next to the navigation. */
	private static void fill(Parent screen) {
		if (screen instanceof javafx.scene.layout.Region) {
			javafx.scene.layout.Region region = (javafx.scene.layout.Region) screen;
			region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			region.setMinSize(0, 0);
		}
	}

	private static VBox buildNav(String active, boolean supervisor) {
		VBox nav = new VBox();
		nav.getStyleClass().add("ceinNav");
		if (collapsed) nav.getStyleClass().add("ceinNavCollapsed");

		HBox flagBar = new HBox(band("#00853F"), band("#FDEF42"), band("#E31B23"));
		flagBar.getStyleClass().add("ceinNavFlagBar");

		Label brand = new Label("CEIN");
		brand.getStyleClass().add("ceinNavBrand");
		Label brandSub = new Label(label("ceinNavSubtitle", "Enregistrement"));
		brandSub.getStyleClass().add("ceinMuted");
		VBox brandText = new VBox(0, brand, brandSub);
		HBox brandRow = new HBox(12, flag(), brandText);
		brandRow.getStyleClass().add("ceinNavBrandRow");
		brandRow.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
		brandText.setVisible(!collapsed);
		brandText.setManaged(!collapsed);

		VBox items = new VBox(4);
		items.getStyleClass().add("ceinNavItems");
		items.getChildren().add(item("home", label("ceinNavHome", "Accueil"), ICON_HOME, active, null, CeinShell::goHome));
		items.getChildren().add(item("packets", label("ceinNavPackets", "Paquets"), ICON_PACKETS, active, null,
				() -> bean(PacketHandlerController.class).uploadPacketToServer()));
		if (supervisor) {
			items.getChildren().add(item("approval", label("ceinNavApproval", "Approbation"), ICON_APPROVAL, active,
					countPendingApproval(), () -> bean(PacketHandlerController.class).approvePacket()));
			items.getChildren().add(item("reregistration", label("ceinNavReregistration", "Réinscription"), ICON_REREG,
					active, countReRegistration(), () -> bean(PacketHandlerController.class).loadReRegistrationScreen()));
		}
		items.getChildren().add(item("sync", label("ceinNavSync", "Synchronisation"), ICON_SYNC, active, null,
				() -> bean(PacketHandlerController.class).syncData()));
		items.getChildren().add(item("settings", label("ceinNavSettings", "Paramètres"), ICON_SETTINGS, active, null,
				() -> bean(HeaderController.class).openSettings()));

		Region spacer = new Region();
		VBox.setVgrow(spacer, Priority.ALWAYS);

		VBox footer = new VBox(4);
		footer.getStyleClass().add("ceinNavFooter");
		Label center = new Label(centerName());
		center.getStyleClass().add("ceinNavCenter");
		Label machine = new Label(machineInfo());
		machine.getStyleClass().add("ceinMuted");
		VBox footerText = new VBox(2, center, machine);
		footerText.setVisible(!collapsed);
		footerText.setManaged(!collapsed);
		Button toggle = new Button(collapsed ? "" : label("ceinNavCollapse", "Réduire le menu"));
		if (collapsed) toggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		toggle.getStyleClass().addAll("ceinNavItem", "ceinNavToggle");
		toggle.setGraphic(icon(ICON_COLLAPSE, collapsed));
		toggle.setMaxWidth(Double.MAX_VALUE);
		toggle.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
		toggle.setTooltip(new Tooltip(collapsed ? label("ceinNavExpand", "Agrandir le menu") : label("ceinNavCollapse", "Réduire le menu")));
		toggle.setOnAction(e -> {
			collapsed = !collapsed;
			Node parent = nav.getParent();
			if (parent instanceof BorderPane) {
				((BorderPane) parent).setLeft(buildNav(active, supervisor));
			}
		});
		footer.getChildren().addAll(footerText, toggle);

		nav.getChildren().addAll(flagBar, brandRow, items, spacer, footer);
		return nav;
	}

	private static Button item(String key, String text, String svg, String active, Integer count, Runnable action) {
		Button button = new Button();
		button.getStyleClass().add("ceinNavItem");
		if (key.equals(active)) button.getStyleClass().add("ceinNavItemActive");
		button.setMaxWidth(Double.MAX_VALUE);
		button.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
		boolean hasCount = count != null && count > 0;
		button.setTooltip(new Tooltip(hasCount ? text + " (" + count + ")" : text));
		if (collapsed) {
			Node graphic = icon(svg, false);
			if (hasCount) {
				Label badge = new Label(String.valueOf(count));
				badge.getStyleClass().add("ceinNavBadgeMini");
				StackPane withBadge = new StackPane(graphic, badge);
				StackPane.setAlignment(badge, Pos.TOP_RIGHT);
				withBadge.setMinSize(32, 24);
				graphic = withBadge;
			}
			button.setGraphic(graphic);
			button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		} else {
			Label name = new Label(text);
			name.getStyleClass().add("ceinNavLabel");
			HBox content = new HBox(12, icon(svg, false), name);
			content.setAlignment(Pos.CENTER_LEFT);
			if (hasCount) {
				Label badge = new Label(String.valueOf(count));
				badge.getStyleClass().add("ceinNavBadge");
				content.getChildren().addAll(spacerH(), badge);
			}
			content.prefWidthProperty().bind(button.widthProperty().subtract(26));
			button.setGraphic(content);
			button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}
		button.setOnAction(e -> run(action));
		return button;
	}

	private static Region spacerH() {
		Region r = new Region();
		HBox.setHgrow(r, Priority.ALWAYS);
		return r;
	}

	private static void run(Runnable action) {
		try {
			action.run();
		} catch (RuntimeException e) {
			LOGGER.error("CEIN navigation action failed", e);
		}
	}

	private static void goHome() {
		bean(HeaderController.class).redirectHome(null);
	}

	private static <T> T bean(Class<T> type) {
		return ClientApplication.getApplicationContext().getBean(type);
	}

	private static boolean isSupervisor() {
		try {
			return Role.hasSupervisorRole(SessionContext.userContext().getRoles());
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static Integer countPendingApproval() {
		try {
			List<?> list = bean(RegistrationApprovalService.class)
					.getEnrollmentByStatus(RegistrationClientStatusCode.CREATED.getCode());
			return list == null ? 0 : list.size();
		} catch (Exception e) {
			return null;
		}
	}

	private static Integer countReRegistration() {
		try {
			List<?> list = bean(ReRegistrationService.class).getAllReRegistrationPackets();
			return list == null ? 0 : list.size();
		} catch (Exception e) {
			return null;
		}
	}

	private static String centerName() {
		try {
			String name = SessionContext.userContext().getRegistrationCenterDetailDTO().getRegistrationCenterName();
			return name == null ? "" : name;
		} catch (RuntimeException e) {
			return "";
		}
	}

	private static String machineInfo() {
		String machine;
		try {
			machine = RegistrationSystemPropertiesChecker.getMachineId();
		} catch (RuntimeException e) {
			machine = "";
		}
		return machine;
	}

	private static String label(String key, String fallback) {
		try {
			return ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString(key);
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	private static Region band(String color) {
		Region r = new Region();
		r.setStyle("-fx-background-color: " + color + ";");
		HBox.setHgrow(r, Priority.ALWAYS);
		r.setMaxWidth(Double.MAX_VALUE);
		r.setPrefHeight(4);
		r.setMinHeight(4);
		return r;
	}

	private static Node flag() {
		HBox flag = new HBox();
		Region g = new Region(); g.setStyle("-fx-background-color: #00853F;");
		Region y = new Region(); y.setStyle("-fx-background-color: #FDEF42;");
		Region r = new Region(); r.setStyle("-fx-background-color: #E31B23;");
		for (Region b : new Region[] { g, y, r }) {
			b.setPrefSize(12, 24);
			b.setMinSize(12, 24);
		}
		SVGPath star = new SVGPath();
		star.setContent("M6 1.5 L7.3 5.5 L11.5 5.5 L8.1 8 L9.4 12 L6 9.5 L2.6 12 L3.9 8 L0.5 5.5 L4.7 5.5 Z");
		star.setStyle("-fx-fill: #00853F;");
		star.setScaleX(0.75);
		star.setScaleY(0.75);
		flag.getChildren().addAll(g, new StackPane(y, star), r);
		flag.setMinSize(36, 24);
		flag.setMaxSize(36, 24);
		return flag;
	}

	private static Node icon(String content, boolean mirrored) {
		SVGPath path = new SVGPath();
		path.setContent(content);
		path.getStyleClass().add("ceinNavIcon");
		StackPane box = new StackPane(path);
		box.setMinSize(20, 20);
		box.setPrefSize(20, 20);
		box.setMaxSize(20, 20);
		if (mirrored) box.setScaleX(-1);
		return box;
	}

	// 24x24 stroke icons (same drawings as the validated design)
	private static final String ICON_HOME = "M3 10.5 L12 3 L21 10.5 M5 9.5 L5 21 L19 21 L19 9.5";
	private static final String ICON_PACKETS = "M21 8 L12 3 L3 8 L3 16 L12 21 L21 16 Z M3 8 L12 13 L21 8 M12 13 L12 21";
	private static final String ICON_APPROVAL = "M9 12 L11 14 L15 10 M12 3 L20 6 L20 12 C20 16.5 16.6 20.3 12 21 C7.4 20.3 4 16.5 4 12 L4 6 Z";
	private static final String ICON_REREG = "M4 4 L20 4 L20 16 L8 16 L4 20 Z M9 9 L15 9 M9 12 L13 12";
	private static final String ICON_SYNC = "M20 11 A8 8 0 0 0 5.4 6.5 L3 9 M3 4 L3 9 L8 9 M4 13 A8 8 0 0 0 18.6 17.5 L21 15 M21 20 L21 15 L16 15";
	private static final String ICON_SETTINGS = "M4 6 L14 6 M18 6 L20 6 M4 12 L8 12 M12 12 L20 12 M4 18 L16 18 M16 4 A2 2 0 1 0 16 8 A2 2 0 1 0 16 4 M10 10 A2 2 0 1 0 10 14 A2 2 0 1 0 10 10 M18 16 A2 2 0 1 0 18 20 A2 2 0 1 0 18 16";
	private static final String ICON_COLLAPSE = "M3 4 L21 4 L21 20 L3 20 Z M9 4 L9 20 M16 10 L14 12 L16 14";
}
