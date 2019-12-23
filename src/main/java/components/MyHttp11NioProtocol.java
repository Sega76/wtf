package components;

import com.sun.org.apache.xalan.internal.res.XSLTErrorResources;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.ObjectName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;
import org.apache.coyote.ContainerThreadMarker;
import org.apache.coyote.Processor;
import org.apache.coyote.ProtocolException;
import org.apache.coyote.Request;
import org.apache.coyote.RequestGroupInfo;
import org.apache.coyote.RequestInfo;
import org.apache.coyote.UpgradeProtocol;
import org.apache.coyote.UpgradeToken;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.coyote.http11.upgrade.InternalHttpUpgradeHandler;
import org.apache.coyote.http11.upgrade.UpgradeProcessorInternal;
import org.apache.juli.logging.Log;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.collections.SynchronizedStack;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.AbstractJsseEndpoint;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SecureNioChannel;
import org.apache.tomcat.util.net.SocketEvent;
import org.apache.tomcat.util.net.SocketWrapperBase;
import org.apache.tomcat.util.res.StringManager;
import other.MyNioEndpoint;

public class MyHttp11NioProtocol extends Http11NioProtocol {
    /**
     * The string manager for this package.
     */
    private static final StringManager sm = StringManager.getManager(MyHttp11NioProtocol.class);

    private AbstractEndpoint<NioChannel, ?> endpoint;

    public MyHttp11NioProtocol() {
        super();
        this.endpoint = new MyNioEndpoint();
        this.setHandler(new MyHandler(this));
    }

    @Override
    protected AbstractJsseEndpoint<NioChannel, ?> getEndpoint() {
        // Over-ridden to add cast
        return super.getEndpoint();
    }

    protected static class MyHandler<S> implements AbstractEndpoint.Handler<S> {

        private final MyHttp11NioProtocol proto;
        private final RequestGroupInfo global = new RequestGroupInfo();
        private final AtomicLong registerCount = new AtomicLong(0);
        private final RecycledProcessors recycledProcessors = new RecycledProcessors(this);

        public MyHandler(MyHttp11NioProtocol proto) {
            this.proto = proto;
        }


        protected MyHttp11NioProtocol getProtocol() {
            return proto;
        }

        protected Log getLog() {
            return getProtocol().getLog();
        }

        @Override
        public Object getGlobal() {
            return global;
        }

        @Override
        public void recycle() {
            recycledProcessors.clear();
        }

