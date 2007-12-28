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
package org.codehaus.jremoting.client.factories;

import org.codehaus.jremoting.ConnectionException;
import org.codehaus.jremoting.client.Transport;
import org.codehaus.jremoting.client.StubHelper;
import org.codehaus.jremoting.client.ContextFactory;

/**
 * DynamicStubs creates stubs(@see DynamicStub) for the given
 * publishedName at the time of invocation.Using this factory removes the need
 * for any compiled stubs corresponding to the remote interface
 * to be present on the client side to invoke any remote method on the server.
 *
 * @author <a href="mailto:vinayc@apache.org">Vinay Chandran</a>
 */
public class DynamicStubs extends JRemotingServiceResolver {

    public DynamicStubs(Transport transport, ContextFactory contextFactory) throws ConnectionException {
        super(transport, contextFactory);
    }

    protected Class getStubClass(String publishedServiceName, String objectName) throws ConnectionException, ClassNotFoundException {
        //NOT USED
        return null;
    }

    protected Object getInstance(String publishedServiceName, String objectName, StubHelper stubHelper) throws ConnectionException {
        return new DynamicStub(publishedServiceName, objectName, stubHelper);
    }

}
