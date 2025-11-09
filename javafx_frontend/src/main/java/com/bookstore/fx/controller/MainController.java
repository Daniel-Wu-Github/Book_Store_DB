package com.bookstore.fx.controller;

import com.bookstore.fx.core.SceneRouter;
import com.bookstore.fx.api.ApiClient;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainController {
    @FXML private TabPane tabPane;
    @FXML private Tab adminTab;
    @FXML private Label statusLabel;

    private final ApiClient api = new ApiClient();

    @FXML
    public void initialize() {
        // Fetch user info asynchronously (JavaFX thread safety) and update role label / admin access.
        new Thread(() -> {
            try {
                var me = api.me();
                Platform.runLater(() -> {
                    if (me.isEmpty()) {
                        statusLabel.setText("Not authenticated");
                    } else {
                        String roles = String.valueOf(me.getOrDefault("roles", ""));
                        statusLabel.setText("Logged in as " + me.get("username") + " (" + roles + ")");
                        if (roles.contains("ROLE_ADMIN")) {
                            adminTab.setDisable(false);
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("User info error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void onLogout() {
        SceneRouter.navigate("/fxml/LoginView.fxml");
    }
}
