package ui.component;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import model.BusLocation;
import model.RouteStop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

public class RouteStopCell extends ListCell<RouteStop> {
    private final Supplier<String> highlightedStopIdSupplier;
    private final Supplier<HashMap<Integer, ArrayList<BusLocation>>> busLocationsByStopSeqSupplier;

    public RouteStopCell(Supplier<String> highlightedStopIdSupplier,
                         Supplier<HashMap<Integer, ArrayList<BusLocation>>> busLocationsByStopSeqSupplier) {
        this.highlightedStopIdSupplier = highlightedStopIdSupplier;
        this.busLocationsByStopSeqSupplier = busLocationsByStopSeqSupplier;
    }

    @Override
    protected void updateItem(RouteStop item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().remove("highlighted-stop");
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        StackPane marker = new StackPane();
        marker.getStyleClass().add("timeline-marker");
        Label markerArrow = new Label("⌄");
        markerArrow.getStyleClass().add("timeline-marker-arrow");
        marker.getChildren().add(markerArrow);

        Region line = new Region();
        line.getStyleClass().add("timeline-line");

        StackPane busOverlay = new StackPane();
        busOverlay.getStyleClass().add("timeline-bus-overlay");
        ArrayList<BusLocation> buses = findBusesForStop(item);
        if (!buses.isEmpty()) {
            VBox busStack = new VBox(3);
            busStack.getStyleClass().add("bus-marker-stack");
            for (BusLocation bus : buses) {
                StackPane busMarker = new StackPane();
                busMarker.getStyleClass().add("bus-marker");

                Label busNo = new Label(bus.getBidNo());
                busNo.getStyleClass().add("bus-marker-label");

                StackPane busIcon = createBusIcon();
                busIcon.setTranslateX(22);

                StackPane.setAlignment(busNo, Pos.CENTER_LEFT);
                StackPane.setAlignment(busIcon, Pos.CENTER);
                busMarker.getChildren().addAll(busNo, busIcon);
                busStack.getChildren().add(busMarker);
            }
            busOverlay.getChildren().add(busStack);
        }

        StackPane timeline = new StackPane();
        timeline.getStyleClass().add("timeline-axis");
        StackPane.setAlignment(line, Pos.CENTER);
        StackPane.setAlignment(marker, Pos.CENTER);
        timeline.getChildren().addAll(line, marker);

        StackPane timelineLayer = new StackPane(timeline, busOverlay);
        timelineLayer.getStyleClass().add("timeline-layer");

        Label name = new Label(item.getStopName());
        name.getStyleClass().add("list-title");

        Label id = new Label(item.getServiceId());
        id.getStyleClass().add("list-subtitle");

        VBox text = new VBox(3, name, id);
        text.getStyleClass().add("route-stop-text");
        HBox row = new HBox(10);
        row.getStyleClass().add("route-stop-row");
        row.getChildren().add(timelineLayer);
        row.getChildren().add(text);

        String highlightedStopId = highlightedStopIdSupplier.get();
        if (highlightedStopId != null && highlightedStopId.equals(item.getServiceId())) {
            getStyleClass().add("highlighted-stop");
        }

        setText(null);
        setGraphic(row);
    }

    private StackPane createBusIcon() {
        StackPane icon = new StackPane();
        icon.getStyleClass().add("bus-front-icon");

        VBox face = new VBox(2);
        face.getStyleClass().add("bus-front-face");

        Region window = new Region();
        window.getStyleClass().add("bus-front-window");

        Region bumper = new Region();
        bumper.getStyleClass().add("bus-front-bumper");

        HBox wheels = new HBox(12);
        wheels.getStyleClass().add("bus-front-wheels");
        Region leftWheel = new Region();
        leftWheel.getStyleClass().add("bus-front-wheel");
        Region rightWheel = new Region();
        rightWheel.getStyleClass().add("bus-front-wheel");
        wheels.getChildren().addAll(leftWheel, rightWheel);

        face.getChildren().addAll(window, bumper, wheels);
        icon.getChildren().add(face);
        return icon;
    }

    private ArrayList<BusLocation> findBusesForStop(RouteStop stop) {
        int stopSeq = parseInt(stop.getBrsSeqno());
        if (stopSeq < 0) {
            return new ArrayList<>();
        }
        HashMap<Integer, ArrayList<BusLocation>> busesByStopSeq = busLocationsByStopSeqSupplier.get();
        if (busesByStopSeq == null || busesByStopSeq.isEmpty()) {
            return new ArrayList<>();
        }
        return busesByStopSeq.getOrDefault(stopSeq, new ArrayList<>());
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
