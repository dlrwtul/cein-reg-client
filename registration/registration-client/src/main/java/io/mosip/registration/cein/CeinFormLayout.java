package io.mosip.registration.cein;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Layout rules of the dynamic registration screens (GenericController), as validated in the
 * CEIN design:
 * <ul>
 * <li>one card per UI spec "group", titled with its groupLabel; a card without any visible field
 * is hidden;</li>
 * <li>one row per "alignmentGroup": 1 field = half width, 2 = half + half, 3 = thirds, 4 and more
 * = quarters wrapping on new lines; widths are recomputed when fields are shown / hidden (MVEL
 * visibility rules);</li>
 * <li>screens of the process are shown as numbered steps.</li>
 * </ul>
 */
public final class CeinFormLayout {

	public static final double H_GAP = 20;
	public static final double V_GAP = 16;

	private CeinFormLayout() {
	}

	/** A card holding the rows of one field group. */
	public static VBox card(String title) {
		VBox card = new VBox(18);
		card.getStyleClass().addAll("ceinCard", "ceinFormCard");
		card.setMaxWidth(Double.MAX_VALUE);
		if (title != null && !title.isBlank()) {
			Label label = new Label(title);
			label.getStyleClass().add("ceinFormCardTitle");
			label.setWrapText(true);
			card.getChildren().add(label);
		}
		return card;
	}

	/** Prepares a row (FlowPane of one alignment group) and keeps its field widths up to date. */
	public static FlowPane row(FlowPane row) {
		row.setHgap(H_GAP);
		row.setVgap(V_GAP);
		row.getStyleClass().add("ceinFormRow");
		row.setMaxWidth(Double.MAX_VALUE);
		InvalidationListener relayout = o -> relayout(row);
		row.widthProperty().addListener(relayout);
		row.getChildren().addListener((ListChangeListener<Node>) c -> {
			while (c.next()) {
				for (Node added : c.getAddedSubList()) {
					added.visibleProperty().addListener(relayout);
					added.managedProperty().addListener(relayout);
					markRequired(added);
				}
			}
			relayout(row);
		});
		return row;
	}

	/** Hides the card when none of its rows has a visible field. */
	public static void watchCard(VBox card) {
		InvalidationListener update = o -> updateCard(card);
		card.getChildren().addListener((ListChangeListener<Node>) c -> {
			while (c.next()) {
				for (Node added : c.getAddedSubList()) {
					if (added instanceof FlowPane) {
						((FlowPane) added).getChildren().forEach(n -> n.visibleProperty().addListener(update));
						((FlowPane) added).getChildren().addListener((ListChangeListener<Node>) cc -> {
							while (cc.next()) {
								cc.getAddedSubList().forEach(n -> n.visibleProperty().addListener(update));
							}
							updateCard(card);
						});
					}
				}
			}
			updateCard(card);
		});
	}

	private static void updateCard(VBox card) {
		boolean anyVisible = card.getChildren().stream().filter(n -> n instanceof FlowPane)
				.flatMap(n -> ((FlowPane) n).getChildren().stream()).anyMatch(Node::isVisible);
		boolean hasRows = card.getChildren().stream().anyMatch(n -> n instanceof FlowPane);
		boolean show = !hasRows || anyVisible;
		card.setVisible(show);
		card.setManaged(show);
	}

	static void relayout(FlowPane row) {
		// full-width controls (their width is bound by the caller) are left untouched
		List<Region> fields = row.getChildren().stream().filter(n -> n instanceof Region && n.isVisible())
				.map(n -> (Region) n).filter(r -> !r.prefWidthProperty().isBound()).collect(Collectors.toList());
		if (fields.isEmpty() || row.getWidth() <= 0) {
			return;
		}
		int count = fields.size();
		int columns = count == 1 ? 2 : Math.min(count, 4);
		double insets = row.getInsets().getLeft() + row.getInsets().getRight();
		double width = Math.floor((row.getWidth() - insets - H_GAP * (columns - 1)) / columns) - 1;
		if (width < 160) {
			width = Math.max(160, row.getWidth() - insets - 1);
		}
		for (Region field : fields) {
			field.setPrefWidth(width);
			field.setMinWidth(Region.USE_PREF_SIZE);
			field.setMaxWidth(width);
		}
	}