        @Override
        public SocketState process(SocketWrapperBase<S> wrapper, SocketEvent status) {

            System.err.println(status);

//            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
//            Arrays.asList(trace).forEach(System.err::println);


            if (getLog().isDebugEnabled()) {
                getLog().debug(sm.getString("abstractConnectionHandler.process",
                    wrapper.getSocket(), status));
            }
            if (wrapper == null) {
                // Nothing to do. Socket has been closed.
                return SocketState.CLOSED;
            }

            S socket = wrapper.getSocket();


//            try {
//            SecureNioChannel channel = (SecureNioChannel) socket;
//            X509Certificate[] chain = new X509Certificate[0];
//
//                SSLParameters parameters = new SSLParameters(null);
//                parameters.setNeedClientAuth(false);
//
//            channel.getSslEngine().setSSLParameters(parameters);
////            channel.handshake(true,true);
//
//                ByteBuffer buf = channel.getEmptyBuf();
//                int read = channel.read(buf);
//                System.out.println(read);
//                System.out.println(buf.toString());
//            channel.rehandshake(100L);
////                chain=channel.getSslEngine().getSession().getPeerCertificateChain();
////                System.err.println("channel.getSslEngine().getSession().getPeerCertificateChain");
////                Arrays.asList(chain).forEach(s-> System.err.println(s.getSubjectDN()));
//
//            }
//            catch (SSLPeerUnverifiedException e) {
//                e.printStackTrace();
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
            Processor processor = (Processor)wrapper.getCurrentProcessor();
            if (getLog().isDebugEnabled()) {
                getLog().debug(sm.getString("abstractConnectionHandler.connectionsGet",
                    processor, socket));
            }

            // Timeouts are calculated on a dedicated thread and then
            // dispatched. Because of delays in the dispatch process, the
            // timeout may no longer be required. Check here and avoid
            // unnecessary processing.
            if (SocketEvent.TIMEOUT == status &&
                (processor == null ||
                    !processor.isAsync() && !processor.isUpgrade() ||
                    processor.isAsync() && !processor.checkAsyncTimeoutGeneration())) {
                // This is effectively a NO-OP
                return SocketState.OPEN;
            }

            if (processor != null) {
                // Make sure an async timeout doesn't fire
                getProtocol().removeWaitingProcessor(processor);
            }
            else if (status == SocketEvent.DISCONNECT || status == SocketEvent.ERROR) {
                // Nothing to do. Endpoint requested a close and there is no
                // longer a processor associated with this socket.
                return SocketState.CLOSED;
            }

            ContainerThreadMarker.set();

            try {
                if (processor == null) {
                    String negotiatedProtocol = wrapper.getNegotiatedProtocol();
                    // OpenSSL typically returns null whereas JSSE typically
                    // returns "" when no protocol is negotiated
                    if (negotiatedProtocol != null && negotiatedProtocol.length() > 0) {
                        UpgradeProtocol upgradeProtocol =
                            getProtocol().getNegotiatedProtocol(negotiatedProtocol);
                        if (upgradeProtocol != null) {
                            processor = upgradeProtocol.getProcessor(
                                wrapper, getProtocol().getAdapter());
                        }
                        else if (negotiatedProtocol.equals("http/1.1")) {
                            // Explicitly negotiated the default protocol.
                            // Obtain a processor below.
                        }
                        else {
                            // TODO:
                            // OpenSSL 1.0.2's ALPN callback doesn't support
                            // failing the handshake with an error if no
                            // protocol can be negotiated. Therefore, we need to
                            // fail the connection here. Once this is fixed,
                            // replace the code below with the commented out
                            // block.
                            if (getLog().isDebugEnabled()) {
                                getLog().debug(sm.getString(
                                    "abstractConnectionHandler.negotiatedProcessor.fail",
                                    negotiatedProtocol));
                            }
                            return SocketState.CLOSED;
                            /*
                             * To replace the code above once OpenSSL 1.1.0 is
                             * used.
                            // Failed to create processor. This is a bug.
                            throw new IllegalStateException(sm.getString(
                                    "abstractConnectionHandler.negotiatedProcessor.fail",
                                    negotiatedProtocol));
                            */
                        }
                    }
                }
                if (processor == null) {
                    processor = recycledProcessors.pop();
                    if (getLog().isDebugEnabled()) {
                        getLog().debug(sm.getString("abstractConnectionHandler.processorPop",
                            processor));
                    }
                }
                if (processor == null) {
                    processor = getProtocol().createProcessor();
                    register(processor);
                }

                processor.setSslSupport(
                    wrapper.getSslSupport(getProtocol().getClientCertProvider()));

                // Associate the processor with the connection
                wrapper.setCurrentProcessor(processor);

                SocketState state = SocketState.CLOSED;
                do {
                    state = processor.process(wrapper, status);

                    if (state == SocketState.UPGRADING) {
                        // Get the HTTP upgrade handler
                        UpgradeToken upgradeToken = processor.getUpgradeToken();
                        // Retrieve leftover input
                        ByteBuffer leftOverInput = processor.getLeftoverInput();
                        if (upgradeToken == null) {
                            // Assume direct HTTP/2 connection
                            UpgradeProtocol upgradeProtocol = getProtocol().getUpgradeProtocol("h2c");
                            if (upgradeProtocol != null) {
                                processor = upgradeProtocol.getProcessor(
                                    wrapper, getProtocol().getAdapter());
                                wrapper.unRead(leftOverInput);
                                // Associate with the processor with the connection
                                wrapper.setCurrentProcessor(processor);
                            }
                            else {
                                if (getLog().isDebugEnabled()) {
                                    getLog().debug(sm.getString(
                                        "abstractConnectionHandler.negotiatedProcessor.fail",
                                        "h2c"));
                                }
                                return SocketState.CLOSED;
                            }
                        }
                        else {
                            HttpUpgradeHandler httpUpgradeHandler = upgradeToken.getHttpUpgradeHandler();
                            // Release the Http11 processor to be re-used
                            release(processor);
                            // Create the upgrade processor
                            processor = getProtocol().createUpgradeProcessor(wrapper, upgradeToken);
                            if (getLog().isDebugEnabled()) {
                                getLog().debug(sm.getString("abstractConnectionHandler.upgradeCreate",
                                    processor, wrapper));
                            }
                            wrapper.unRead(leftOverInput);
                            // Mark the connection as upgraded
                            wrapper.setUpgraded(true);
                            // Associate with the processor with the connection
                            wrapper.setCurrentProcessor(processor);
                            // Initialise the upgrade handler (which may trigger
                            // some IO using the new protocol which is why the lines
                            // above are necessary)
                            // This cast should be safe. If it fails the error
                            // handling for the surrounding try/catch will deal with
                            // it.
                            if (upgradeToken.getInstanceManager() == null) {
                                httpUpgradeHandler.init((WebConnection)processor);
                            }
                            else {
                                ClassLoader oldCL = upgradeToken.getContextBind().bind(false, null);
                                try {
                                    httpUpgradeHandler.init((WebConnection)processor);
                                }
                                finally {
                                    upgradeToken.getContextBind().unbind(false, oldCL);
                                }
                            }
                            if (httpUpgradeHandler instanceof InternalHttpUpgradeHandler) {
                                if (((InternalHttpUpgradeHandler)httpUpgradeHandler).hasAsyncIO()) {
                                    // The handler will initiate all further I/O
                                    state = SocketState.LONG;
                                }
                            }
                        }
                    }
                }
                while (state == SocketState.UPGRADING);

                if (state == SocketState.LONG) {
                    // In the middle of processing a request/response. Keep the
                    // socket associated with the processor. Exact requirements
                    // depend on type of long poll
                    longPoll(wrapper, processor);
                    if (processor.isAsync()) {
                        getProtocol().addWaitingProcessor(processor);
                    }
                }
                else if (state == SocketState.OPEN) {
                    // In keep-alive but between requests. OK to recycle
                    // processor. Continue to poll for the next request.
                    wrapper.setCurrentProcessor(null);
                    release(processor);
                    wrapper.registerReadInterest();
                }
                else if (state == SocketState.SENDFILE) {
                    // Sendfile in progress. If it fails, the socket will be
                    // closed. If it works, the socket either be added to the
                    // poller (or equivalent) to await more data or processed
                    // if there are any pipe-lined requests remaining.
                }
                else if (state == SocketState.UPGRADED) {
                    // Don't add sockets back to the poller if this was a
                    // non-blocking write otherwise the poller may trigger
                    // multiple read events which may lead to thread starvation
                    // in the connector. The write() method will add this socket
                    // to the poller if necessary.
                    if (status != SocketEvent.OPEN_WRITE) {
                        longPoll(wrapper, processor);
                        getProtocol().addWaitingProcessor(processor);
                    }
                }
                else if (state == SocketState.SUSPENDED) {
                    // Don't add sockets back to the poller.
                    // The resumeProcessing() method will add this socket
                    // to the poller.
                }
                else {
                    // Connection closed. OK to recycle the processor.
                    // Processors handling upgrades require additional clean-up
                    // before release.
                    wrapper.setCurrentProcessor(null);
                    if (processor.isUpgrade()) {
                        UpgradeToken upgradeToken = processor.getUpgradeToken();
                        HttpUpgradeHandler httpUpgradeHandler = upgradeToken.getHttpUpgradeHandler();
                        InstanceManager instanceManager = upgradeToken.getInstanceManager();
                        if (instanceManager == null) {
                            httpUpgradeHandler.destroy();
                        }
                        else {
                            ClassLoader oldCL = upgradeToken.getContextBind().bind(false, null);
                            try {
                                httpUpgradeHandler.destroy();
                            }
                            finally {
                                try {
                                    instanceManager.destroyInstance(httpUpgradeHandler);
                                }
                                catch (Throwable e) {
                                    ExceptionUtils.handleThrowable(e);
                                    getLog().error(sm.getString("abstractConnectionHandler.error"), e);
                                }
                                upgradeToken.getContextBind().unbind(false, oldCL);
                            }
                        }
                    }
                    release(processor);
                }
                return state;
            }
            catch (java.net.SocketException e) {
                // SocketExceptions are normal
                getLog().debug(sm.getString(
                    "abstractConnectionHandler.socketexception.debug"), e);
            }
            catch (java.io.IOException e) {
                // IOExceptions are normal
                getLog().debug(sm.getString(
                    "abstractConnectionHandler.ioexception.debug"), e);
            }
            catch (ProtocolException e) {
                // Protocol exceptions normally mean the client sent invalid or
                // incomplete data.
                getLog().debug(sm.getString(
                    "abstractConnectionHandler.protocolexception.debug"), e);
            }
            // Future developers: if you discover any other
            // rare-but-nonfatal exceptions, catch them here, and log as
            // above.
            catch (OutOfMemoryError oome) {
                // Try and handle this here to give Tomcat a chance to close the
                // connection and prevent clients waiting until they time out.
                // Worst case, it isn't recoverable and the attempt at logging
                // will trigger another OOME.
                getLog().error(sm.getString("abstractConnectionHandler.oome"), oome);
            }
            catch (Throwable e) {
                ExceptionUtils.handleThrowable(e);
                // any other exception or error is odd. Here we log it
                // with "ERROR" level, so it will show up even on
                // less-than-verbose logs.
                getLog().error(sm.getString("abstractConnectionHandler.error"), e);
            }
            finally {
                ContainerThreadMarker.clear();
            }

            // Make sure socket/processor is removed from the list of current
            // connections
            wrapper.setCurrentProcessor(null);
            release(processor);
            return SocketState.CLOSED;
        }

