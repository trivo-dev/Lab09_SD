package vn.edu.qnu.payment.core;

import java.io.PrintStream;

import vn.edu.qnu.payment.model.PaymentRequest;
import vn.edu.qnu.payment.model.PaymentResult;

/**
 * Lớp trừu tượng định nghĩa KHUNG THUẬT TOÁN thanh toán (Template Method).
 *
 * <pre>
 * process()  [final — template method]
 *   1. validateCommon()        bước chung   (private)
 *   2. validateSpecific()      bước thay đổi (abstract)
 *   3. calculateFee()          bước thay đổi (abstract)
 *   4. requiresOtp()?          hook          -> verifyOtp() (hook)
 *   5. executePayment()        bước thay đổi (abstract)
 *   6. saveTransaction()       bước chung   (private)
 *   7. sendReceipt()           bước chung, kênh gửi do hook receiptChannel() quyết định
 * </pre>
 *
 * Lớp con chỉ được hiện thực các bước thay đổi, không thể đổi thứ tự các bước.
 */
public abstract class PaymentProcessor {

    /** Hạn mức mặc định cho một giao dịch: 100 triệu đồng. */
    protected static final long DEFAULT_MAX_AMOUNT = 100_000_000L;

    private final TransactionLog transactionLog;
    private final PrintStream out;

    protected PaymentProcessor(TransactionLog transactionLog, PrintStream out) {
        this.transactionLog = transactionLog;
        this.out = out;
    }

    // ===================== TEMPLATE METHOD =====================

    /**
     * Khung thuật toán xử lý thanh toán. Khai báo {@code final}
     * để lớp con không thể thay đổi trình tự các bước.
     */
    public final PaymentResult process(PaymentRequest request) {
        out.println("=== Thanh toán đơn " + request.getOrderId() + " bằng " + methodName()
                + " | " + Money.format(request.getAmount()) + " ===");
        String txnId = transactionLog.nextTransactionId();
        long fee = 0;
        PaymentResult result;
        try {
            step(1, "Kiểm tra thông tin chung");
            validateCommon(request);

            step(2, "Kiểm tra thông tin riêng của " + methodName());
            validateSpecific(request);

            step(3, "Tính phí giao dịch");
            fee = calculateFee(request.getAmount());
            detail("Phí: " + Money.format(fee) + " -> Tổng trừ: " + Money.format(request.getAmount() + fee));

            if (requiresOtp()) {
                step(4, "Xác thực OTP");
                verifyOtp(request);
            } else {
                step(4, "Xác thực OTP (bỏ qua — phương thức không yêu cầu)");
            }

            step(5, "Thực hiện giao dịch qua cổng " + methodName());
            String reference = executePayment(request, request.getAmount() + fee);
            detail("Mã tham chiếu: " + reference);

            result = new PaymentResult(txnId, request.getOrderId(), methodName(),
                    request.getAmount(), fee, true, reference, "Thanh toán thành công");
        } catch (PaymentException e) {
            detail("LỖI: " + e.getMessage());
            result = new PaymentResult(txnId, request.getOrderId(), methodName(),
                    request.getAmount(), fee, false, null, e.getMessage());
        }

        step(6, "Lưu giao dịch " + txnId + " vào sổ giao dịch");
        saveTransaction(result);

        step(7, "Gửi biên nhận qua " + receiptChannel());
        sendReceipt(request, result);

        out.println("=> KẾT QUẢ: " + (result.success() ? "THÀNH CÔNG" : "THẤT BẠI") + " - " + result.message());
        out.println();
        return result;
    }

    // ===================== BƯỚC CHUNG (cố định) =====================

    private void validateCommon(PaymentRequest r) {
        if (r.getOrderId() == null || r.getOrderId().isBlank()) {
            throw new PaymentException("Thiếu mã đơn hàng");
        }
        if (r.getAmount() <= 0) {
            throw new PaymentException("Số tiền phải lớn hơn 0");
        }
        if (r.getAmount() > maxAmount()) {
            throw new PaymentException("Số tiền vượt hạn mức " + Money.format(maxAmount())
                    + " của " + methodName());
        }
    }

    private void saveTransaction(PaymentResult result) {
        transactionLog.save(result);
    }

    private void sendReceipt(PaymentRequest r, PaymentResult result) {
        detail("Gửi tới " + r.getCustomerName() + ": [" + result.transactionId() + "] "
                + (result.success()
                    ? "Đã thanh toán " + Money.format(result.total()) + " cho đơn " + r.getOrderId()
                    : "Thanh toán đơn " + r.getOrderId() + " không thành công"));
    }

    // ===================== BƯỚC THAY ĐỔI (abstract) =====================

    /** Tên phương thức thanh toán, dùng khi hiển thị và lưu sổ. */
    protected abstract String methodName();

    /** Kiểm tra dữ liệu đặc thù (số thẻ, số ví, số tài khoản...). */
    protected abstract void validateSpecific(PaymentRequest request);

    /** Tính phí giao dịch theo chính sách của từng phương thức. */
    protected abstract long calculateFee(long amount);

    /** Gọi cổng thanh toán tương ứng; trả về mã tham chiếu. */
    protected abstract String executePayment(PaymentRequest request, long totalAmount);

    // ===================== HOOK (có mặc định, lớp con tùy chọn ghi đè) =====================

    /** Có cần xác thực OTP không. Mặc định: không. */
    protected boolean requiresOtp() {
        return false;
    }

    /** Xác thực OTP. Mặc định không làm gì; chỉ được gọi khi requiresOtp() = true. */
    protected void verifyOtp(PaymentRequest request) {
    }

    /** Hạn mức tối đa cho một giao dịch. */
    protected long maxAmount() {
        return DEFAULT_MAX_AMOUNT;
    }

    /** Kênh gửi biên nhận. Mặc định: Email. */
    protected String receiptChannel() {
        return "Email";
    }

    // ===================== Tiện ích in log =====================

    private void step(int n, String text) {
        out.println("  [Bước " + n + "] " + text);
    }

    /** Lớp con dùng để in thông tin chi tiết trong một bước. */
    protected final void detail(String text) {
        out.println("           " + text);
    }
}
