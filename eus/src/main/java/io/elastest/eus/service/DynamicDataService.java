package io.elastest.eus.service;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DynamicDataService {

    @Value("${et.mon.lshttps.api:#{null}}")
    private String etMonLsHttpsApi;

    @Value("${et.mon.exec:#{null}}")
    private String etMonExec;

    private String lsHttpApi;

    @PostConstruct
    private void init() {
        this.lsHttpApi = etMonLsHttpsApi;
    }

    public void setLogstashHttpsApi(String lsHttpsUrl) {
        this.lsHttpApi = lsHttpsUrl;
    }

    public String getLogstashHttpsApi() {
        return lsHttpApi;
    }

    public String getDefaultEtMonExec() {
        return etMonExec;
    }

}
