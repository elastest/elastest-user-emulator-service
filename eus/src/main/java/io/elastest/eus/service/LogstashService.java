package io.elastest.eus.service;

import static java.lang.invoke.MethodHandles.lookup;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static okhttp3.MediaType.parse;
import static okhttp3.RequestBody.create;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.PostConstruct;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.elastest.eus.external.EtmLogstashApi;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Service
public class LogstashService {
    @Value("${et.mon.lshttp.api:#{null}}")
    private String lsHttpApi;

    @Value("${et.mon.exec:#{null}}")
    private String etMonExec;

    private EtmLogstashApi logstash;
    final Logger log = getLogger(lookup().lookupClass());

    @PostConstruct
    public void postConstruct() {
        if (lsHttpApi != null && !lsHttpApi.isEmpty()) {
            Retrofit retrofit = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(lsHttpApi).build();
            logstash = retrofit.create(EtmLogstashApi.class);
        }
    }

    // public void sendBrowserConsoleToLogstash(String jsonMessages,
    // String sessionId) {
    //
    // if (lsHttpApi == null || etMonExec == null) {
    // return;
    // }
    // try {
    // String component = "tss_eus_browser_" + sessionId;
    //
    // String body = "{" + "\"component\":\"" + component + "\""
    // + ",\"exec\":\"" + etMonExec + "\""
    // + ",\"stream\":\"console\"" + ",\"messages\":"
    // + jsonMessages + "}";
    //
    // System.out.println("json " + body);
    //
    // RequestBody data = create(parse(APPLICATION_JSON), body);
    // Call<ResponseBody> response = logstash.sendMessages(data);
    //
    // System.out.println("response " + response.request());
    //
    // } catch (Exception e) {
    // log.error("Exception in send browser console log trace", e);
    // }
    // }

    public void sendBrowserConsoleToLogstash(String jsonMessages,
            String sessionId) {
        if (lsHttpApi == null || etMonExec == null) {
            return;
        }

        try {
            URL url = new URL(lsHttpApi);

            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            String component = "tss_eus_browser_" + sessionId;
            String body = "{" + "\"component\":\"" + component + "\""
                    + ",\"exec\":\"" + etMonExec + "\""
                    + ",\"stream\":\"console\"" + ",\"messages\":"
                    + jsonMessages + "}";
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            System.out.println(body);
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type",
                    "application/json; charset=UTF-8");
            http.connect();
            try (OutputStream os = http.getOutputStream()) {
                os.write(out);
            }
        } catch (Exception e) {
            log.error("Exception in send browser console log trace", e);
        }

    }

    public String getJsonMessageFromValueList(
            List<io.elastest.eus.json.WebDriverLog.Value> values) {
        String jsonMessage = "[";
        int counter = 0;
        for (io.elastest.eus.json.WebDriverLog.Value value : values) {
            jsonMessage += formatJsonMessage(value.toString());
            if (counter < values.size() - 1) {
                jsonMessage += ",";
            } else {
                jsonMessage += "]";
            }
            counter++;
        }
        return jsonMessage;
    }

    public static String formatJsonMessage(String msg) {
        return "\"" + msg + "\"";
    }

}
