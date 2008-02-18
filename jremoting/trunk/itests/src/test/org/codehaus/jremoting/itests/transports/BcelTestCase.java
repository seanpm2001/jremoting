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
package org.codehaus.jremoting.itests.transports;

import org.codehaus.jremoting.client.ContextFactory;
import org.codehaus.jremoting.client.SocketDetails;
import org.codehaus.jremoting.client.resolver.ServiceResolver;
import org.codehaus.jremoting.client.stubs.StubsFromServer;
import org.codehaus.jremoting.client.monitors.ConsoleClientMonitor;
import org.codehaus.jremoting.client.transports.SocketTransport;
import org.codehaus.jremoting.itests.TestFacade;
import org.codehaus.jremoting.itests.TestFacade2;
import org.codehaus.jremoting.itests.TestFacade3;
import org.codehaus.jremoting.itests.TestFacadeImpl;
import org.codehaus.jremoting.server.Publication;
import org.codehaus.jremoting.server.ServerMonitor;
import org.codehaus.jremoting.server.authenticators.NullAuthenticator;
import org.codehaus.jremoting.server.context.ThreadLocalServerContextFactory;
import org.codehaus.jremoting.server.stubretrievers.BcelDynamicStubRetriever;
import org.codehaus.jremoting.server.streams.ByteStreamConnectionFactory;
import org.codehaus.jremoting.server.transports.SocketServer;
import org.jmock.Mock;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.net.InetSocketAddress;

/**
 * Test case which tests the proxies generated using BCEL generator
 *
 * @author Vinay Chandrasekharan
 */
public class BcelTestCase extends AbstractHelloTestCase {

    /**
     * Fetch the directory to store the classes and java source generated by the dynamic class retrievers
     */
    private String getClassGenDir()
    {
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        return path;
    }

    protected void setUp() throws Exception {
        super.setUp();

        // server side setup.
        BcelDynamicStubRetriever stubRetriever = new BcelDynamicStubRetriever(this.getClass().getClassLoader());

        String class_gen_dir = getClassGenDir();
        stubRetriever.setClassGenDir(class_gen_dir);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
        server = new SocketServer((ServerMonitor) mockServerMonitor.proxy(), stubRetriever, new NullAuthenticator(), new ByteStreamConnectionFactory(), executorService, new ThreadLocalServerContextFactory(), new InetSocketAddress(10201));

        testServer = new TestFacadeImpl();
        Publication pd = new Publication(TestFacade.class).addAdditionalFacades(TestFacade3.class, TestFacade2.class);
        stubRetriever.generate("Hello223", pd, this.getClass().getClassLoader());
        server.publish(testServer, "Hello223", pd);
        server.start();

        // Client side setup
        Mock mock = mock(ContextFactory.class);
        mock.expects(atLeastOnce()).method("getClientContext").withNoArguments().will(returnValue(null));
        jremotingClient = new ServiceResolver(new SocketTransport(new ConsoleClientMonitor(),
                new org.codehaus.jremoting.client.streams.ByteStreamConnectionFactory(), new SocketDetails("127.0.0.1", 10201)), (ContextFactory) mock.proxy(), new StubsFromServer());
        testClient = (TestFacade) jremotingClient.serviceResolver("Hello223");

    }

    public void testHelloCall() throws Exception {
        super.testHelloCall();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        testClient = null;
        System.gc();
        Thread.sleep(300);
        jremotingClient.close();
        server.stop();
        super.tearDown();
    }


}
