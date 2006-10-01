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
package org.codehaus.jremoting.test;

import org.codehaus.jremoting.client.Factory;
import org.codehaus.jremoting.client.ClientInvocationHandler;
import org.codehaus.jremoting.client.monitors.ConsoleClientMonitor;
import org.codehaus.jremoting.client.factories.ClientSideStubFactory;
import org.codehaus.jremoting.client.factories.ServerSideStubFactory;
import org.codehaus.jremoting.client.transports.socket.SocketStreamInvocationHandler;
import org.codehaus.jremoting.client.transports.ClientCustomStreamDriverFactory;
import org.codehaus.jremoting.client.transports.ClientObjectStreamDriverFactory;

/**
 * Class ProConClientTest
 *
 * @author Vinay Chandrasekharan
 */
public class ProConClientTest {

    /**
     * Method main
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        System.out.println("Stream over Socket Client");

        ClientInvocationHandler ih;

        if (args[1].equals("ObjectStream")) {
            System.out.println("(Object Stream)");

            ih = new SocketStreamInvocationHandler(new ConsoleClientMonitor(),
                new ClientObjectStreamDriverFactory(), "127.0.0.1", 1234);
        } else {
            System.out.println("(Custom Stream)");

            ih = new SocketStreamInvocationHandler(new ConsoleClientMonitor(),
                new ClientCustomStreamDriverFactory(), "127.0.0.1", 1235);
        }

        Factory af = null;

        if (args[0].equals("S")) {
            af = new ServerSideStubFactory(ih);
        } else {
            af = new ClientSideStubFactory(ih);
        }

        //list
        System.out.println("Listing services At Server...");

        String[] listOfPublishedObjectsOnServer = af.listServices();

        for (int i = 0; i < listOfPublishedObjectsOnServer.length; i++) {
            System.out.println("..[" + i + "]:" + listOfPublishedObjectsOnServer[i]);
        }

        TestProvider tpi = (TestProvider) af.lookupService("P");
        TestConsumer tci = (TestConsumer) af.lookupService("C");

        System.out.println("Provider.getName(0)" + tpi.getName(0));
        System.out.println("Consumer.getProviderName(0)" + tci.getProviderName(tpi));
        af.close();
    }
}
