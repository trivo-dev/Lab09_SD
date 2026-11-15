package vn.edu.qnu.payment.core;

/** Lỗi nghiệp vụ phát sinh trong một bước của quy trình thanh toán. */
public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
