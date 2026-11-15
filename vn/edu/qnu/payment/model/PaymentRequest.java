package vn.edu.qnu.payment.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Yêu cầu thanh toán cho một đơn hàng.
 * Các thông tin riêng của từng phương thức (số thẻ, số ví, số tài khoản, OTP...)
 * được đặt trong {@code details} để lớp trừu tượng không phụ thuộc vào chúng.
 */
public final class PaymentRequest {
    private final String orderId;
    private final String customerName;
    private final long amount;              // đơn vị: VNĐ
    private final Map<String, String> details;

    private PaymentRequest(Builder b) {
        this.orderId = b.orderId;
        this.customerName = b.customerName;
        this.amount = b.amount;
        this.details = Collections.unmodifiableMap(new HashMap<>(b.details));
    }

    public String getOrderId()      { return orderId; }
    public String getCustomerName() { return customerName; }
    public long getAmount()         { return amount; }

    /** Trả về thông tin riêng theo khóa, hoặc null nếu không có. */
    public String detail(String key) { return details.get(key); }

    public static Builder builder(String orderId, String customerName, long amount) {
        return new Builder(orderId, customerName, amount);
    }

    public static final class Builder {
        private final String orderId;
        private final String customerName;
        private final long amount;
        private final Map<String, String> details = new HashMap<>();

        private Builder(String orderId, String customerName, long amount) {
            this.orderId = orderId;
            this.customerName = customerName;
            this.amount = amount;
        }

        public Builder with(String key, String value) {
            details.put(key, value);
            return this;
        }

        public PaymentRequest build() { return new PaymentRequest(this); }
    }
}
