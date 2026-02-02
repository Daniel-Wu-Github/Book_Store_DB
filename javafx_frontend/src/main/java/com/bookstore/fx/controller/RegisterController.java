package com.bookstore.fx.controller;

import com.bookstore.fx.api.ApiClient;
import com.bookstore.fx.core.SceneRouter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final ApiClient api = new ApiClient();

    @FXML
    public void initialize() {
        statusLabel.setText("");
    }

    @FXML
    public void onRegister() {
        String u = usernameField.getText();
        String e = emailField.getText();
        String p = passwordField.getText();
        if (u.isBlank() || e.isBlank() || p.isBlank()) {
            statusLabel.setText("Fill all fields");
            return;
        }
        if (u.length() < 3) {
            statusLabel.setText("Username must be at least 3 characters");
            return;
        }
        if (p.length() < 6) {
            statusLabel.setText("Password must be at least 6 characters");
            return;
        }
        if (!e.matches("^[^@\n]+@[^@\n]+\\.[^@\n]+$")) {
            statusLabel.setText("Enter a valid email address");
            return;
        }
        statusLabel.setText("Registering...");
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return api.register(u, e, p);
            }
        };
        task.setOnSucceeded(ev -> {
            Boolean ok = task.getValue();
            if (ok != null && ok) {
                statusLabel.setText("Registered. Please login.");
            } else {
                statusLabel.setText("Register failed");
            }
        });
        task.setOnFailed(ev -> statusLabel.setText("Error: " + task.getException().getMessage()));
        new Thread(task, "register-task").start();
    }

    @FXML
    public void goBack() {
        SceneRouter.navigate("/fxml/LoginView.fxml");
    }
}
