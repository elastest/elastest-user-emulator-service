package io.elastest.eus.json;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WebDriverScriptBody {
    String script;
    List<Object> args;
    String sessionId;

    public WebDriverScriptBody() {
    }

    public WebDriverScriptBody(String script, List<Object> args,
            String sessionId) {
        this.script = script;
        this.args = args;
        this.sessionId = sessionId;
    }

    public WebDriverScriptBody(WebDriverScriptBody body) {
        this.script = body.script;
        this.args = body.args;
        this.sessionId = body.sessionId;
    }

    public WebDriverScriptBody(String body)
            throws JsonParseException, JsonMappingException, IOException {

        ObjectMapper mapper = new ObjectMapper();
        WebDriverScriptBody bodyObj = mapper.readValue(body,
                WebDriverScriptBody.class);

        this.script = bodyObj.script;
        // If starts/ends with '
        if (this.script.startsWith("'") && this.script.endsWith("'")) {
            this.script = this.script.substring(1);
            this.script = this.script.substring(0, this.script.length() - 1);
        }

        this.args = bodyObj.args;
        this.sessionId = bodyObj.sessionId;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public List<Object> getArgs() {
        return args;
    }

    public void setArgs(List<Object> args) {
        this.args = args;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "WebDriverScriptBody [script=" + script + ", args=" + args
                + ", sessionId=" + sessionId + "]";
    }

    public String toJsonString() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

}
