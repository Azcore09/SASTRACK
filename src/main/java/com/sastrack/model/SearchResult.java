package com.sastrack.model;

public class SearchResult {
    private final String kind;
    private final String reference;
    private final String detail;
    private final String date;
    private final String status;

    public SearchResult(String kind, String reference, String detail, String date, String status) {
        this.kind = kind;
        this.reference = reference;
        this.detail = detail;
        this.date = date;
        this.status = status;
    }

    public String getKind() { return kind; }
    public String getReference() { return reference; }
    public String getDetail() { return detail; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
}
