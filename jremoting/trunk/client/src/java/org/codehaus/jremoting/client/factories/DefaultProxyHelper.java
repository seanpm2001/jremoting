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
package org.codehaus.jremoting.client.factories;

import org.codehaus.jremoting.client.ClientContextFactory;
import org.codehaus.jremoting.client.ClientInvocationHandler;
import org.codehaus.jremoting.client.InvocationException;
import org.codehaus.jremoting.client.NoSuchReferenceException;
import org.codehaus.jremoting.client.NoSuchSessionException;
import org.codehaus.jremoting.client.Proxy;
import org.codehaus.jremoting.client.ProxyHelper;
import org.codehaus.jremoting.commands.ExceptionResponse;
import org.codehaus.jremoting.commands.GarbageCollectionRequest;
import org.codehaus.jremoting.commands.GarbageCollectionResponse;
import org.codehaus.jremoting.commands.GroupedMethodRequest;
import org.codehaus.jremoting.commands.InvocationExceptionResponse;
import org.codehaus.jremoting.commands.MethodAsyncRequest;
import org.codehaus.jremoting.commands.MethodFacadeArrayResponse;
import org.codehaus.jremoting.commands.MethodFacadeRequest;
import org.codehaus.jremoting.commands.MethodFacadeResponse;
import org.codehaus.jremoting.commands.MethodRequest;
import org.codehaus.jremoting.commands.MethodResponse;
import org.codehaus.jremoting.commands.NoSuchReferenceResponse;
import org.codehaus.jremoting.commands.NoSuchSessionResponse;
import org.codehaus.jremoting.commands.PublishedNameRequest;
import org.codehaus.jremoting.commands.ReplyConstants;
import org.codehaus.jremoting.commands.Response;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Class DefaultProxyHelper
 *
 * @author Paul Hammant
 * @author Vinay Chandrasekharan <a href="mailto:vinayc77@yahoo.com">vinayc77@yahoo.com</a>
 * @version $Revision: 1.3 $
 */
public final class DefaultProxyHelper implements ProxyHelper {

    private final transient AbstractFactory factory;
    private final transient ClientInvocationHandler clientInvocationHandler;
    private final transient String publishedServiceName;
    private final transient String objectName;
    private final transient Long referenceID;
    private final transient Long session;
    private ClientContextFactory clientContextClientFactory;
    private ArrayList queuedAsyncRequests = new ArrayList();

    /**
     * Constructor DefaultProxyHelper
     *
     * @param abstractFactory
     * @param clientInvocationHandler
     * @param pubishedServiceName
     * @param objectName
     * @param referenceID
     * @param session
     */
    public DefaultProxyHelper(AbstractFactory abstractFactory, ClientInvocationHandler clientInvocationHandler, String pubishedServiceName, String objectName, Long referenceID, Long session) {

        factory = abstractFactory;
        this.clientInvocationHandler = clientInvocationHandler;
        publishedServiceName = pubishedServiceName;
        this.objectName = objectName;
        this.referenceID = referenceID;
        this.session = session;
        if (abstractFactory == null) {
            throw new IllegalArgumentException("abstractFactory cannot be null");
        }
        if (clientInvocationHandler == null) {
            throw new IllegalArgumentException("clientInvocationHandler cannot be null");
        }

    }

    /**
     * Method registerImplObject
     *
     * @param implBean
     */
    public void registerImplObject(Object implBean) {
        factory.registerReferenceObject(implBean, referenceID);
    }

    /**
     * Method processObjectRequestGettingFacade
     *
     * @param returnClassType
     * @param methodSignature
     * @param args
     * @param objectName
     * @return
     * @throws Throwable
     */

    public Object processObjectRequestGettingFacade(Class returnClassType, String methodSignature, Object[] args, String objectName) throws Throwable {
        try {
            return processObjectRequestGettingFacade2(returnClassType, methodSignature, args, objectName);
        } catch (InvocationException ie) {
            clientInvocationHandler.getClientMonitor().invocationFailure(this.getClass(), this.getClass().getName(), ie);
            throw ie;
        }
    }

