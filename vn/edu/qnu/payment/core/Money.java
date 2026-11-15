package vn.edu.qnu.payment.core;

/** Tiện ích định dạng tiền VNĐ, ví dụ 1250000 -> "1.250.000 đ". */
public final class Money {
    private Money() { }

    public static String format(long vnd) {
        return String.format("%,d", vnd).replace(',', '.') + " đ";
    }
}
