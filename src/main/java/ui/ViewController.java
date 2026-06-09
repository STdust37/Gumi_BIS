package ui;

import javafx.scene.Parent;

public interface ViewController {
    Parent getRoot();

    void loadInitialData();
}
