package io.github.adrish_s.confluence.client;

public class ConfluencePageRequest {
    private final String title;
    private final String spaceKey;
    private final String parentId;
    private final String xhtmlContent;

    public ConfluencePageRequest(String title, String spaceKey, String parentId, String xhtmlContent) {
        this.title = title;
        this.spaceKey = spaceKey;
        this.parentId = parentId;
        this.xhtmlContent = xhtmlContent;
    }

    public String getTitle() { return title; }
    public String getSpaceKey() { return spaceKey; }
    public String getParentId() { return parentId; }
    public String getXhtmlContent() { return xhtmlContent; }
}