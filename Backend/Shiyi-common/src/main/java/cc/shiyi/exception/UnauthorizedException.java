package cc.shiyi.exception;

import cc.shiyi.constant.MessageConstant;

public class UnauthorizedException extends TokenException{
    public UnauthorizedException() {
    }
    public UnauthorizedException(String msg) {
        super(msg);
    }
}
