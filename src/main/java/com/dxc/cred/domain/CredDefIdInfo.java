package com.dxc.cred.domain;

public class CredDefIdInfo {
    private String uuid;
    private String credDefId;
    private String revRegId;
    private String credName;
    private String createdDate;
    private String modifiedDate;
    private String deleted;
    public CredDefIdInfo() {
    }
    public CredDefIdInfo(String uuid, String credName, String credDefId, String revRegId) {
        this.uuid = uuid;
        this.credName = credName;
        this.credDefId = credDefId;
        this.revRegId = revRegId;
    }
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCredDefId() {
        return credDefId;
    }

    public void setCredDefId(String credDefId) {
        this.credDefId = credDefId;
    }

    public String getCredName() {
        return credName;
    }

    public void setCredName(String credName) {
        this.credName = credName;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }
	public String getRevRegId() {
		return revRegId;
	}
	public void setRevRegId(String revRegId) {
		this.revRegId = revRegId;
	}

}
