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

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.elastest.eus.session.SessionInfo;

/**
 * Service implementation for VNC capabilities.
 *
 * @author Boni Garcia (boni.garcia@urjc.es)
 * @since 0.1.1
 */
@Service
public class VncService {

    final Logger log = getLogger(lookup().lookupClass());

    SessionService sessionService;

    @Autowired
    public VncService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public ResponseEntity<String> getVnc(String sessionId) {
        Optional<SessionInfo> sessionInfo = sessionService
                .getSession(sessionId);
        ResponseEntity<String> responseEntity;
        if (sessionInfo.isPresent()) {
            responseEntity = new ResponseEntity<>(sessionInfo.get().getVncUrl(),
                    OK);
        } else {
            responseEntity = new ResponseEntity<>(NOT_FOUND);
            log.debug("<< Response: Session {} Not found -> {} ", sessionId,
                    responseEntity.getStatusCode());
        }

        return responseEntity;
    }

}
