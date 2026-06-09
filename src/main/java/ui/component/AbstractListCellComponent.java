package ui.component;

import javafx.scene.control.ListCell;

public abstract class AbstractListCellComponent<T> extends ListCell<T> {
    @Override
    protected final void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            clearCell();
            return;
        }
        renderItem(item);
    }

    protected abstract void renderItem(T item);

    protected void clearCell() {
        setText(null);
        setGraphic(null);
    }
}