	/**
	 * Step badge shown before each step label (design "Stepper"): a connector line from the previous
	 * step, then a circle with the number (to do), the number on green (current) or a check (done).
	 */
	public static void numberTab(Tab tab, int number) {
		Label badge = new Label(String.valueOf(number));
		badge.getStyleClass().add("ceinStepNumber");
		javafx.scene.shape.SVGPath check = new javafx.scene.shape.SVGPath();
		check.setContent("M5 12 L10 17 L19 7");
		check.getStyleClass().add("ceinStepCheck");
		check.setVisible(false);
		StackPane circle = new StackPane(badge, check);
		circle.getStyleClass().add("ceinStepCircle");
		javafx.scene.layout.HBox holder = new javafx.scene.layout.HBox(10);
		holder.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
		holder.getStyleClass().add("ceinStepNumberHolder");
		if (number > 1) {
			Region line = new Region();
			line.getStyleClass().add("ceinStepLine");
			holder.getChildren().add(line);
		}
		holder.getChildren().add(circle);
		tab.setGraphic(holder);
	}

	/** Keeps the done / current / to-do state of the step badges in line with the selected tab. */
	public static void trackSteps(javafx.scene.control.TabPane tabPane) {
		InvalidationListener update = o -> {
			int selected = tabPane.getSelectionModel().getSelectedIndex();
			for (int i = 0; i < tabPane.getTabs().size(); i++) {
				Node graphic = tabPane.getTabs().get(i).getGraphic();
				if (!(graphic instanceof javafx.scene.layout.HBox)) {
					continue;
				}
				graphic.getStyleClass().removeAll("ceinTabDone", "ceinTabCurrent", "ceinTabTodo");
				graphic.getStyleClass().add(i < selected ? "ceinTabDone" : (i == selected ? "ceinTabCurrent" : "ceinTabTodo"));
				for (Node child : ((javafx.scene.layout.HBox) graphic).getChildren()) {
					if (child instanceof StackPane) {
						((StackPane) child).getChildren().get(0).setVisible(i >= selected);
						((StackPane) child).getChildren().get(1).setVisible(i < selected);
					}
				}
			}
		};
		tabPane.getSelectionModel().selectedIndexProperty().addListener(update);
		tabPane.getTabs().addListener(update);
		update.invalidated(null);
	}

	/** Title and caption of a screen of the process (UI spec label and caption). */
	public static VBox screenHeader(String title, String caption) {
		Label titleLabel = new Label(title == null ? "" : title);
		titleLabel.getStyleClass().add("ceinScreenTitle");
		titleLabel.setWrapText(true);
		VBox header = new VBox(2, titleLabel);
		header.getStyleClass().add("ceinScreenHeader");
		if (caption != null && !caption.isBlank()) {
			Label captionLabel = new Label(caption);
			captionLabel.getStyleClass().add("ceinScreenCaption");
			captionLabel.setWrapText(true);
			header.getChildren().add(captionLabel);
		}
		return header;
	}

	/** Column headers of the document rows (same column widths as DocumentFxControl). */
	public static javafx.scene.layout.GridPane documentHeader(java.util.ResourceBundle labels) {
		javafx.scene.layout.GridPane header = new javafx.scene.layout.GridPane();
		header.setHgap(16);
		header.getStyleClass().add("ceinDocHeader");
		double[] widths = { 22, 27, 20, 31 };
		String[] keys = { "ceinDocColCategory", "ceinDocColType", "ceinDocColRef", "ceinDocColFile" };
		for (int i = 0; i < widths.length; i++) {
			javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
			column.setPercentWidth(widths[i]);
			if (i == widths.length - 1) {
				column.setHalignment(javafx.geometry.HPos.RIGHT);
			}
			header.getColumnConstraints().add(column);
			Label label = new Label(labels.containsKey(keys[i]) ? labels.getString(keys[i]) : keys[i]);
			label.getStyleClass().add("ceinOverline");
			header.add(label, i, 0);
		}
		return header;
	}

	/**
	 * Required fields: MOSIP appends " * " to the field label; the design shows a red asterisk after
	 * the label. Applied to every label of a field (and again if the label text changes).
	 */
	public static void markRequired(Node node) {
		if (node instanceof Label) {
			Label label = (Label) node;
			if (!label.getProperties().containsKey("ceinRequired")) {
				label.getProperties().put("ceinRequired", true);
				label.textProperty().addListener((o, oldValue, newValue) -> applyRequired(label));
			}
			applyRequired(label);
		} else if (node instanceof javafx.scene.Parent) {
			for (Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
				markRequired(child);
			}
		}
	}

	private static void applyRequired(Label label) {
		String text = label.getText();
		if (text == null || !text.trim().endsWith("*") || text.trim().equals("*")) {
			return;
		}
		label.setText(text.substring(0, text.lastIndexOf('*')).replaceAll("\\s+$", ""));
		Label star = new Label("*");
		star.getStyleClass().add("ceinRequiredStar");
		label.setGraphic(star);
		label.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
		label.setGraphicTextGap(4);
	}
}
