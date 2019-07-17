package io.elastest.eus.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CrossBrowserWebDriverCapabilities extends WebDriverCapabilities {
    protected List<WebDriverCapabilities> sessionsCapabilities;
    protected String sutUrl;
    protected Boolean withBrowserSync;

    public CrossBrowserWebDriverCapabilities() {
        super();
        sessionsCapabilities = new ArrayList<>();
        withBrowserSync = true;
    }

    public CrossBrowserWebDriverCapabilities(
            List<WebDriverCapabilities> subsessionsCapabilitiesList,
            String sutUrl, boolean withBrowserSync) {
        super();
        this.sessionsCapabilities = subsessionsCapabilitiesList;
        this.sutUrl = sutUrl;
        this.withBrowserSync = withBrowserSync;
    }

    public List<WebDriverCapabilities> getSessionsCapabilities() {
        return sessionsCapabilities;
    }

    public void setSessionsCapabilities(
            List<WebDriverCapabilities> sessionsCapabilities) {
        this.sessionsCapabilities = sessionsCapabilities;
    }

    public String getSutUrl() {
        return sutUrl;
    }

    public void setSutUrl(String sutUrl) {
        this.sutUrl = sutUrl;
    }

    public Boolean getWithBrowserSync() {
        return withBrowserSync;
    }

    public void setWithBrowserSync(Boolean withBrowserSync) {
        this.withBrowserSync = withBrowserSync;
    }

    @Override
    public String toString() {
        return "CrossBrowserWebDriverCapabilities [sessionsCapabilities="
                + sessionsCapabilities + ", sutUrl=" + sutUrl
                + ", withBrowserSync=" + withBrowserSync + "]";
    }
}