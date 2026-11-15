package vn.edu.qnu.payment.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.qnu.payment.model.PaymentResult;

/**
 * Sổ ghi giao dịch dùng chung cho mọi phương thức thanh toán
 * (mô phỏng bảng transactions trong cơ sở dữ liệu).
 */
public class TransactionLog {
    private final List<PaymentResult> records = new ArrayList<>();
    private int sequence = 0;

    public synchronized String nextTransactionId() {
        sequence++;
        return String.format("TXN-%04d", sequence);
    }

    public synchronized void save(PaymentResult result) {
        records.add(result);
    }

    public synchronized List<PaymentResult> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    public synchronized long totalCollected() {
        return records.stream().filter(PaymentResult::success).mapToLong(PaymentResult::total).sum();
    }
}
