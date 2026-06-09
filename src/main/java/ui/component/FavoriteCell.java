package ui.component;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FavoriteCell extends ListCell<String> {
    private final Consumer<String> removeHandler;
    private final Supplier<String> selectedItemSupplier;
    private final Consumer<String> selectHandler;
    private final Consumer<String> openHandler;

    public FavoriteCell(Consumer<String> removeHandler, Supplier<String> selectedItemSupplier,
                        Consumer<String> selectHandler, Consumer<String> openHandler) {
        this.removeHandler = removeHandler;
        this.selectedItemSupplier = selectedItemSupplier;
        this.selectHandler = selectHandler;
        this.openHandler = openHandler;

        addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            String item = getItem();
            if (isEmpty() || item == null || isRemoveButtonTarget(event.getTarget())) {
                return;
            }

            selectHandler.accept(item);
            if (event.getClickCount() == 2) {
                openHandler.accept(item);
            }
            event.consume();
        });

        hoverProperty().addListener((observable, oldValue, newValue) -> syncCellBackground());
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || item.isBlank()) {
            setText(null);
            setGraphic(null);
            syncCellBackground();
            return;
        }

        FavoriteRow rowData = parse(item);
        Label badge = new Label(rowData.route ? "버스" : "정류장");
        badge.getStyleClass().add("route-type-badge");
        if (!rowData.route) {
            badge.getStyleClass().add("favorite-stop-badge");
        }

        Label title = new Label(rowData.label);
        title.getStyleClass().add("result-title");

        HBox titleLine = new HBox(6, badge, title);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label(rowData.id + " | 즐겨찾기");
        subtitle.getStyleClass().add("result-subtitle");

        VBox textBox = new VBox(6, titleLine, subtitle);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button removeButton = new Button("x");
        removeButton.getStyleClass().add("favorite-remove-button");
        removeButton.setOnAction(event -> {
            event.consume();
            removeHandler.accept(item);
        });

        HBox row = new HBox(8, textBox, removeButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("favorite-result-row");

        setText(null);
        setGraphic(row);
        syncCellBackground();
    }

    private void syncCellBackground() {
        if (isEmpty()) {
            setStyle("-fx-background-color: white;");
            return;
        }
        if (getItem() != null && getItem().equals(selectedItemSupplier.get())) {
            setStyle("-fx-background-color: #e4f0fb;");
        } else if (isHover()) {
            setStyle("-fx-background-color: #f6f9fc;");
        } else {
            setStyle("-fx-background-color: white;");
        }
    }

    private boolean isRemoveButtonTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        while (node != null) {
            if (node instanceof Button && node.getStyleClass().contains("favorite-remove-button")) {
                return true;
            }
            Parent parent = node.getParent();
            node = parent;
        }
        return false;
    }

    private FavoriteRow parse(String item) {
        String[] parts = item.split("\\|", 3);
        String type = parts.length > 0 ? parts[0].trim() : "";
        String id = parts.length > 1 ? parts[1].trim() : "";
        String label = parts.length > 2 ? parts[2].trim() : item.trim();
        return new FavoriteRow(type.startsWith("노선"), id, label);
    }

    private static final class FavoriteRow {
        private final boolean route;
        private final String id;
        private final String label;

        private FavoriteRow(boolean route, String id, String label) {
            this.route = route;
            this.id = id;
            this.label = label;
        }
    }
}
