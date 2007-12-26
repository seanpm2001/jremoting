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
package org.codehaus.jremoting.itests.mismatch;

import junit.framework.TestCase;

import org.codehaus.jremoting.BadConnectionException;
import org.codehaus.jremoting.client.factories.StubsOnClient;
import org.codehaus.jremoting.client.transports.rmi.RmiClientInvoker;
import org.codehaus.jremoting.client.transports.socket.SocketClientInvoker;
import org.codehaus.jremoting.client.transports.ClientByteStreamDriverFactory;
import org.codehaus.jremoting.client.transports.ClientObjectStreamDriverFactory;
import org.codehaus.jremoting.client.monitors.ConsoleClientMonitor;
import org.codehaus.jremoting.server.PublicationDescription;
import org.codehaus.jremoting.server.ServerMonitor;
import org.codehaus.jremoting.server.monitors.ConsoleServerMonitor;
import org.codehaus.jremoting.server.transports.rmi.RmiServer;
import org.codehaus.jremoting.server.transports.socket.SelfContainedSocketStreamServer;
import org.codehaus.jremoting.itests.TestInterface;
import org.codehaus.jremoting.itests.TestInterface2;
import org.codehaus.jremoting.itests.TestInterface3;
import org.codehaus.jremoting.itests.TestInterfaceImpl;

import java.io.IOException;

/**
 * Test Custom Stream over sockets.
 *
 * @author Paul Hammant
 */
public class SocketMismatchTestCase extends TestCase {
    private Class x_class;
    private String x_msg;
    private BadConnectionException x_bce;


    public void testByteStreamObjectStreamMismatchCanCauseTimeOut() throws Exception {

        ServerMonitor sm = new ServerMonitor() {
            public void closeError(Class clazz, String s, IOException e) {
                fail();
            }
            public void badConnection(Class clazz, String s, BadConnectionException bce) {
                fail();
            }
            public void classNotFound(Class clazz, ClassNotFoundException e) {
                fail();
            }
            public void unexpectedException(Class clazz, String s, Exception e) {
                fail();
            }
            public void stopServerError(Class clazz, String s, Exception e) {
                fail();
            }
        };

        SelfContainedSocketStreamServer server = new SelfContainedSocketStreamServer(sm, 12001);

        TestInterfaceImpl testServer = new TestInterfaceImpl();
        PublicationDescription pd = new PublicationDescription(TestInterface.class, new Class[]{TestInterface3.class, TestInterface2.class});
        server.publish(testServer, "Hello", pd);
        server.start();

        StubsOnClient factory = null;
        TestInterface testClient;
        try {

            // Client side setup
            factory = new StubsOnClient(new SocketClientInvoker(new ConsoleClientMonitor(),
                new ClientObjectStreamDriverFactory(), "127.0.0.1", 12001));
            testClient = (TestInterface) factory.lookupService("Hello");

            testClient.hello("hello");


            fail("ByteStreams and ObjectStreams cannot interoperate");
        } catch (BadConnectionException bce) {

            // expected

        } finally {

            testClient = null;
            System.gc();

            try {
                factory.close();
            } catch (Exception e) {
            }

            server.stop();


        }
    }

    public void dont_testObjectStreamByteStreamMismatch() throws Exception {

        // server side setup.
        SelfContainedSocketStreamServer server = new SelfContainedSocketStreamServer(new ConsoleServerMonitor(), 12002, SelfContainedSocketStreamServer.OBJECTSTREAM);
        TestInterfaceImpl testServer = new TestInterfaceImpl();
        PublicationDescription pd = new PublicationDescription(TestInterface.class, new Class[]{TestInterface3.class, TestInterface2.class});
        server.publish(testServer, "Hello", pd);
        server.start();

        StubsOnClient factory = null;
        try {

            // Client side setup
            factory = new StubsOnClient(new SocketClientInvoker(new ConsoleClientMonitor(),
                new ClientByteStreamDriverFactory(), "127.0.0.1", 12002));
            TestInterface testClient = (TestInterface) factory.lookupService("Hello");


            testClient.hello("hello");
            fail("ByteStreams and ObjectStreams cannot interoperate");
        } catch (BadConnectionException bce) {
            // expected.
        } finally {

            System.gc();


            try {
                factory.close();
            } catch (Exception e) {
            }

            server.stop();

        }
    }


    public void dont_testByteStreamRmiMismatch() throws Exception {

        // server side setup.
        SelfContainedSocketStreamServer server = new SelfContainedSocketStreamServer(new ConsoleServerMonitor(), 12003);
        TestInterfaceImpl testServer = new TestInterfaceImpl();
        PublicationDescription pd = new PublicationDescription(TestInterface.class, new Class[]{TestInterface3.class, TestInterface2.class});
        server.publish(testServer, "Hello", pd);
        server.start();

        StubsOnClient factory = null;
        try {

            // Client side setup
            factory = new StubsOnClient(new RmiClientInvoker(new ConsoleClientMonitor(), "127.0.0.1", 12003));
            TestInterface testClient = (TestInterface) factory.lookupService("Hello");


            testClient.hello("hello");
            fail("ByteStreams and RMI trasnports cannot interoperate");
        } catch (BadConnectionException bce) {
            // expected.
        } finally {

            System.gc();


            try {
                factory.close();
            } catch (Exception e) {
            }

            server.stop();

        }
    }

    public void dont_testRmiByteStreamMismatch() throws Exception {

        // server side setup.
        RmiServer server = new RmiServer(new ConsoleServerMonitor(), 12004);
        TestInterfaceImpl testServer = new TestInterfaceImpl();
        PublicationDescription pd = new PublicationDescription(TestInterface.class, new Class[]{TestInterface3.class, TestInterface2.class});
        server.publish(testServer, "Hello", pd);
        server.start();

        StubsOnClient factory = null;
        try {

            // Client side setup
            factory = new StubsOnClient(new SocketClientInvoker(new ConsoleClientMonitor(),
                new ClientObjectStreamDriverFactory(), "127.0.0.1", 12004));
            TestInterface testClient = (TestInterface) factory.lookupService("Hello");


            testClient.hello("hello");
            fail("ByteStreams and RMI trasnports cannot interoperate");
        } catch (BadConnectionException bce) {
            // expected.
        } finally {

            System.gc();


            try {
                factory.close();
            } catch (Exception e) {
            }

            server.stop();

        }
    }

}