        protected void longPoll(SocketWrapperBase<?> socket, Processor processor) {
            if (!processor.isAsync()) {
                // This is currently only used with HTTP
                // Either:
                //  - this is an upgraded connection
                //  - the request line/headers have not been completely
                //    read
                socket.registerReadInterest();
            }
        }

        @Override
        public Set<S> getOpenSockets() {
            Set<SocketWrapperBase<NioChannel>> set = proto.getEndpoint().getConnections();
            Set<S> result = new HashSet<S>();
            for (SocketWrapperBase<NioChannel> socketWrapper : set) {
                S socket = (S)socketWrapper.getSocket();
                if (socket != null) {
                    result.add(socket);
                }
            }
            return result;
        }

        /**
         * Expected to be used by the Endpoint to release resources on socket close, errors etc.
         */
        @Override
        public void release(SocketWrapperBase<S> socketWrapper) {
            Processor processor = (Processor)socketWrapper.getCurrentProcessor();
            socketWrapper.setCurrentProcessor(null);
            release(processor);
        }

        /**
         * Expected to be used by the handler once the processor is no longer required.
         *
         * @param processor Processor being released (that was associated with the socket)
         */
        private void release(Processor processor) {
            if (processor != null) {
                processor.recycle();
                if (processor.isUpgrade()) {
                    // UpgradeProcessorInternal instances can utilise AsyncIO.
                    // If they do, the processor will not pass through the
                    // process() method and be removed from waitingProcessors
                    // so do that here.
                    if (processor instanceof UpgradeProcessorInternal) {
                        if (((UpgradeProcessorInternal)processor).hasAsyncIO()) {
                            getProtocol().removeWaitingProcessor(processor);
                        }
                    }
                }
                else {
                    // After recycling, only instances of UpgradeProcessorBase
                    // will return true for isUpgrade().
                    // Instances of UpgradeProcessorBase should not be added to
                    // recycledProcessors since that pool is only for AJP or
                    // HTTP processors
                    recycledProcessors.push(processor);
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Pushed Processor [" + processor + "]");
                    }
                }
            }
        }



