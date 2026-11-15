package vn.edu.qnu.payment.model;

/** Kết quả xử lý một yêu cầu thanh toán. */
public record PaymentResult(
        String transactionId,
        String orderId,
        String method,
        long amount,
        long fee,
        boolean success,
        String reference,   // mã tham chiếu từ cổng thanh toán (nếu thành công)
        String message) {

    public long total() { return amount + fee; }
}
