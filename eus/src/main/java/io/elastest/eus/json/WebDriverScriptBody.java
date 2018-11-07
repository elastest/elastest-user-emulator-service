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

    public WebDriverScriptBody() {
    }

    public WebDriverScriptBody(String script, List<Object> args) {
        this.script = script;
        this.args = args;
    }

    public WebDriverScriptBody(WebDriverScriptBody body) {
        this.script = body.script;
        this.args = body.args;
    }

    public WebDriverScriptBody(String body)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        WebDriverScriptBody bodyObj = mapper.readValue(body,
                WebDriverScriptBody.class);
        this.script = bodyObj.script;
        this.args = bodyObj.args;
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

    @Override
    public String toString() {
        return "WebDriverScriptBody [script=" + script + ", args=" + args + "]";
    }

    public String toJsonString() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

}
