package com.bookstore.fx;

import com.bookstore.fx.core.SceneRouter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneRouter.init(primaryStage);
        Scene login = SceneRouter.loadScene("/fxml/LoginView.fxml");
        primaryStage.setTitle("Bookstore");
        primaryStage.setScene(login);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