    private Object processObjectRequestGettingFacade2(Class returnClassType, String methodSignature, Object[] args, String objectName) throws Throwable {

        boolean arrayRetVal = objectName.endsWith("[]");
        String objNameWithoutArray = objectName.substring(0, objectName.length() - 2);
        MethodFacadeRequest request;

        if (arrayRetVal) {
            request = new MethodFacadeRequest(publishedServiceName, this.objectName, methodSignature, args, referenceID, objNameWithoutArray, session);
        } else {
            request = new MethodFacadeRequest(publishedServiceName, this.objectName, methodSignature, args, referenceID, objectName, session);
        }

        setContext(request);
        Response response = clientInvocationHandler.handleInvocation(request);

        if (response.getReplyCode() == ReplyConstants.METHODFACADEREPLY) {
            MethodFacadeResponse mfr = (MethodFacadeResponse) response;
            Long ref = mfr.getReferenceID();

            // it might be that the return value was intended to be null.
            if (ref == null) {
                return null;
            }

            Object implBean = factory.getImplObj(ref);

            if (implBean == null) {
                DefaultProxyHelper pHelper = new DefaultProxyHelper(factory, clientInvocationHandler, publishedServiceName, mfr.getObjectName(), ref, session);
                Object retFacade = factory.getInstance(publishedServiceName, mfr.getObjectName(), pHelper);

                pHelper.registerImplObject(retFacade);

                return retFacade;
            } else {
                return implBean;
            }
        } else if (response.getReplyCode() == ReplyConstants.METHODFACADEARRAYREPLY) {
            MethodFacadeArrayResponse mfar = (MethodFacadeArrayResponse) response;
            Long[] refs = mfar.getReferenceIDs();
            String[] objectNames = mfar.getObjectNames();
            Object[] implBeans = (Object[]) Array.newInstance(returnClassType, refs.length);

            for (int i = 0; i < refs.length; i++) {
                Long ref = refs[i];

                if (ref == null) {
                    implBeans[i] = null;
                } else {
                    Object o = factory.getImplObj(ref);

                    implBeans[i] = o;

                    if (implBeans[i] == null) {
                        DefaultProxyHelper bo2 = new DefaultProxyHelper(factory, clientInvocationHandler, publishedServiceName, objectNames[i], refs[i], session);
                        Object retFacade = null;

                        try {
                            retFacade = factory.getInstance(publishedServiceName, objectNames[i], bo2);
                        } catch (Exception e) {
                            System.out.println("objNameWithoutArray=" + returnClassType.getName());
                            System.out.flush();
                            e.printStackTrace();
                        }

                        bo2.registerImplObject(retFacade);

                        implBeans[i] = retFacade;
                    }
                }
            }

            return implBeans;
        } else {
            throw makeUnexpectedReplyThrowable(response);
        }
    }

    /**
     * Method processObjectRequest
     *
     * @param methodSignature
     * @param args
     * @return
     * @throws Throwable
     */
    public Object processObjectRequest(String methodSignature, Object[] args, Class[] argClasses) throws Throwable {

        try {
            factory.marshallCorrection(publishedServiceName, methodSignature, args, argClasses);

            MethodRequest request = new MethodRequest(publishedServiceName, objectName, methodSignature, args, referenceID, session);
            setContext(request);
            Response response = clientInvocationHandler.handleInvocation(request);

            if (response.getReplyCode() == ReplyConstants.METHODREPLY) {
                MethodResponse or = (MethodResponse) response;

                return or.getReplyObject();
            } else {
                throw makeUnexpectedReplyThrowable(response);
            }
        } catch (InvocationException ie) {
            clientInvocationHandler.getClientMonitor().invocationFailure(this.getClass(), this.getClass().getName(), ie);
            throw ie;
        }
    }


    /**
     * Method processVoidRequest
     *
     * @param methodSignature
     * @param args
     * @throws Throwable
     */
    public void processVoidRequest(String methodSignature, Object[] args, Class[] argClasses) throws Throwable {

        try {
            factory.marshallCorrection(publishedServiceName, methodSignature, args, argClasses);

            MethodRequest request = new MethodRequest(publishedServiceName, objectName, methodSignature, args, referenceID, session);

            //debug(args);
            setContext(request);
            Response response = clientInvocationHandler.handleInvocation(request);

            if (response.getReplyCode() == ReplyConstants.METHODREPLY) {
                MethodResponse or = (MethodResponse) response;

                return;
            } else {
                throw makeUnexpectedReplyThrowable(response);
            }
        } catch (InvocationException ie) {
            clientInvocationHandler.getClientMonitor().invocationFailure(this.getClass(), this.getClass().getName(), ie);
            throw ie;
        }
    }

    public void queueAsyncRequest(String methodSignature, Object[] args, Class[] argClasses) {

        synchronized (queuedAsyncRequests) {

            GroupedMethodRequest request = new GroupedMethodRequest(methodSignature, args);
            queuedAsyncRequests.add(request);
        }

    }

