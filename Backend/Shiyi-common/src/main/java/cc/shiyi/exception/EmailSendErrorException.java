package cc.shiyi.exception;

public class EmailSendErrorException extends BaseException{
    public EmailSendErrorException(){
    }
    public EmailSendErrorException(String msg){
        super(msg);
    }
}
