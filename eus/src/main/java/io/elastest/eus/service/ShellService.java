/*
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.elastest.eus.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.io.CharStreams;

import io.elastest.eus.EusException;

/**
 * Utilities to execute commands on the shell.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.0.1
 */
@Service
public class ShellService {

    private final Logger log = LoggerFactory.getLogger(ShellService.class);

    public String runAndWait(String... command) {
        return runAndWaitArray(command);
    }

    public String runAndWaitArray(String[] command) {
        assert (command.length > 0);

        String commandStr = Arrays.toString(command);
        log.trace("Running command on the shell: {}", commandStr);
        String result = runAndWaitNoLog(command);
        log.trace("Result: {}", result);
        return result;
    }

    public String runAndWaitNoLog(String... command) {
        assert (command.length > 0);

        Process p;
        try {
            p = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = CharStreams.toString(
                    new InputStreamReader(p.getInputStream(), "UTF-8"));
            p.destroy();
            return output;

        } catch (IOException e) {
            throw new EusException(
                    "Exception executing command on the shell: {} "
                            + Arrays.toString(command),
                    e);
        }
    }

}
