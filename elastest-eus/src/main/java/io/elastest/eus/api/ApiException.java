package io.elastest.eus.api;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2017-06-01T16:29:27.571+02:00")

public class ApiException extends Exception {

    private static final long serialVersionUID = 1L;

    private int code;

    public ApiException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
