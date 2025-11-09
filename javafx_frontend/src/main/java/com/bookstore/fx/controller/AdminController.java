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
    @FXML private Label statusLabel;

    private final ApiClient api = new ApiClient();
    private final ObservableList<Map<String,Object>> ordersData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(num(cd.getValue().get("id"))));
        userCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("username"))));
        totalCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("totalAmount"))));
        paymentCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("paymentStatus"))));
        statusCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyObjectWrapper<>(str(cd.getValue().get("orderStatus"))));
        ordersTable.setItems(ordersData);
        statusLabel.setText("");
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

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static Number num(Object o) {
        if (o instanceof Number) return (Number) o;
        if (o instanceof String) {
            try { return Long.parseLong((String) o); } catch (Exception ignored) {}
        }
        return null;
    }
}
