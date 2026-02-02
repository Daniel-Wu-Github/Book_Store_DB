package com.bookstore.fx.controller;

import com.bookstore.fx.api.ApiClient;
import com.bookstore.fx.core.SceneRouter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final ApiClient api = new ApiClient();

    @FXML
    public void initialize() {
        statusLabel.setText("");
    }

    @FXML
    public void onLogin() {
        String u = usernameField.getText();
        String p = passwordField.getText();
        if (u.isBlank() || p.isBlank()) {
            statusLabel.setText("Enter credentials");
            return;
        }
        statusLabel.setText("Logging in...");
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return api.login(u, p);
            }
        };
        task.setOnSucceeded(ev -> {
            Boolean ok = task.getValue();
            if (ok != null && ok) {
                statusLabel.setText("");
                Platform.runLater(() -> SceneRouter.navigate("/fxml/MainView.fxml"));
            } else {
                statusLabel.setText("Login failed");
            }
        });
        task.setOnFailed(ev -> statusLabel.setText("Error: " + task.getException().getMessage()));
        new Thread(task, "login-task").start();
    }

    @FXML
    public void goRegister() {
        SceneRouter.navigate("/fxml/RegisterView.fxml");
    }
}
