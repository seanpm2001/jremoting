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
import org.codehaus.jremoting.authentications.Authentication;
import org.codehaus.jremoting.client.ContextFactory;
import org.codehaus.jremoting.client.Stub;
import org.codehaus.jremoting.client.ServiceResolver;
import org.codehaus.jremoting.client.StubHelper;
import org.codehaus.jremoting.client.StubRegistry;
import org.codehaus.jremoting.client.Transport;
import org.codehaus.jremoting.client.context.ThreadLocalContextFactory;
import org.codehaus.jremoting.client.stubs.StubClassLoader;
import org.codehaus.jremoting.client.stubs.StubsOnClient;
import org.codehaus.jremoting.client.stubs.StubsViaReflection;
import org.codehaus.jremoting.requests.ListServices;
import org.codehaus.jremoting.requests.LookupService;
import org.codehaus.jremoting.responses.ConnectionOpened;
import org.codehaus.jremoting.responses.ExceptionThrown;
import org.codehaus.jremoting.responses.NotPublished;
import org.codehaus.jremoting.responses.Response;
import org.codehaus.jremoting.responses.Service;
import org.codehaus.jremoting.responses.ServicesList;
import org.codehaus.jremoting.util.FacadeRefHolder;

import java.lang.ref.WeakReference;
import java.util.HashMap;


/**
 * Class JRemotingServiceResolver
 *
 * @author Paul Hammant
 * @author Peter Royal <a href="mailto:proyal@managingpartners.com">proyal@managingpartners.com</a>
 * @author Mauro Talevi
 */
public class JRemotingServiceResolver implements ServiceResolver {

    private static final int STUB_PREFIX_LENGTH = org.codehaus.jremoting.util.StubHelper.getStubPrefixLength();
    protected final Transport transport;
    private final ContextFactory contextFactory;
    private final StubClassLoader stubClassLoader;
    protected final HashMap<Long,WeakReference<Object>> refObjs = new HashMap<Long, WeakReference<Object>>();
    private transient String textToSign;
    protected final long session;
    private StubRegistry stubRegistry;

    public JRemotingServiceResolver(Transport transport) throws ConnectionException {
        this(transport, new ThreadLocalContextFactory());
    }

    public JRemotingServiceResolver(final Transport transport, ContextFactory contextFactory) throws ConnectionException {
        this (transport, contextFactory, JRemotingServiceResolver.class.getClassLoader());
    }
    
    public JRemotingServiceResolver(final Transport transport, ContextFactory contextFactory, ClassLoader classLoader) throws ConnectionException {
        this (transport, contextFactory, new StubsOnClient(classLoader));
    }

    public JRemotingServiceResolver(final Transport transport, ContextFactory contextFactory, StubClassLoader stubClassLoader) throws ConnectionException {
        this.transport = transport;
        this.contextFactory = contextFactory;
        this.stubClassLoader = stubClassLoader;

        ConnectionOpened response = transport.openConnection();

        textToSign = response.getTextToSign();
        session = response.getSessionID();

        stubRegistry = new StubRegistry() {

            public void registerReferenceObject(Object instance, Long reference) {
                synchronized (this) {
                    refObjs.put(reference, new WeakReference<Object>(instance));
                }
            }

            public Object getInstance(Long reference) {
                WeakReference<Object> wr;
                synchronized (this) {
                    wr = refObjs.get(reference);
                }
                if (wr == null) {
                    return null;
                }
                Object obj = wr.get();

                if (obj == null) {
                    refObjs.remove(reference);
                }
                return obj;
            }

            public Object getInstance(String facadeClassName, String publishedServiceName, String objectName, StubHelper stubHelper) throws ConnectionException {
                return JRemotingServiceResolver.this.getInstance(facadeClassName, publishedServiceName, objectName, stubHelper);
            }

            public void marshallCorrection(String remoteObjectName, String methodSignature, Object[] args, Class[] argClasses) {
                for (int i = 0; i < argClasses.length; i++) {
                    Class argClass = argClasses[i];
                    if (argClass == null) {
                        continue;
                    }
                    //All remote references implement Stub interface
                    if (args[i] instanceof Stub) {
                        Stub stub = (Stub) args[i];

                        if (getReferenceID(stub) != null) {
                            //The stripping STUB_PREFIX from the stub names generated by StubGenerator
                            String objName;

                            if (args[i] instanceof StubsViaReflection.Stub2) {
                                objName = ((StubsViaReflection.Stub2) args[i]).jRemotingGetObjectName();
                            } else {
                                objName = args[i].getClass().getName().substring(STUB_PREFIX_LENGTH);
                            }

                            args[i] = makeFacadeRefHolder(stub, objName);
                        }
                    } else // Let the specific Invokers be given the last chance to modify the arguments.
                    {
                        args[i] = transport.resolveArgument(remoteObjectName, methodSignature, argClasses[i], args[i]);
                    }
                }
            }
        };
    }

    public Object lookupService(String publishedServiceName, Authentication authentication) throws ConnectionException {

        Response response = transport.invoke(new LookupService(publishedServiceName, authentication, session), true);

        if (response instanceof NotPublished) {
            throw new ConnectionException("Service '" + publishedServiceName + "' not published");
        } else if (response instanceof ExceptionThrown) {
            ExceptionThrown er = (ExceptionThrown) response;
            Throwable t = er.getResponseException();

            if (t instanceof ConnectionException) {
                throw (ConnectionException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new ConnectionException("Problem doing lookup on service [exception: " + t.getMessage() + "]");
            }
        }

        Service service = (Service) response;
        DefaultStubHelper stubHelper = new DefaultStubHelper(stubRegistry, transport, contextFactory, publishedServiceName, "Main", service.getReference(), session, service.getFacadeName(), service.getAdditionalFacadeNames());
        Object retVal = getInstance(service.getFacadeName(), publishedServiceName, "Main", stubHelper);

        stubHelper.registerInstance(retVal);

        return retVal;
    }

    public final Long getReferenceID(Stub obj) {
        return obj.jRemotingGetReferenceID(this);
    }

    public final Object lookupService(String publishedServiceName) throws ConnectionException {
        return lookupService(publishedServiceName, null);
    }

    public String getTextToSignForAuthentication() {
        return textToSign;
    }

    public String[] listServices() {
        Response ar = transport.invoke(new ListServices(), true);
        return ((ServicesList) ar).getServices();
    }

    public boolean hasService(String publishedServiceName) {
        final String[] services = listServices();
        for (String service : services) {
            if (service.equals(publishedServiceName)) {
                return true;
            }
        }
        return false;
    }

    private FacadeRefHolder makeFacadeRefHolder(Stub obj, String objectName) {
        Long refID = getReferenceID(obj);
        return new FacadeRefHolder(refID, objectName);
    }


    protected Object getInstance(String facadeClassName, String publishedServiceName, String objectName, StubHelper stubHelper) throws ConnectionException {
            return stubClassLoader.instantiateStub(facadeClassName, publishedServiceName, objectName, transport, stubHelper);
    }

    public void close() {
        transport.closeConnection(session);
    }



}