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

	/** Number badge shown before each step label. */
	public static void numberTab(Tab tab, int number) {
		Label badge = new Label(String.valueOf(number));
		badge.getStyleClass().add("ceinStepNumber");
		StackPane holder = new StackPane(badge);
		holder.getStyleClass().add("ceinStepNumberHolder");
		tab.setGraphic(holder);
	}
}
