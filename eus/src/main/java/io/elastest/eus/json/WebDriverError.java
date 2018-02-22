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
package io.elastest.eus.json;

import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;

/**
 * Utility class for serialize W3C webdriver errors.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.5.1
 */
public class WebDriverError {

    String error;
    String message;
    String stacktrace;

    public WebDriverError() {
        // Empty default construct (needed by Jackson)
    }

    public WebDriverError(String error, String message, Exception exception) {
        this.error = error;
        this.message = message;
        this.stacktrace = getStackTrace(exception);
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    @Override
    public String toString() {
        return "error=" + getError() + ", message=" + getMessage()
                + ", stacktrace=" + getStacktrace();
    }

}
