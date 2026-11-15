package vn.edu.qnu.payment;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.edu.qnu.payment.core.Money;
import vn.edu.qnu.payment.core.PaymentProcessor;
import vn.edu.qnu.payment.core.TransactionLog;
import vn.edu.qnu.payment.model.PaymentRequest;
import vn.edu.qnu.payment.model.PaymentResult;
import vn.edu.qnu.payment.processor.BankTransferPaymentProcessor;
import vn.edu.qnu.payment.processor.CreditCardPaymentProcessor;
import vn.edu.qnu.payment.processor.EWalletPaymentProcessor;

/**
 * Chương trình minh họa: cùng một khung xử lý {@code process()} chạy cho
 * ba phương thức thanh toán khác nhau, gồm cả trường hợp thành công và thất bại.
 */
public class Main {

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);

        TransactionLog log = new TransactionLog();
        Map<String, Long> wallets = new HashMap<>();
        wallets.put("0905123456", 2_000_000L);
        wallets.put("0987654321", 150_000L);

        PaymentProcessor card = new CreditCardPaymentProcessor(log, out);
        PaymentProcessor wallet = new EWalletPaymentProcessor(log, out, wallets);
        PaymentProcessor bank = new BankTransferPaymentProcessor(log, out);

        // Trường hợp 1: thẻ tín dụng hợp lệ
        card.process(PaymentRequest.builder("ORD-001", "Nguyễn Văn An", 1_200_000)
                .with(CreditCardPaymentProcessor.CARD_NUMBER, "4111111111111111")
                .with(CreditCardPaymentProcessor.EXPIRY, "12/28")
                .with(CreditCardPaymentProcessor.OTP, "123456")
                .build());

        // Trường hợp 2: ví điện tử hợp lệ
        wallet.process(PaymentRequest.builder("ORD-002", "Trần Thị Bình", 450_000)
                .with(EWalletPaymentProcessor.PHONE, "0905123456")
                .with(EWalletPaymentProcessor.OTP, "123456")
                .build());

        // Trường hợp 3: chuyển khoản ngân hàng (không có bước OTP)
        bank.process(PaymentRequest.builder("ORD-003", "Lê Minh Cường", 850_000)
                .with(BankTransferPaymentProcessor.BANK_CODE, "VCB")
                .with(BankTransferPaymentProcessor.ACCOUNT_NO, "0071000123456")
                .build());

        // Trường hợp 4: ví điện tử không đủ số dư -> thất bại ở bước 5
        wallet.process(PaymentRequest.builder("ORD-004", "Phạm Thu Dung", 300_000)
                .with(EWalletPaymentProcessor.PHONE, "0987654321")
                .with(EWalletPaymentProcessor.OTP, "123456")
                .build());

        // Trường hợp 5: thẻ tín dụng nhập sai OTP -> thất bại ở bước 4
        card.process(PaymentRequest.builder("ORD-005", "Hoàng Gia Huy", 5_000_000)
                .with(CreditCardPaymentProcessor.CARD_NUMBER, "5555555555554444")
                .with(CreditCardPaymentProcessor.EXPIRY, "06/27")
                .with(CreditCardPaymentProcessor.OTP, "654321")
                .build());

        printSummary(out, log.findAll(), log.totalCollected());
    }

    private static void printSummary(PrintStream out, List<PaymentResult> rows, long collected) {
        out.println("================================ SỔ GIAO DỊCH ================================");
        out.printf("%-9s %-8s %-23s %13s %10s  %s%n", "Mã GD", "Đơn", "Phương thức", "Số tiền", "Phí", "Trạng thái");
        out.println("------------------------------------------------------------------------------");
        for (PaymentResult r : rows) {
            out.printf("%-9s %-8s %-23s %13s %10s  %s%n",
                    r.transactionId(), r.orderId(), r.method(),
                    Money.format(r.amount()), Money.format(r.fee()),
                    r.success() ? "Thành công" : "Thất bại");
        }
        out.println("------------------------------------------------------------------------------");
        out.println("Tổng tiền thu thành công: " + Money.format(collected));
    }
}
