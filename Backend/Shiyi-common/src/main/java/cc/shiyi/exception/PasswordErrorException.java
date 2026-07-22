package cc.shiyi.exception;

import cc.shiyi.constant.MessageConstant;

public class PasswordErrorException extends BaseException{
    public PasswordErrorException() {
    }
    public PasswordErrorException(String msg) {
        super(msg);
    }
}
