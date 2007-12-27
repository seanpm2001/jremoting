/* ====================================================================
 * Copyright 2005-2006 JRemoting Committers
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
package org.codehaus.jremoting.client.transports.socket;

import org.codehaus.jremoting.BadConnectionException;
import org.codehaus.jremoting.ConnectionException;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

import org.codehaus.jremoting.client.ClientMonitor;
import org.codehaus.jremoting.client.ConnectionPinger;
import org.codehaus.jremoting.client.ConnectionRefusedException;
import org.codehaus.jremoting.client.pingers.NeverConnectionPinger;
import org.codehaus.jremoting.client.transports.StreamClientInvoker;
import org.codehaus.jremoting.client.transports.ClientStreamDriverFactory;

import java.io.IOException;
import java.net.Socket;

/**
 * Class SocketClientInvoker
 *
 * @author Paul Hammant
 * @version $Revision: 1.2 $
 */
public class SocketClientInvoker extends StreamClientInvoker {

    private final String host;
    private final int port;


    /**
     * SocketClientInvoker
     *
     * @param clientMonitor
     * @param executorService
     * @param connectionPinger
     * @param facadesClassLoader The class loader
     * @param host                  The host to connect to
     * @param port                  The port to conenct to
     */
    public SocketClientInvoker(ClientMonitor clientMonitor, ScheduledExecutorService executorService,
                                                 ConnectionPinger connectionPinger, ClassLoader facadesClassLoader,
                                                 ClientStreamDriverFactory streamDriverFactory,
                                                 String host, int port) throws ConnectionRefusedException, BadConnectionException {
        super(clientMonitor, executorService, connectionPinger, facadesClassLoader, streamDriverFactory);
        this.host = host;
        this.port = port;

        try {
            Socket socket = new Socket(this.host, this.port);
            socket.setSoTimeout(60 * 1000);
            setStreamDriver(streamDriverFactory.makeStreamDriver(socket.getInputStream(), socket.getOutputStream(), getFacadesClassLoader()));
        } catch (IOException ioe) {
            if (ioe.getMessage().startsWith("Connection refused")) {
                throw new ConnectionRefusedException("Connection to port " + port + " on host " + host + " refused.");
            }
            throw new BadConnectionException("Cannot open Stream(s) for socket: " + ioe.getMessage());
        }
    }


    public SocketClientInvoker(ClientMonitor clientMonitor, ClientStreamDriverFactory streamDriverFactory, String host, int port) throws ConnectionRefusedException, BadConnectionException {
        this(clientMonitor, Executors.newScheduledThreadPool(10), new NeverConnectionPinger(),
                Thread.currentThread().getContextClassLoader(), streamDriverFactory, host, port);
    }

    /**
     * Method tryReconnect
     *
     * @return connected or not.
     */
    protected boolean tryReconnect() {

        try {
            Socket socket = new Socket(host, port);
            socket.setSoTimeout(60 * 1000);
            setStreamDriver(streamDriverFactory.makeStreamDriver(socket.getInputStream(), socket.getOutputStream(), getFacadesClassLoader()));
            return true;
        } catch (ConnectionException ce) {
            // TODO log ?
            return false;
        } catch (IOException ce) {

            // TODO log ?
            return false;
        }
    }

}