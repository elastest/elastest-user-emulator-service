package io.elastest.eus.json;

import java.util.HashMap;
import java.util.Map;

public class ElasTestWebdriverScript {
    String command;
    Map<String, Object> args = new HashMap<>();

    public ElasTestWebdriverScript() {
    }

    public ElasTestWebdriverScript(String command, Map<String, Object> args) {
        this.command = command;
        this.args = args;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return "ElasTestWebdriverScript [command=" + command + ", args=" + args
                + "]";
    }

    /* ************** */
    /* *** Others *** */
    /* ************** */

    public boolean isStartTestCommand() {
        return "startTest".equals(command);
    }

}
