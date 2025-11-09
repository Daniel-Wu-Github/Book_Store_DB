package com.bookstore.fx.core;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SceneRouter {
    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static Scene loadScene(String fxmlPath) {
        try {
            URL url = SceneRouter.class.getResource(fxmlPath);
            if (url == null) throw new IllegalArgumentException("FXML not found: " + fxmlPath);
            Parent root = FXMLLoader.load(url);
            return new Scene(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void navigate(String fxmlPath) {
        Scene scene = loadScene(fxmlPath);
        stage.setScene(scene);
        stage.sizeToScene();
    }
}
