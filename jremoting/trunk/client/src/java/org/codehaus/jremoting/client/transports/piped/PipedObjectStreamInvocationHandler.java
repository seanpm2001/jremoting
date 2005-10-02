/* ====================================================================
 * Copyright 2005 JRemoting Committers
 * Portions copyright 2001 - 2004 Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.codehaus.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.codehaus.jremoting.client.transports.piped;

import org.codehaus.jremoting.api.ConnectionException;
import org.codehaus.jremoting.api.ThreadPool;
import org.codehaus.jremoting.client.ClientMonitor;
import org.codehaus.jremoting.client.ClientStreamReadWriter;
import org.codehaus.jremoting.client.ConnectionPinger;
import org.codehaus.jremoting.client.transports.ClientObjectStreamReadWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Class PipedObjectStreamInvocationHandler
 *
 * @author Paul Hammant
 * @version $Revision: 1.2 $
 */
public class PipedObjectStreamInvocationHandler extends AbstractPipedStreamInvocationHandler {

    /**
     * Constructor PipedObjectStreamInvocationHandler
     *
     * @param threadPool
     * @param clientMonitor
     * @param connectionPinger
     * @param is
     * @param os
     */
    public PipedObjectStreamInvocationHandler(ThreadPool threadPool, ClientMonitor clientMonitor, ConnectionPinger connectionPinger, PipedInputStream is, PipedOutputStream os, ClassLoader classLoader) {
        super(threadPool, clientMonitor, connectionPinger, is, os, classLoader);
    }

    public static class WithCurrentClassLoader extends PipedObjectStreamInvocationHandler {

        public WithCurrentClassLoader(ThreadPool threadPool, ClientMonitor clientMonitor, ConnectionPinger connectionPinger, PipedInputStream is, PipedOutputStream os) {
            super(threadPool, clientMonitor, connectionPinger, is, os, PipedObjectStreamInvocationHandler.class.getClassLoader());
        }

    }

    protected ClientStreamReadWriter createClientStreamReadWriter(InputStream in, OutputStream out) throws ConnectionException {
        return new ClientObjectStreamReadWriter(in, out);
    }
}
