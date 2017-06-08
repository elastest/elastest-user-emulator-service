package io.elastest.eus.api;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2017-06-01T16:29:27.571+02:00")

public class NotFoundException extends ApiException {

    private static final long serialVersionUID = 1L;
    private int code;

    public NotFoundException(int code, String msg) {
        super(code, msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
