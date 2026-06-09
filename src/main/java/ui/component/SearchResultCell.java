package ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import model.BusStop;
import model.Route;
import ui.viewmodel.SearchResult;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SearchResultCell extends ListCell<SearchResult> {
    private final Supplier<String> keywordSupplier;
    private final Supplier<SearchResult> selectedItemSupplier;
    private final Consumer<SearchResult> selectHandler;
    private final Consumer<SearchResult> openHandler;

    public SearchResultCell(Supplier<String> keywordSupplier, Supplier<SearchResult> selectedItemSupplier,
                            Consumer<SearchResult> selectHandler, Consumer<SearchResult> openHandler) {
        this.keywordSupplier = keywordSupplier;
        this.selectedItemSupplier = selectedItemSupplier;
        this.selectHandler = selectHandler;
        this.openHandler = openHandler;

        addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            SearchResult item = getItem();
            if (isEmpty() || item == null) {
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
    protected void updateItem(SearchResult item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            syncCellBackground();
            return;
        }

        setText(null);
        setGraphic(item.isStop() ? buildStopRow(item.getStop()) : buildRouteRow(item.getRoute()));
        syncCellBackground();
    }

    private void syncCellBackground() {
        if (isEmpty()) {
            setStyle("-fx-background-color: white;");
            return;
        }
        if (getItem() == selectedItemSupplier.get()) {
            setStyle("-fx-background-color: #e4f0fb;");
        } else if (isHover()) {
            setStyle("-fx-background-color: #f6f9fc;");
        } else {
            setStyle("-fx-background-color: white;");
        }
    }

    private VBox buildRouteRow(Route route) {
        Label badge = new Label(routeBadgeText(route));
        badge.getStyleClass().add("route-type-badge");
        if ("좌석".equals(badge.getText())) {
            badge.getStyleClass().add("route-type-badge-seat");
        }

        Label title = new Label(route.getBrtId());
        title.getStyleClass().add("result-title");

        HBox titleLine = new HBox(6, badge, title);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label(value(route.getStartStop()) + " ↔ " + value(route.getLastStop()));
        subtitle.getStyleClass().add("result-subtitle");

        VBox row = new VBox(6, titleLine, subtitle);
        row.getStyleClass().add("result-row");
        return row;
    }

    private VBox buildStopRow(BusStop stop) {
        TextFlow title = highlightedTitle(stop.getStopKname());
        title.getStyleClass().add("result-title-flow");

        Label subtitle = new Label(stop.getStopServiceid() + " | 정류장");
        subtitle.getStyleClass().add("result-subtitle");

        VBox row = new VBox(6, title, subtitle);
        row.getStyleClass().add("result-row");
        return row;
    }

    private TextFlow highlightedTitle(String title) {
        String keyword = keywordSupplier.get();
        String term = keyword == null ? "" : keyword.trim();
        if (term.isBlank()) {
            return new TextFlow(titleText(title, false));
        }

        String lowerTitle = title.toLowerCase(Locale.ROOT);
        String lowerTerm = term.toLowerCase(Locale.ROOT);
        int start = lowerTitle.indexOf(lowerTerm);
        if (start < 0) {
            return new TextFlow(titleText(title, false));
        }

        int end = start + term.length();
        TextFlow flow = new TextFlow();
        if (start > 0) {
            flow.getChildren().add(titleText(title.substring(0, start), false));
        }
        flow.getChildren().add(titleText(title.substring(start, end), true));
        if (end < title.length()) {
            flow.getChildren().add(titleText(title.substring(end), false));
        }
        return flow;
    }

    private Text titleText(String value, boolean highlight) {
        Text text = new Text(value);
        text.getStyleClass().add(highlight ? "result-title-highlight" : "result-title-text");
        return text;
    }

    private String routeBadgeText(Route route) {
        String text = (route.getRemark() + " " + route.getBrtDirection()).toLowerCase(Locale.ROOT);
        if (text.contains("좌석")) {
            return "좌석";
        }
        return "일반";
    }

    private String value(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
