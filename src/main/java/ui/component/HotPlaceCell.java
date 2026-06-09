package ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.HotPlace;

public class HotPlaceCell extends AbstractListCellComponent<HotPlace> {
    @Override
    protected void renderItem(HotPlace item) {
        Label name = new Label(item.getName());
        name.getStyleClass().add("hot-place-name");
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMaxWidth(Double.MAX_VALUE);

        Label meta = new Label(buildMeta(item));
        meta.getStyleClass().add("hot-place-meta");
        meta.setTextOverrun(OverrunStyle.ELLIPSIS);
        meta.setMaxWidth(Double.MAX_VALUE);

        VBox text = new VBox(3, name, meta);
        text.setMinWidth(0);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label infoText = new Label("i");
        infoText.getStyleClass().add("hot-place-info-text");
        StackPane info = new StackPane(infoText);
        info.getStyleClass().add("hot-place-info");
        Tooltip tooltip = new Tooltip(buildInfo(item));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
        Tooltip.install(info, tooltip);

        HBox row = new HBox(8, text, info);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("hot-place-row");
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);

        setText(null);
        setGraphic(row);
    }

    private String buildMeta(HotPlace item) {
        String category = value(item.getCategory());
        if (!category.equals("-")) {
            return category;
        }
        return value(item.getAddress());
    }

    private String buildInfo(HotPlace item) {
        String metrics = "블로그 " + displayMentionCount(item) + "건"
                + " | 리뷰 " + item.getReviewCount()
                + " | 평점 " + String.format("%.1f", item.getRating());

        return "순번 " + (getIndex() + 1)
                + "\n" + metrics
                + "\n계산식: " + buildFormula(item)
                + "\n점수 " + String.format("%.1f", item.getScore());
    }

    private int displayMentionCount(HotPlace item) {
        return Math.max(0, item.getMentionCount());
    }

    private String buildFormula(HotPlace item) {
        if (item.getMentionCount() < 0) {
            return "기본 후보 점수";
        }
        if (item.getReviewCount() <= 0 && item.getRating() <= 0.0) {
            return "언급량 정규화 x 0.65 + 유형 보정 x 0.35";
        }
        return "언급량 정규화 x 0.4 + 리뷰 수 정규화 x 0.25 + 평점 환산 x 0.2 + 유형 보정 x 0.15";
    }

    private String value(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
