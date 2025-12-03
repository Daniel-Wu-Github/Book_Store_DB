package com.bookstore.fx.controller;

import com.bookstore.fx.core.SceneRouter;
import com.bookstore.fx.api.ApiClient;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
        Task<java.util.Map<String,Object>> task = new Task<>() {
            @Override
            protected java.util.Map<String,Object> call() throws Exception {
                return api.me();
            }
        };
        task.setOnSucceeded(ev -> {
            var me = task.getValue();
            if (me == null || me.isEmpty()) {
                statusLabel.setText("Not authenticated");
            } else {
                String roles = String.valueOf(me.getOrDefault("roles", ""));
                statusLabel.setText("Logged in as " + me.get("username") + " (" + roles + ")");
                if (roles.contains("ROLE_ADMIN")) {
                    adminTab.setDisable(false);
                }
            }
        });
        task.setOnFailed(ev -> statusLabel.setText("User info error: " + task.getException().getMessage()));
        new Thread(task, "fetch-me-task").start();
    }

    @FXML
    public void onLogout() {
        SceneRouter.navigate("/fxml/LoginView.fxml");
    }
}
