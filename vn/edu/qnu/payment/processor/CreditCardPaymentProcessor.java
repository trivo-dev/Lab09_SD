package vn.edu.qnu.payment.processor;

import java.io.PrintStream;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import vn.edu.qnu.payment.core.OtpGateway;
import vn.edu.qnu.payment.core.PaymentException;
import vn.edu.qnu.payment.core.PaymentProcessor;
import vn.edu.qnu.payment.core.TransactionLog;
import vn.edu.qnu.payment.model.PaymentRequest;

/**
 * Thanh toán bằng thẻ tín dụng quốc tế.
 * - Kiểm tra số thẻ theo thuật toán Luhn và hạn sử dụng MM/YY.
 * - Phí 2,5% giá trị đơn hàng.
 * - Bắt buộc OTP (3-D Secure).
 */
public class CreditCardPaymentProcessor extends PaymentProcessor {

    public static final String CARD_NUMBER = "cardNumber";
    public static final String EXPIRY = "expiry";
    public static final String OTP = "otp";

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/yy");

    public CreditCardPaymentProcessor(TransactionLog log, PrintStream out) {
        super(log, out);
    }

    @Override
    protected String methodName() {
        return "Thẻ tín dụng";
    }

    @Override
    protected void validateSpecific(PaymentRequest r) {
        String card = r.detail(CARD_NUMBER);
        if (card == null || !card.matches("\\d{16}")) {
            throw new PaymentException("Số thẻ phải gồm 16 chữ số");
        }
        if (!passesLuhn(card)) {
            throw new PaymentException("Số thẻ không hợp lệ (sai mã kiểm tra Luhn)");
        }
        YearMonth expiry;
        try {
            expiry = YearMonth.parse(String.valueOf(r.detail(EXPIRY)), EXPIRY_FORMAT);
        } catch (DateTimeParseException e) {
            throw new PaymentException("Hạn thẻ phải có dạng MM/YY");
        }
        if (expiry.isBefore(YearMonth.now())) {
            throw new PaymentException("Thẻ đã hết hạn");
        }
        detail("Thẻ " + mask(card) + ", hạn " + r.detail(EXPIRY) + " hợp lệ");
    }

    @Override
    protected long calculateFee(long amount) {
        return Math.round(amount * 0.025);
    }

    @Override
    protected boolean requiresOtp() {
        return true;
    }

    @Override
    protected void verifyOtp(PaymentRequest r) {
        OtpGateway.verify(r.detail(OTP));
        detail("OTP 3-D Secure hợp lệ");
    }

    @Override
    protected String executePayment(PaymentRequest r, long totalAmount) {
        String card = r.detail(CARD_NUMBER);
        return "VISA-AUTH-" + card.substring(12) + "-" + r.getOrderId();
    }

    // Kênh biên nhận giữ mặc định (Email).

    static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    private static String mask(String card) {
        return "**** **** **** " + card.substring(12);
    }
}
