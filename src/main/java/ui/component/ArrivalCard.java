package ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.ArrivalInfo;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ArrivalCard extends ListCell<ArrivalInfo> {
    private final Predicate<ArrivalInfo> favoriteChecker;
    private final Consumer<ArrivalInfo> favoriteToggler;
    private final Supplier<String> highlightedRouteIdSupplier;

    public ArrivalCard(Predicate<ArrivalInfo> favoriteChecker, Consumer<ArrivalInfo> favoriteToggler,
                       Supplier<String> highlightedRouteIdSupplier) {
        this.favoriteChecker = favoriteChecker;
        this.favoriteToggler = favoriteToggler;
        this.highlightedRouteIdSupplier = highlightedRouteIdSupplier;
    }

    @Override
    protected void updateItem(ArrivalInfo item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().remove("highlighted-arrival-route");
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        String highlightedRouteId = highlightedRouteIdSupplier.get();
        if (highlightedRouteId != null && highlightedRouteId.equals(item.getRouteId())) {
            getStyleClass().add("highlighted-arrival-route");
        }

        boolean favoriteActive = favoriteChecker.test(item);
        Button favorite = new Button(favoriteActive ? "★" : "☆");
        favorite.getStyleClass().add("arrival-star-button");
        if (favoriteActive) {
            favorite.getStyleClass().add("favorite-active");
        }
        favorite.setOnAction(event -> {
            event.consume();
            favoriteToggler.accept(item);
            getListView().refresh();
        });

        Label route = new Label(item.getBrtId());
        route.getStyleClass().add("arrival-route-number");

        Label direction = new Label(buildDirectionText(item));
        direction.getStyleClass().add("arrival-direction");

        Label time = new Label(buildArrivalText(item));
        time.getStyleClass().add("arrival-time");

        VBox mainText = new VBox(3, route, direction);
        mainText.getStyleClass().add("arrival-main-text");
        HBox.setHgrow(mainText, Priority.ALWAYS);

        HBox row = new HBox(16, favorite, mainText, time);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("arrival-card");

        setText(null);
        setGraphic(row);
    }

    private String buildArrivalText(ArrivalInfo item) {
        if ("정보없음".equals(item.getRemainStop()) || item.getRemainStop().isBlank()) {
            return "도착 정보 없음";
        }
        return "약 " + item.getRemainTime() + "분 [" + item.getRemainStop() + "번째 전]";
    }

    private String buildDirectionText(ArrivalInfo item) {
        if (!value(item.getLastStop()).equals("-")) {
            return value(item.getLastStop()) + " 방면";
        }
        return value(item.getStartStop()) + " -> " + value(item.getLastStop());
    }

    private String value(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
