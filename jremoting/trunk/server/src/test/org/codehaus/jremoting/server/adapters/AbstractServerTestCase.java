package org.codehaus.jremoting.server.adapters;

import org.codehaus.jremoting.server.transports.AbstractServer;
import org.codehaus.jremoting.server.transports.DefaultServerSideClientContextFactory;
import org.codehaus.jremoting.server.ServerException;
import org.codehaus.jremoting.server.PublicationException;
import org.codehaus.jremoting.server.classretrievers.NoStubRetriever;
import org.codehaus.jremoting.server.monitors.ConsoleServerMonitor;
import org.codehaus.jremoting.server.authenticators.NullAuthenticator;
import org.codehaus.jremoting.requests.InvokeMethod;
import org.codehaus.jremoting.requests.OpenConnection;
import org.codehaus.jremoting.responses.ConnectionOpened;
import org.codehaus.jremoting.responses.AbstractResponse;
import org.codehaus.jremoting.responses.NotPublished;
import org.codehaus.jremoting.responses.ServicesSuspended;
import org.jmock.MockObjectTestCase;

import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

public class AbstractServerTestCase extends MockObjectTestCase {

    private InvocationHandlerAdapter iha;
    private AbstractServer server;
    HashMap impl = new HashMap();

    protected void setUp() throws Exception {

        iha = new InvocationHandlerAdapter(new NoStubRetriever(), new NullAuthenticator(), new ConsoleServerMonitor(), new DefaultServerSideClientContextFactory());
        server = new AbstractServer(iha, new ConsoleServerMonitor(), Executors.newCachedThreadPool()) {
            public void start() throws ServerException {
            }

            public void stop() {
            }
        };

    }

    public void testPublishAndUnpublish() throws PublicationException {
        server.publish(impl, "foo", Map.class);
        putTestEntry();
        server.unPublish(impl, "foo");
        AbstractResponse resp = putTestEntry();
        assertTrue(resp instanceof NotPublished);
    }

    public void testPublishAndUnpublishDoes() throws PublicationException {
        server.publish(impl, "foo", Map.class);
        server.unPublish(impl, "foo");
        try {
            server.unPublish(impl, "foo");
            fail("should have barfed");
        } catch (PublicationException e) {
        }
    }

    public void testCantUnpublishSomethingThatWasNeverPublished() throws PublicationException {
        try {
            server.unPublish(impl, "foo");
            fail("should have barfed");
        } catch (PublicationException e) {
        }
    }

    public void testCantPublishServiceTwice() throws PublicationException {
        server.publish(impl, "foo", Map.class);
        try {
            server.publish(impl, "foo", Map.class);
            fail("should have barfed");
        } catch (PublicationException e) {
            assertEquals("Service 'foo' already published",e.getMessage());
        }
    }

    public void testPublishAndRePublish() throws PublicationException {
        HashMap impl2 = new HashMap();
        server.publish(impl, "foo", Map.class);
        server.replacePublished(impl, "foo", impl2);
        putTestEntry();
        assertEquals("2", impl2.get("1"));
        assertNull(impl.get("1"));
    }

    private AbstractResponse putTestEntry() {
        ConnectionOpened co = (ConnectionOpened) iha.handleInvocation(new OpenConnection(), new Object());
        return iha.handleInvocation(new InvokeMethod("foo", "Main", "put(java.lang.Object, java.lang.Object)",new Object[] {"1","2"}, new Long(0), co.getSessionID()), new Object());
    }

    public void testPublishAndSuspendBlocksOperations() throws PublicationException {
        server.publish(impl, "foo", Map.class);
        server.suspend();
        assertTrue(iha.handleInvocation(new OpenConnection(), new Object()) instanceof ServicesSuspended);
    }

    public void testPublishAndSuspendAndResumeDoesNotBlockOperation() throws PublicationException {
        server.publish(impl, "foo", Map.class);
        server.suspend();
        server.resume();
        assertTrue(iha.handleInvocation(new OpenConnection(), new Object()) instanceof ConnectionOpened);
    }

}