        protected void register(Processor processor) {
            if (getProtocol().getDomain() != null) {
                synchronized (this) {
                    try {
                        long count = registerCount.incrementAndGet();
                        RequestInfo rp =
                            processor.getRequest().getRequestProcessor();
                        rp.setGlobalProcessor(global);
                        ObjectName rpName = new ObjectName(
                            getProtocol().getDomain() +
                                ":type=RequestProcessor,worker="
                                + getProtocol().getName() +
                                ",name=" + getProtocol().getProtocolName() +
                                "Request" + count);
                        if (getLog().isDebugEnabled()) {
                            getLog().debug("Register [" + processor + "] as [" + rpName + "]");
                        }
                        Registry.getRegistry(null, null).registerComponent(rp,
                            rpName, null);
                        rp.setRpName(rpName);
                    }
                    catch (Exception e) {
                        getLog().warn(sm.getString("abstractProtocol.processorRegisterError"), e);
                    }
                }
            }
        }

        protected void unregister(Processor processor) {
            if (getProtocol().getDomain() != null) {
                synchronized (this) {
                    try {
                        Request r = processor.getRequest();
                        if (r == null) {
                            // Probably an UpgradeProcessor
                            return;
                        }
                        RequestInfo rp = r.getRequestProcessor();
                        rp.setGlobalProcessor(null);
                        ObjectName rpName = rp.getRpName();
                        if (getLog().isDebugEnabled()) {
                            getLog().debug("Unregister [" + rpName + "]");
                        }
                        Registry.getRegistry(null, null).unregisterComponent(
                            rpName);
                        rp.setRpName(null);
                    }
                    catch (Exception e) {
                        getLog().warn(sm.getString("abstractProtocol.processorUnregisterError"), e);
                    }
                }
            }
        }

