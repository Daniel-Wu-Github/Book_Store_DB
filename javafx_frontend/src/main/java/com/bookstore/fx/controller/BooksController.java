package com.bookstore.fx.controller;

import com.bookstore.fx.api.ApiClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class BooksController {
    @FXML private TextField searchField;
    @FXML private TableView<Map<String,Object>> booksTable;
    @FXML private TableColumn<Map<String,Object>, String> titleCol;
    @FXML private TableColumn<Map<String,Object>, String> authorCol;
    @FXML private TableColumn<Map<String,Object>, BigDecimal> priceCol;
    @FXML private Spinner<Integer> qtySpinner;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField rentalDaysField;
    @FXML private TableView<Map<String,Object>> cartTable;
    @FXML private TableColumn<Map<String,Object>, String> cartTitleCol;
    @FXML private TableColumn<Map<String,Object>, String> cartTypeCol;
    @FXML private TableColumn<Map<String,Object>, Integer> cartQtyCol;
    @FXML private TableColumn<Map<String,Object>, Integer> cartRentalCol;
    @FXML private Label statusLabel;

    private final ApiClient api = new ApiClient();
    private final ObservableList<Map<String,Object>> booksData = FXCollections.observableArrayList();
    private final ObservableList<Map<String,Object>> cartData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        titleCol.setCellValueFactory(cd -> fxString(cd.getValue().get("title")));
        authorCol.setCellValueFactory(cd -> fxString(cd.getValue().get("author")));
        priceCol.setCellValueFactory(cd -> fxBigDecimal(cd.getValue().get("price")));
        booksTable.setItems(booksData);

        cartTitleCol.setCellValueFactory(cd -> fxString(cd.getValue().get("title")));
        cartTypeCol.setCellValueFactory(cd -> fxString(cd.getValue().get("itemType")));
        cartQtyCol.setCellValueFactory(cd -> fxInteger(cd.getValue().get("quantity")));
        cartRentalCol.setCellValueFactory(cd -> fxInteger(cd.getValue().get("rentalDays")));
        cartTable.setItems(cartData);

        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        typeCombo.setItems(FXCollections.observableArrayList("BUY", "RENT"));
        typeCombo.getSelectionModel().selectFirst();
        statusLabel.setText("");
        // Auto search on load to populate any seeded books
        onSearch();
    }

    @FXML
    public void onSearch() {
        String q = searchField.getText();
        try {
            List<Map<String,Object>> results = api.searchBooks(q == null ? "" : q);
            booksData.setAll(results);
            statusLabel.setText("Found " + results.size() + " books");
        } catch (Exception e) {
            statusLabel.setText("Search error: " + e.getMessage());
        }
    }

    @FXML
    public void onAddSelected() {
        Map<String,Object> selected = booksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a book first");
            return;
        }
        int qty = qtySpinner.getValue();
        String type = typeCombo.getValue();
        Integer rentalDays = null;
        if ("RENT".equals(type)) {
            String txt = rentalDaysField.getText();
            if (txt != null && !txt.isBlank()) {
                try { rentalDays = Integer.parseInt(txt); } catch (NumberFormatException ignored) {}
            }
            if (rentalDays == null) rentalDays = 7; // default rental period
        }
        Map<String,Object> cartItem = new HashMap<>();
        cartItem.put("bookId", selected.get("id"));
        cartItem.put("title", selected.get("title"));
        cartItem.put("quantity", qty);
        cartItem.put("itemType", type);
        if (rentalDays != null) cartItem.put("rentalDays", rentalDays);
        cartData.add(cartItem);
        statusLabel.setText("Added to cart");
    }

    @FXML
    public void onRemoveSelected() {
        Map<String,Object> sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            cartData.remove(sel);
            statusLabel.setText("Removed");
        }
    }

    @FXML
    public void onPlaceOrder() {
        if (cartData.isEmpty()) {
            statusLabel.setText("Cart empty");
            return;
        }
        try {
            // Build items array with required fields for backend
            List<Map<String,Object>> items = new ArrayList<>();
            for (Map<String,Object> c : cartData) {
                Map<String,Object> item = new HashMap<>();
                item.put("bookId", c.get("bookId"));
                item.put("quantity", c.get("quantity"));
                item.put("itemType", c.get("itemType"));
                if (c.containsKey("rentalDays")) item.put("rentalDays", c.get("rentalDays"));
                items.add(item);
            }
            Map<String,Object> order = api.placeOrder(items);
            statusLabel.setText("Order placed: status=" + order.get("paymentStatus"));
            cartData.clear();
        } catch (IOException | InterruptedException e) {
            statusLabel.setText("Order error: " + e.getMessage());
        }
    }

    private static javafx.beans.property.ReadOnlyObjectWrapper<String> fxString(Object v) {
        return new javafx.beans.property.ReadOnlyObjectWrapper<>(v == null ? "" : String.valueOf(v));
    }
    private static javafx.beans.property.ReadOnlyObjectWrapper<BigDecimal> fxBigDecimal(Object v) {
        BigDecimal b = null;
        if (v instanceof Number) b = new BigDecimal(((Number) v).toString());
        else if (v instanceof String) {
            try { b = new BigDecimal((String) v); } catch (Exception ignored) {}
        }
        return new javafx.beans.property.ReadOnlyObjectWrapper<>(b);
    }
    private static javafx.beans.property.ReadOnlyObjectWrapper<Integer> fxInteger(Object v) {
        Integer i = null;
        if (v instanceof Number) i = ((Number) v).intValue();
        else if (v instanceof String) {
            try { i = Integer.parseInt((String) v); } catch (Exception ignored) {}
        }
        return new javafx.beans.property.ReadOnlyObjectWrapper<>(i);
    }
}
