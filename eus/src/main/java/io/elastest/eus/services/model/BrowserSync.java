package io.elastest.eus.services.model;

import java.util.ArrayList;
import java.util.List;

import io.elastest.eus.json.CrossBrowserWebDriverCapabilities;
import io.elastest.eus.session.SessionManager;

public class BrowserSync extends EusServiceModel {
    List<SessionManager> sessions;

    String guiUrl;
    String appUrl;

    CrossBrowserWebDriverCapabilities crossBrowserWebDriverCapabilities;

    public BrowserSync(
            CrossBrowserWebDriverCapabilities crossBrowserWebDriverCapabilities) {
        super(EusServiceName.BROWSERSYNC);
        this.sessions = new ArrayList<>();
        this.crossBrowserWebDriverCapabilities = crossBrowserWebDriverCapabilities;
    }

    public List<SessionManager> getSessions() {
        return sessions;
    }

    public void setSessions(List<SessionManager> sessions) {
        this.sessions = sessions;
    }

    public String getGuiUrl() {
        return guiUrl;
    }

    public void setGuiUrl(String guiUrl) {
        this.guiUrl = guiUrl;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public CrossBrowserWebDriverCapabilities getCrossBrowserWebDriverCapabilities() {
        return crossBrowserWebDriverCapabilities;
    }

    public void setCrossBrowserWebDriverCapabilities(
            CrossBrowserWebDriverCapabilities crossBrowserWebDriverCapabilities) {
        this.crossBrowserWebDriverCapabilities = crossBrowserWebDriverCapabilities;
    }

    @Override
    public String toString() {
        return "BrowserSync [sessions=" + sessions + ", guiUrl=" + guiUrl
                + ", appUrl=" + appUrl + ", crossBrowserWebDriverCapabilities="
                + crossBrowserWebDriverCapabilities + "]";
    }

}
