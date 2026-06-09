package app;

import client.BisClient;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.BisSearchService;
import ui.MainView;

public class DesktopApp extends Application {
    @Override
    public void start(Stage stage) {
        BisSearchService service = new BisSearchService(new BisClient());
        MainView mainView = new MainView(service);

        Scene scene = new Scene(mainView.getRoot(), 1280, 800);
        scene.getStylesheets().add(DesktopApp.class.getResource("/ui/app.css").toExternalForm());

        stage.setTitle("Gumi BIS");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();

        mainView.loadInitialData();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
