/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agocontrol.agent;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * The shell command listener.
 *
 * @author Tommi S.E. Laukkanen
 */
public class ShellCommandListener implements CommandListener {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(ShellCommandListener.class);

    @Override
    public final Map<String, Object> commandReceived(final Map<String, Object> command) {
        LOGGER.debug("Processing command: " + command.toString());

        if ("screenon".equals(command.get("command"))) {
            executeShellCommand("/usr/bin/xset dpms force on");
        }

        return null;
    }

    /**
     * Executes requested shell command.
     *
     * @param cmd the shell command to execute
     */
    private void executeShellCommand(String cmd) {
        LOGGER.debug("Executing: " + cmd);
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmd);
            process.waitFor();

            String line;

            BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = error.readLine()) != null){
                LOGGER.error(line);
            }
            error.close();

            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = input.readLine()) != null){
                LOGGER.debug(line);
            }

            input.close();

            LOGGER.debug("Completed: " + cmd);
        } catch (final Throwable t) {
            LOGGER.error("Error executing shell command: " + cmd, t);
        }
    }
}
