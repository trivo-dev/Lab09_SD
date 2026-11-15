package vn.edu.qnu.payment.processor;

import java.io.PrintStream;
import java.util.Set;

import vn.edu.qnu.payment.core.PaymentException;
import vn.edu.qnu.payment.core.PaymentProcessor;
import vn.edu.qnu.payment.core.TransactionLog;
import vn.edu.qnu.payment.model.PaymentRequest;

/**
 * Thanh toán bằng chuyển khoản ngân hàng (Internet Banking).
 * - Mã ngân hàng phải nằm trong danh sách hỗ trợ, số tài khoản 9–14 chữ số.
 * - Phí cố định 3.300 đ, miễn phí cho đơn từ 2.000.000 đ.
 * - Không yêu cầu OTP tại hệ thống bán hàng (khách xác thực trên app ngân hàng),
 *   biên nhận gửi qua SMS.
 */
public class BankTransferPaymentProcessor extends PaymentProcessor {

    public static final String BANK_CODE = "bankCode";
    public static final String ACCOUNT_NO = "accountNo";

    private static final Set<String> SUPPORTED_BANKS = Set.of("VCB", "BIDV", "MB", "TCB", "ACB");
    private static final long FIXED_FEE = 3_300L;
    private static final long FREE_FROM = 2_000_000L;

    public BankTransferPaymentProcessor(TransactionLog log, PrintStream out) {
        super(log, out);
    }

    @Override
    protected String methodName() {
        return "Chuyển khoản ngân hàng";
    }

    @Override
    protected void validateSpecific(PaymentRequest r) {
        String bank = r.detail(BANK_CODE);
        if (bank == null || !SUPPORTED_BANKS.contains(bank)) {
            throw new PaymentException("Ngân hàng " + bank + " chưa được hỗ trợ");
        }
        String account = r.detail(ACCOUNT_NO);
        if (account == null || !account.matches("\\d{9,14}")) {
            throw new PaymentException("Số tài khoản phải gồm 9–14 chữ số");
        }
        detail("Tài khoản " + bank + " " + account + " hợp lệ");
    }

    @Override
    protected long calculateFee(long amount) {
        return amount >= FREE_FROM ? 0 : FIXED_FEE;
    }

    @Override
    protected String executePayment(PaymentRequest r, long totalAmount) {
        return "FT-" + r.detail(BANK_CODE) + "-" + r.getOrderId();
    }

    // Không ghi đè requiresOtp(): dùng mặc định false nên bước OTP bị bỏ qua.

    @Override
    protected String receiptChannel() {
        return "SMS";
    }
}