        @Override
        public final void pause() {
            /*
             * Inform all the processors associated with current connections
             * that the endpoint is being paused. Most won't care. Those
             * processing multiplexed streams may wish to take action. For
             * example, HTTP/2 may wish to stop accepting new streams.
             *
             * Note that even if the endpoint is resumed, there is (currently)
             * no API to inform the Processors of this.
             */
            for (SocketWrapperBase<NioChannel> wrapper : proto.getEndpoint().getConnections()) {
                Processor processor = (Processor)wrapper.getCurrentProcessor();
                if (processor != null) {
                    processor.pause();
                }
            }
        }

        protected static class RecycledProcessors extends SynchronizedStack<Processor> {

            private final transient MyHandler<NioChannel> handler;
            protected final AtomicInteger size = new AtomicInteger(0);

            public RecycledProcessors(MyHandler handler) {
                this.handler = handler;
            }



            @SuppressWarnings("sync-override") // Size may exceed cache size a bit
            @Override
            public boolean push(Processor processor) {
                int cacheSize = handler.getProtocol().getProcessorCache();
                boolean offer = cacheSize == -1 ? true : size.get() < cacheSize;
                //avoid over growing our cache or add after we have stopped
                boolean result = false;
                if (offer) {
                    result = super.push(processor);
                    if (result) {
                        size.incrementAndGet();
                    }
                }
                if (!result)
                    handler.unregister(processor);
                return result;
            }

            @SuppressWarnings("sync-override") // OK if size is too big briefly
            @Override
            public Processor pop() {
                Processor result = super.pop();
                if (result != null) {
                    size.decrementAndGet();
                }
                return result;
            }

            @Override
            public synchronized void clear() {
                Processor next = pop();
                while (next != null) {
                    handler.unregister(next);
                    next = pop();
                }
                super.clear();
                size.set(0);
            }
        }
    }
}
