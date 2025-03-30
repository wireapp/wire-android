package com.wearezeta.auto.common.email.messages;

public abstract class WireMessage extends BackendMessage {
    public static final String ZETA_CODE_HEADER_NAME = "X-Zeta-Code";
    static final String ZETA_KEY_HEADER_NAME = "X-Zeta-Key";
    public static final String ZETA_PURPOSE_HEADER_NAME = "X-Zeta-Purpose";

	public WireMessage(String msg) throws Exception {
		super(msg);
	}

	public String getXZetaPurpose() {
		return this.getHeaderValue(ZETA_PURPOSE_HEADER_NAME);
	}

    protected abstract String getExpectedPurposeValue();

    public boolean isValid() {
        try {
            return getXZetaPurpose().equals(getExpectedPurposeValue());
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
