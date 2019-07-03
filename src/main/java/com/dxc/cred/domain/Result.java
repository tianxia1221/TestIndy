package com.dxc.cred.domain;

public class Result {
    public Result(){}

    public Result(String message, String body, String exception){
        this.message = message;
        this.body = body;
        this.exception = exception;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    private String message;
    private String body;
    private String exception;

}
