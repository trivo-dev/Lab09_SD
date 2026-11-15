package vn.edu.qnu.payment.core;

/**
 * Mô phỏng dịch vụ OTP: trong bản demo, mã OTP hợp lệ luôn là {@value #DEMO_OTP}.
 */
public final class OtpGateway {
    public static final String DEMO_OTP = "123456";

    private OtpGateway() { }

    public static void verify(String otp) {
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new PaymentException("OTP phải gồm 6 chữ số");
        }
        if (!DEMO_OTP.equals(otp)) {
            throw new PaymentException("OTP không chính xác");
        }
    }
}
