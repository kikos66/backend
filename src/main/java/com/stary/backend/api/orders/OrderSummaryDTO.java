package com.stary.backend.api.orders;

import java.util.List;
import java.util.Map;

public class OrderSummaryDTO {
    private double total;
    private List<Map<String, Object>> items; // each item: productId, name, qty, price, subtotal
    private String message;

    public double getTotal() {
        return total;
    }
    public void setTotal(double total) {
        this.total = total;
    }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
