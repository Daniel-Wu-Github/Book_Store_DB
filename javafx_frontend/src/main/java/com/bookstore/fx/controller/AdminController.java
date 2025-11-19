package com.bookstore.fx.controller;

import com.bookstore.fx.api.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Map;

public class AdminController {
    @FXML private TableView<Map<String,Object>> ordersTable;
    @FXML private TableColumn<Map<String,Object>, Number> idCol;
    @FXML private TableColumn<Map<String,Object>, String> userCol;
    @FXML private TableColumn<Map<String,Object>, String> totalCol;
    @FXML private TableColumn<Map<String,Object>, String> paymentCol;
    @FXML private TableColumn<Map<String,Object>, String> statusCol;
    @FXML private TableColumn<Map<String,Object>, Boolean> emailedCol;
    @FXML private Label statusLabel;
    // users
    @FXML private TableView<Map<String,Object>> usersTable;
    @FXML private TableColumn<Map<String,Object>, Number> uIdCol;
    @FXML private TableColumn<Map<String,Object>, String> uNameCol;
    @FXML private TableColumn<Map<String,Object>, String> uEmailCol;
    @FXML private TableColumn<Map<String,Object>, String> uRolesCol;
    @FXML private TableColumn<Map<String,Object>, Boolean> uEnabledCol;
    @FXML private Label usersStatus;
    // books
    @FXML private TableView<Map<String,Object>> adminBooksTable;
    @FXML private TableColumn<Map<String,Object>, Number> bIdCol;
    @FXML private TableColumn<Map<String,Object>, String> bTitleCol;
    @FXML private TableColumn<Map<String,Object>, String> bAuthorCol;
    @FXML private TableColumn<Map<String,Object>, Number> bPriceCol;
    @FXML private TableColumn<Map<String,Object>, Number> bStockCol;
    @FXML private Label booksStatus;

