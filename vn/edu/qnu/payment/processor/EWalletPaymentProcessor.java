package vn.edu.qnu.payment.processor;

import java.io.PrintStream;
import java.util.Map;

import vn.edu.qnu.payment.core.Money;
import vn.edu.qnu.payment.core.OtpGateway;
import vn.edu.qnu.payment.core.PaymentException;
import vn.edu.qnu.payment.core.PaymentProcessor;
import vn.edu.qnu.payment.core.TransactionLog;
import vn.edu.qnu.payment.model.PaymentRequest;

/**
 * Thanh toán bằng ví điện tử.
 * - Số ví là số điện thoại 10 chữ số, phải tồn tại và đủ số dư.
 * - Phí 1%, tối thiểu 1.000 đ.
 * - Hạn mức 20 triệu đồng/giao dịch, bắt buộc OTP, biên nhận qua thông báo đẩy.
 */
public class EWalletPaymentProcessor extends PaymentProcessor {

    public static final String PHONE = "phone";
    public static final String OTP = "otp";

    private static final long MIN_FEE = 1_000L;
    private static final long WALLET_LIMIT = 20_000_000L;

    /** Số dư các ví (mô phỏng hệ thống ví). */
    private final Map<String, Long> balances;

    public EWalletPaymentProcessor(TransactionLog log, PrintStream out, Map<String, Long> balances) {
        super(log, out);
        this.balances = balances;
    }

    @Override
    protected String methodName() {
        return "Ví điện tử";
    }

    @Override
    protected void validateSpecific(PaymentRequest r) {
        String phone = r.detail(PHONE);
        if (phone == null || !phone.matches("0\\d{9}")) {
            throw new PaymentException("Số ví phải là số điện thoại 10 chữ số, bắt đầu bằng 0");
        }
        if (!balances.containsKey(phone)) {
            throw new PaymentException("Không tìm thấy ví " + phone);
        }
        detail("Ví " + phone + " tồn tại, số dư " + Money.format(balances.get(phone)));
    }

    @Override
    protected long calculateFee(long amount) {
        return Math.max(MIN_FEE, Math.round(amount * 0.01));
    }

    @Override
    protected boolean requiresOtp() {
        return true;
    }

    @Override
    protected void verifyOtp(PaymentRequest r) {
        OtpGateway.verify(r.detail(OTP));
        detail("OTP ví điện tử hợp lệ");
    }

    @Override
    protected String executePayment(PaymentRequest r, long totalAmount) {
        String phone = r.detail(PHONE);
        long balance = balances.get(phone);
        if (balance < totalAmount) {
            throw new PaymentException("Số dư ví không đủ (cần " + Money.format(totalAmount)
                    + ", hiện có " + Money.format(balance) + ")");
        }
        balances.put(phone, balance - totalAmount);
        detail("Đã trừ ví, số dư còn " + Money.format(balance - totalAmount));
        return "EW-" + phone.substring(6) + "-" + r.getOrderId();
    }

    @Override
    protected long maxAmount() {
        return WALLET_LIMIT;
    }

    @Override
    protected String receiptChannel() {
        return "Thông báo đẩy trên ứng dụng ví";
    }
}
