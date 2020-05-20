package org.eclipse.jetty.websocket.javax.tests;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.javax.client.internal.JavaxWebSocketClientContainer;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JavaxDecodersTest
{
    private Server server;
    private URI serverUri;
    private JavaxWebSocketClientContainer client;

    @BeforeEach
    public void before() throws Exception
    {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");
        server.setHandler(contextHandler);
        JavaxWebSocketServletContainerInitializer.configure(contextHandler, (context, container) ->
            container.addEndpoint(DecoderServerEndpoint.class));
        server.start();
        serverUri = WSURI.toWebsocket(server.getURI());

        client = new JavaxWebSocketClientContainer();
        client.start();
    }

    @AfterEach
    public void after() throws Exception
    {
        server.stop();
        client.stop();
    }

    @Test
    public void test() throws Exception
    {
        EventSocket clientEndpoint = new EventSocket();
        Session session = client.connectToServer(clientEndpoint, serverUri);
        session.getBasicRemote().sendText("test");
        String response = clientEndpoint.textMessages.poll(3, TimeUnit.SECONDS);
        System.err.println(response);
    }

    public static class MyString
    {
        private final String _string;

        public MyString(String s)
        {
            _string = s;
        }

        public String getString()
        {
            return _string;
        }
    }

    public static class MyStringExtension extends MyString
    {

        public MyStringExtension(String s)
        {
            super(s);
        }

        @Override
        public String getString()
        {
            return super.getString() + ":Extension";
        }
    }

    public static class MyDecoder implements Decoder.Text<MyStringExtension>
    {
        @Override
        public MyStringExtension decode(String s)
        {
            return new MyStringExtension(s);
        }

        @Override
        public void init(EndpointConfig config)
        {
        }

        @Override
        public void destroy()
        {
        }

        @Override
        public boolean willDecode(String s)
        {
            return true;
        }
    }

    @ServerEndpoint(value = "/", decoders = MyDecoder.class)
    public static class DecoderServerEndpoint
    {
        @OnMessage
        public void onMessage(MyString message)
        {
            System.err.println(message.getString());
        }
    }
}