    private final ApiClient api = new ApiClient();
    private final ObservableList<Map<String,Object>> ordersData = FXCollections.observableArrayList();
    private final ObservableList<Map<String,Object>> usersData = FXCollections.observableArrayList();
    private final ObservableList<Map<String,Object>> adminBooksData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // orders table (present only in Orders subview)
        if (ordersTable != null) {
            idCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(num(cd.getValue().get("id"))));
            userCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("username"))));
            totalCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("totalAmount"))));
            paymentCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("paymentStatus"))));
            statusCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("orderStatus"))));
            emailedCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>((Boolean)cd.getValue().getOrDefault("emailed", Boolean.FALSE)));
            ordersTable.setItems(ordersData);
        }
        if (statusLabel != null) statusLabel.setText("");

        // users table
        if (usersTable != null) {
            uIdCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(num(cd.getValue().get("id"))));
            uNameCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("username"))));
            uEmailCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("email"))));
            uRolesCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("roles"))));
            uEnabledCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>((Boolean)cd.getValue().getOrDefault("enabled", Boolean.FALSE)));
            usersTable.setItems(usersData);
        }

        // admin books table
        if (adminBooksTable != null) {
            bIdCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(num(cd.getValue().get("id"))));
            bTitleCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("title"))));
            bAuthorCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("author"))));
            bPriceCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(num(cd.getValue().get("price"))));
            bStockCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(num(cd.getValue().get("stock"))));
            adminBooksTable.setItems(adminBooksData);
        }
    }

    @FXML
    public void onRefresh() {
        try {
            List<Map<String,Object>> list = api.adminListOrders();
            ordersData.setAll(list);
            statusLabel.setText("Loaded " + list.size() + " orders");
        } catch (Exception e) {
            statusLabel.setText("Refresh error: " + e.getMessage());
        }
    }

    // --- Users ---
    public void onRefreshUsers() {
        try {
            List<Map<String,Object>> list = api.adminListUsers();
            usersData.setAll(list);
            usersStatus.setText("Loaded " + list.size() + " users");
        } catch (Exception e) {
            usersStatus.setText("Users refresh error: " + e.getMessage());
        }
    }

    public void onNewUser() {
        // naive prompt-based creation — can be improved with a dialog
        TextInputDialog dlg = new TextInputDialog();
        dlg.setHeaderText("Create new user (format: username,email,password,roles)");
        dlg.setContentText("Enter: username,email,password,roles");
        dlg.showAndWait().ifPresent(s -> {
            String[] parts = s.split(",");
            if (parts.length < 3) { usersStatus.setText("Invalid input"); return; }
            try {
                Map<String,Object> req = Map.of(
                        "username", parts[0].trim(),
                        "email", parts[1].trim(),
                        "password", parts[2].trim(),
                        "roles", parts.length > 3 ? parts[3].trim() : "ROLE_USER",
                        "enabled", true
                );
                Map<String,Object> created = api.adminCreateUser(req);
                usersData.add(created);
                usersStatus.setText("User created: " + created.get("username"));
            } catch (Exception e) { usersStatus.setText("Create user error: " + e.getMessage()); }
        });
    }

    public void onDeleteUser() {
        Map<String,Object> sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) { usersStatus.setText("Select a user"); return; }
        Number id = num(sel.get("id"));
        if (id == null) { usersStatus.setText("Invalid id"); return; }
        try {
            boolean ok = api.adminDeleteUser(id.longValue());
            if (ok) { usersData.remove(sel); usersStatus.setText("Deleted"); }
            else usersStatus.setText("Delete failed");
        } catch (Exception e) { usersStatus.setText("Delete error: " + e.getMessage()); }
    }

    // --- Books ---
    public void onRefreshBooks() {
        try { List<Map<String,Object>> list = api.searchBooks(""); adminBooksData.setAll(list); booksStatus.setText("Loaded " + list.size() + " books"); }
        catch (Exception e) { booksStatus.setText("Books refresh error: " + e.getMessage()); }
    }

    public void onNewBook() {
        // open a minimal dialog to collect title/author/price/stock
        TextInputDialog dlg = new TextInputDialog(); dlg.setHeaderText("Create book: title,author,price,stock"); dlg.setContentText("Enter comma-separated values");
        dlg.showAndWait().ifPresent(s -> {
            String[] p = s.split(","); if (p.length < 4) { booksStatus.setText("Invalid input"); return; }
            try {
                Map<String,Object> req = Map.of(
                        "title", p[0].trim(),
                        "author", p[1].trim(),
                        "isbn", "",
                        "price", Double.parseDouble(p[2].trim()),
                        "stock", Integer.parseInt(p[3].trim()),
                        "description", ""
                );
                Map<String,Object> created = api.createBook(req);
                adminBooksData.add(created);
                booksStatus.setText("Book created: " + created.get("title"));
            } catch (Exception e) { booksStatus.setText("Create error: " + e.getMessage()); }
        });
    }

    public void onDeleteBook() {
        Map<String,Object> sel = adminBooksTable.getSelectionModel().getSelectedItem();
        if (sel == null) { booksStatus.setText("Select a book"); return; }
        Number id = num(sel.get("id")); if (id == null) { booksStatus.setText("Invalid id"); return; }
        try {
            boolean ok = api.deleteBook(id.longValue());
            if (ok) { adminBooksData.remove(sel); booksStatus.setText("Deleted"); } else booksStatus.setText("Delete failed");
        } catch (Exception e) { booksStatus.setText("Delete error: " + e.getMessage()); }
    }

    @FXML
    public void onMarkPaid() {
        Map<String,Object> sel = ordersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Select an order");
            return;
        }
        Number id = num(sel.get("id"));
        if (id == null) {
            statusLabel.setText("Invalid ID");
            return;
        }
        try {
            boolean ok = api.adminMarkPaymentPaid(id.longValue());
            if (ok) {
                statusLabel.setText("Marked paid");
                onRefresh();
            } else {
                statusLabel.setText("Failed to mark paid");
            }
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    public void onResendEmail() {
        Map<String,Object> sel = ordersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Select an order");
            return;
        }
        Number id = num(sel.get("id"));
        if (id == null) {
            statusLabel.setText("Invalid ID");
            return;
        }
        try {
            boolean ok = api.adminResendOrderEmail(id.longValue());
            if (ok) {
                statusLabel.setText("Email resent");
                onRefresh();
            } else {
                statusLabel.setText("Resend failed");
            }
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static Number num(Object o) {
        if (o instanceof Number) return (Number) o;
        if (o instanceof String) {
            try { return Long.parseLong((String) o); } catch (Exception ignored) {}
        }
        return null;
    }
}