    public void commitAsyncRequests() throws Throwable {

        synchronized (queuedAsyncRequests) {

            try {
                GroupedMethodRequest[] rawRequests = new GroupedMethodRequest[queuedAsyncRequests.size()];
                queuedAsyncRequests.toArray(rawRequests);
                MethodAsyncRequest request = new MethodAsyncRequest(publishedServiceName, objectName, rawRequests, referenceID, session);

                //debug(args);
                setContext(request);
                Response response = clientInvocationHandler.handleInvocation(request);

                if (response.getReplyCode() == ReplyConstants.METHODREPLY) {
                    MethodResponse or = (MethodResponse) response;
                    return;
                } else {
                    throw makeUnexpectedReplyThrowable(response);
                }
            } catch (InvocationException ie) {
                clientInvocationHandler.getClientMonitor().invocationFailure(this.getClass(), this.getClass().getName(), ie);
                throw ie;
            }
        }
    }

    public void rollbackAsyncRequests() {
        synchronized (queuedAsyncRequests) {
            queuedAsyncRequests.clear();
        }
    }


    /**
     * Method processVoidRequestWithRedirect
     *
     * @param methodSignature
     * @param args
     * @throws Throwable
     */
    public void processVoidRequestWithRedirect(String methodSignature, Object[] args, Class[] argClasses) throws Throwable {

        Object[] newArgs = new Object[args.length];

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Proxy) {

                //TODO somehow get the reference details and put a redirect place holder here
            } else {
                newArgs[i] = args[i];
            }
        }

        processVoidRequest(methodSignature, newArgs, argClasses);
    }


    /**
     * @param response The responsense that represents the unexpected responsense.
     * @return The exception that is pertient.
     */
    private Throwable makeUnexpectedReplyThrowable(Response response) {

        if (response.getReplyCode() == ReplyConstants.EXCEPTIONREPLY) {
            ExceptionResponse er = (ExceptionResponse) response;
            return er.getReplyException();
        } else if (response.getReplyCode() == ReplyConstants.NOSUCHSESSIONREPLY) {
            NoSuchSessionResponse nssr = (NoSuchSessionResponse) response;
            return new NoSuchSessionException(nssr.getSessionID());
        }
        //TODO remove some of these if clover indicates they are not used?
        else if (response.getReplyCode() == ReplyConstants.NOSUCHREFERENCEREPLY) {
            NoSuchReferenceResponse nsrr = (NoSuchReferenceResponse) response;
            return new NoSuchReferenceException(nsrr.getReferenceID());
        } else if (response.getReplyCode() == ReplyConstants.INVOCATIONEXCEPTIONREPLY) {
            InvocationExceptionResponse ier = (InvocationExceptionResponse) response;
            return new InvocationException(ier.getMessage());
        } else {
            return new InvocationException("Internal Error : Unknown response type :" + response.getClass().getName());
        }
    }

    /**
     * Method getReferenceID
     *
     * @param factory
     * @return
     */
    public Long getReferenceID(Object factory) {

        // this checks the factory because reference IDs should not be
        // given out to any requester.  It should be privileged information.
        if (factory == this.factory) {
            return referenceID;
        } else {
            return null;
        }
    }

    public boolean isEquals(Object o1, Object o2) {
        if (o2 == null) {
            return false;
        }
        if (o1 == o2) {
            return true;
        }
        if (o1.getClass() != o2.getClass()) {
            return false;
        }
        try {
            Object retVal = processObjectRequest("equals(java.lang.Object)", new Object[]{o2}, new Class[]{Object.class});
            return ((Boolean) retVal).booleanValue();
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                t.printStackTrace();
                throw new org.codehaus.jremoting.client.InvocationException("Should never get here: " + t.getMessage());
            }
        }


    }

    protected void finalize() throws Throwable {

        synchronized (factory) {
            Response response = clientInvocationHandler.handleInvocation(new GarbageCollectionRequest(publishedServiceName, objectName, session, referenceID));
            if (response instanceof ExceptionResponse) {
                // This happens if the object can not be GCed on the remote server
                //  for any reason.
                // There is nothing that we can do about it from here, so just let
                //  this fall through.
                // One case where this can happen is if the server is restarted quickly.
                //  An object created in one ivocation will try to be gced in the second
                //  invocation.  As the object does not exist, an error is thrown.
                /*
                System.out.println("----> Got an ExceptionResponsensense in response to a GarbageCollectionRequest" );
                ExceptionResponsensense er = (ExceptionResponsensense)response;
                er.getReplyException().printStackTrace();
                */
            } else if (!(response instanceof GarbageCollectionResponse)) {
                System.err.println("----> Some problem during DGC! Make sure factory is closed. ");
            }
        }
        super.finalize();
    }

    public void setClientContextClientFactory(ClientContextFactory clientContextClientFactory) {
        this.clientContextClientFactory = clientContextClientFactory;
    }

    private synchronized void setContext(PublishedNameRequest request) {

        if (clientContextClientFactory == null) {
            clientContextClientFactory = new DefaultClientContextFactory();
        }
        request.setContext(clientContextClientFactory.getClientContext());

    }


}
