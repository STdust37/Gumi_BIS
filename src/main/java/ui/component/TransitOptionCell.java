package ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import model.HotPlaceTransitOption;

public class TransitOptionCell extends ListCell<HotPlaceTransitOption> {
    @Override
    protected void updateItem(HotPlaceTransitOption item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        Label route = new Label(item.getBrtId());
        route.getStyleClass().add("transit-route-number");
        route.setMinWidth(72);
        route.setPrefWidth(72);
        route.setMaxWidth(72);
        route.setTextOverrun(OverrunStyle.CLIP);

        Label path = new Label(item.getDepartureStop().getStopKname()
                + " -> " + item.getDestinationStop().getStop().getStopKname());
        path.getStyleClass().add("transit-path");
        path.setTextOverrun(OverrunStyle.ELLIPSIS);
        path.setMaxWidth(Double.MAX_VALUE);

        int distance = item.getDestinationStop().getRoundedDistanceMeters();
        TextFlow meta = transitMeta(item.getStopsBetween(), distance);
        meta.setMaxWidth(Double.MAX_VALUE);

        VBox text = new VBox(4, path, meta);
        text.setMinWidth(0);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label arrival = new Label(arrivalText(item));
        arrival.getStyleClass().add("transit-arrival");
        arrival.setTextOverrun(OverrunStyle.ELLIPSIS);

        HBox row = new HBox(10, route, text, arrival);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("transit-option-row");

        setText(null);
        setGraphic(row);
    }

    private String arrivalText(HotPlaceTransitOption item) {
        int seconds = parseInt(item.getArrivalInfo().getRemainTimeSec());
        if (seconds >= 0) {
            return "약 " + Math.max(1, (int) Math.ceil(seconds / 60.0)) + "분";
        }
        String minutes = item.getArrivalInfo().getRemainTime();
        int parsedMinutes = parseInt(minutes);
        if (parsedMinutes >= 0) {
            return "약 " + Math.max(1, parsedMinutes) + "분";
        }
        return "도착 정보 없음";
    }

    private TextFlow transitMeta(int stopsBetween, int distance) {
        int walkingMinutes = walkingMinutes(distance);
        Text prefix = new Text(stopsBetween + "정류장 이동 / ");
        prefix.getStyleClass().add("transit-meta-text");

        Text destination = new Text("도착 정류장에서 " + distance + "m 거리 (도보로 약 ");
        destination.getStyleClass().add("transit-meta-strong");

        Text minutes = new Text(walkingMinutes + "분");
        minutes.getStyleClass().add("transit-meta-walk-time");

        Text suffix = new Text(")");
        suffix.getStyleClass().add("transit-meta-strong");

        TextFlow flow = new TextFlow(prefix, destination, minutes, suffix);
        flow.getStyleClass().add("transit-meta");
        return flow;
    }

    private int walkingMinutes(int distanceMeters) {
        return Math.max(1, (int) Math.ceil(distanceMeters / 70.0));
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
