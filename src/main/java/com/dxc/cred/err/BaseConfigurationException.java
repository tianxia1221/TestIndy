package com.dxc.cred.err;

public class BaseConfigurationException extends RuntimeException{

    /**
	 * 
	 */
	private static final long serialVersionUID = 83556277739971821L;

	public BaseConfigurationException() {
        super();
    }

    public BaseConfigurationException(String message) {
        super(message);
    }

    public BaseConfigurationException(Throwable cause) {
        super(cause);
    }

    public BaseConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
