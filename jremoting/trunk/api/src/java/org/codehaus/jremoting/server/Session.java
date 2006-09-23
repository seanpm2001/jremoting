/* ====================================================================
 * Copyright 2005-2006 JRemoting Committers
 * Portions copyright 2001 - 2004 Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.codehaus.jremoting.server;

import java.util.HashMap;

/**
 * Class Session
 *
 * @author Paul Hammant
 * @version $Revision: 1.2 $
 */
public class Session {

    /**
     * Session ID
     */
    private Long session;

    /**
     * A map of in-use beans
     */
    private HashMap beansInUse = new HashMap();

    /**
     * Construct an Session with a session ID
     *
     * @param session the session ID
     */
    public Session(Long session) {
        this.session = session;
    }

    /**
     * Get the Session ID
     *
     * @return The session ID
     */
    public Long getSession() {
        return session;
    }

    /**
     * Add a bean in use.
     *
     * @param referenceID The reference ID
     * @param bean        The bean to use.
     */
    public void addBeanInUse(Long referenceID, Object bean) {
        beansInUse.put(referenceID, bean);
    }

    /**
     * Remove a bean in use
     *
     * @param referenceID The reference ID.
     */
    public void removeBeanInUse(Long referenceID) {
        beansInUse.remove(referenceID);
    }
}