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
package org.codehaus.jremoting.server.transports;

import org.codehaus.jremoting.api.BadConnectionException;
import org.codehaus.jremoting.api.ConnectionException;
import org.codehaus.jremoting.api.SerializationHelper;
import org.codehaus.jremoting.api.ThreadPool;
import org.codehaus.jremoting.requests.AbstractRequest;
import org.codehaus.jremoting.responses.AbstractResponse;
import org.codehaus.jremoting.server.ServerMonitor;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Class ServerCustomStreamDriver
 *
 * @author Paul Hammant
 * @version $Revision: 1.2 $
 */
public class ServerCustomStreamDriver extends AbstractServerStreamDriver {

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public ServerCustomStreamDriver(ServerMonitor serverMonitor, ThreadPool threadPool) {
        super(serverMonitor, threadPool);
    }

    /**
     * Initialize
     *
     * @throws IOException if an IO Excpetion
     */
    protected void initialize() throws IOException {
        dataInputStream = new DataInputStream(getInputStream());
        dataOutputStream = new DataOutputStream(new BufferedOutputStream(getOutputStream()));
    }

    /**
     * Write a response, and wait for a request
     *
     * @param response The response to send
     * @return The new request
     * @throws IOException            In an IO Exception
     * @throws ConnectionException    In an IO Exception
     * @throws ClassNotFoundException If a class not found during deserialization.
     */
    protected synchronized AbstractRequest writeReplyAndGetRequest(AbstractResponse response) throws IOException, ClassNotFoundException, ConnectionException {

        if (response != null) {
            writeReply(response);
        }

        return readRequest();
    }

    private void writeReply(AbstractResponse response) throws IOException {

        byte[] aBytes = SerializationHelper.getBytesFromInstance(response);

        dataOutputStream.writeInt(aBytes.length);
        dataOutputStream.write(aBytes);
        dataOutputStream.flush();
    }

    protected void close() {
        try {
            dataInputStream.close();
        } catch (IOException e) {
        }
        try {
            dataOutputStream.close();
        } catch (IOException e) {
        }
        super.close();
    }

    private AbstractRequest readRequest() throws IOException, ClassNotFoundException, ConnectionException {
        int byteArraySize = dataInputStream.readInt();
        if (byteArraySize < 0) {
            throw new BadConnectionException("Transport mismatch, Unable to " + "read packet of data from CustomStream.");
        }
        byte[] byteArray = new byte[byteArraySize];
        int pos = 0;
        int cnt = 0;

        // Loop here until the entire array has been read in.
        while (pos < byteArraySize) {
            //TODO cater for DOS attack here.
            int read = dataInputStream.read(byteArray, pos, byteArraySize - pos);

            pos += read;

            cnt++;
        }

        /*
        if (cnt > 1)
        {
            System.out.println( "ServerCustomStreamDriver.readReply took " + cnt +
                " reads to read all, " + byteArraySize + ", required bytes." );
        }
        */
        return (AbstractRequest) SerializationHelper.getInstanceFromBytes(byteArray);
    }
}