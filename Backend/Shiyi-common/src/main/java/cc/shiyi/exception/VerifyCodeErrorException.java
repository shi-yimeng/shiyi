package cc.shiyi.exception;

public class VerifyCodeErrorException extends BaseException {
    public VerifyCodeErrorException() {
    }
    public VerifyCodeErrorException(String msg) {
        super(msg);
    }
}
