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
package org.codehaus.jremoting.client.transports.rmi;

import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

import org.codehaus.jremoting.BadConnectionException;
import org.codehaus.jremoting.ConnectionException;
import org.codehaus.jremoting.RmiInvoker;
import org.codehaus.jremoting.client.ClientMonitor;
import org.codehaus.jremoting.client.ConnectionPinger;
import org.codehaus.jremoting.client.InvocationException;
import org.codehaus.jremoting.client.NoSuchReferenceException;
import org.codehaus.jremoting.client.NotPublishedException;
import org.codehaus.jremoting.client.pingers.NeverConnectionPinger;
import org.codehaus.jremoting.client.transports.StatefulClientInvoker;
import org.codehaus.jremoting.requests.Request;
import org.codehaus.jremoting.requests.Servicable;
import org.codehaus.jremoting.requests.InvokeMethod;
import org.codehaus.jremoting.requests.RequestConstants;
import org.codehaus.jremoting.responses.Response;
import org.codehaus.jremoting.responses.NoSuchReference;
import org.codehaus.jremoting.responses.NotPublished;
import org.codehaus.jremoting.responses.ProblemResponse;
import org.codehaus.jremoting.responses.TryLater;

/**
 * Class RmiClientInvoker
 *
 * @author Paul Hammant
 * @version $Revision: 1.2 $
 */
public final class RmiClientInvoker extends StatefulClientInvoker {

    private RmiInvoker rmiInvoker;
    private String url;
    private long lastRealRequest = System.currentTimeMillis();

    public RmiClientInvoker(ClientMonitor clientMonitor, ScheduledExecutorService executorService, ConnectionPinger connectionPinger, String host, int port) throws ConnectionException {

        super(clientMonitor, executorService, connectionPinger, RmiClientInvoker.class.getClassLoader());

        url = "rmi://" + host + ":" + port + "/" + RmiInvoker.class.getName();

        try {
            rmiInvoker = (RmiInvoker) Naming.lookup(url);
        } catch (NotBoundException nbe) {
            throw new ConnectionException("Cannot bind to the remote RMI service.  Either an IP or RMI issue.");
        } catch (MalformedURLException mfue) {
            throw new ConnectionException("Malformed URL, host/port (" + host + "/" + port + ") must be wrong: " + mfue.getMessage());
        } catch (ConnectIOException cioe) {
            throw new BadConnectionException("Cannot connect to remote RMI server. " + "It is possible that transport mismatch");
        } catch (RemoteException re) {
            throw new ConnectionException("Unknown Remote Exception : " + re.getMessage());
        }
    }

    public RmiClientInvoker(ClientMonitor clientMonitor, String host, int port) throws ConnectionException {
        this(clientMonitor, Executors.newScheduledThreadPool(10), new NeverConnectionPinger(), host, port);

    }


    /**
     * Method tryReconnect
     *
     * @return
     */
    protected boolean tryReconnect() {

        try {
            rmiInvoker = (RmiInvoker) Naming.lookup(url);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Method invoke
     *
     * @param request
     * @return
     */
    public synchronized Response invoke(Request request) {

        if (request.getRequestCode() != RequestConstants.PINGREQUEST) {
            lastRealRequest = System.currentTimeMillis();
        }

        boolean again = true;
        Response response = null;
        int tries = 0;
        long start = 0;

        if (methodLogging) {
            start = System.currentTimeMillis();
        }

        while (again) {
            tries++;

            again = false;

            try {
                response = rmiInvoker.invoke(request);

                if (response instanceof ProblemResponse) {
                    if (response instanceof TryLater) {
                        int millis = ((TryLater) response).getSuggestedDelayMillis();

                        clientMonitor.serviceSuspended(this.getClass(), request, tries, millis);

                        again = true;
                    } else if (response instanceof NoSuchReference) {
                        throw new NoSuchReferenceException(((NoSuchReference) response).getReferenceID());
                    } else if (response instanceof NotPublished) {
                        Servicable pnr = (Servicable) request;

                        throw new NotPublishedException(pnr.getService(), pnr.getObjectName());
                    }
                }
            } catch (RemoteException re) {
                if (re instanceof ConnectException | re instanceof ConnectIOException) {
                    int retryConnectTries = 0;

                    rmiInvoker = null;

                    while (!tryReconnect()) {
                        clientMonitor.serviceAbend(this.getClass(), retryConnectTries, re);

                        retryConnectTries++;
                    }
                } else {
                    throw new InvocationException("Unknown RMI problem : " + re.getMessage(), re);
                }
            }
        }

        if (methodLogging) {
            if (request instanceof InvokeMethod) {
                clientMonitor.methodCalled(this.getClass(), ((InvokeMethod) request).getMethodSignature(), System.currentTimeMillis() - start, "");
            }
        }

        return response;
    }

    /**
     * Method getLastRealRequestTime
     *
     * @return
     */
    public long getLastRealRequestTime() {
        return lastRealRequest;
    }
}