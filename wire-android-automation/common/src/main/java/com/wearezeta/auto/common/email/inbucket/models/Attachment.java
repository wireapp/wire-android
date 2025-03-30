package com.wearezeta.auto.common.email.inbucket.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Attachment {
    private String filename;
    @JsonProperty("content-type")
    private String contentType;
    @JsonProperty("download-link")
    private String downloadLink;
    @JsonProperty("view-link")
    private String viewLink;
    private String md5;

}
