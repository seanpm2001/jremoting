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

import org.codehaus.jremoting.client.ClientMonitor;
import org.codehaus.jremoting.client.factories.JRemotingServiceResolver;
import org.codehaus.jremoting.client.transports.direct.DirectTransport;
import org.codehaus.jremoting.itests.TestFacade;
import org.codehaus.jremoting.itests.TestFacade2;
import org.codehaus.jremoting.itests.TestInterface3;
import org.codehaus.jremoting.itests.TestInterfaceImpl;
import org.codehaus.jremoting.server.Publication;
import org.codehaus.jremoting.server.ServerMonitor;
import org.codehaus.jremoting.server.transports.direct.DirectServer;


/**
 * Test Direct Marshalled Transport
 *
 * @author Paul Hammant
 */
public class DirectUnMarshalledTestCase extends AbstractHelloTestCase {

    protected void setUp() throws Exception {
        super.setUp();

        // server side setup.
        server = new DirectServer((ServerMonitor) mockServerMonitor.proxy());
        testServer = new TestInterfaceImpl();
        Publication pd = new Publication(TestFacade.class).addAdditionalFacades(TestInterface3.class, TestFacade2.class);
        server.publish(testServer, "Hello", pd);
        server.start();

        // Client side setup
        mockClientMonitor.expects(atLeastOnce()).method("methodLogging").will(returnValue(false));
        serviceResolver = new JRemotingServiceResolver(new DirectTransport((ClientMonitor) mockClientMonitor.proxy(), server));

        testClient = (TestFacade) serviceResolver.lookupService("Hello");

    }

    protected void tearDown() throws Exception {
        testClient = null;
        System.gc();
        serviceResolver.close();
        server.stop();
    }

}
