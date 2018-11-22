package io.elastest.eus.json;

import java.util.HashMap;
import java.util.Map;

public class ElasTestWebdriverScript {
    String elastestCommand;
    Map<String, Object> args = new HashMap<>();

    public ElasTestWebdriverScript() {
    }

    public ElasTestWebdriverScript(String elastestCommand,
            Map<String, Object> args) {
        this.elastestCommand = elastestCommand;
        this.args = args;
    }

    public String getElastestCommand() {
        return elastestCommand;
    }

    public void setElastestCommand(String elastestCommand) {
        this.elastestCommand = elastestCommand;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return "ElasTestWebdriverScript [elastestCommand=" + elastestCommand
                + ", args=" + args + "]";
    }

    /* ************** */
    /* *** Others *** */
    /* ************** */

    public boolean isStartTestCommand() {
        return "startTest".equals(elastestCommand);
    }

}
