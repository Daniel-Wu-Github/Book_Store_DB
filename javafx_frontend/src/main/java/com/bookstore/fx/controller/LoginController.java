package com.bookstore.fx.controller;

import com.bookstore.fx.api.ApiClient;
import com.bookstore.fx.core.SceneRouter;
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
        try {
            boolean ok = api.login(u, p);
            if (ok) {
                statusLabel.setText("");
                SceneRouter.navigate("/fxml/MainView.fxml");
            } else {
                statusLabel.setText("Login failed");
            }
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    public void goRegister() {
        SceneRouter.navigate("/fxml/RegisterView.fxml");
    }
}
