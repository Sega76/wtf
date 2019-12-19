package other;

import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;

public class MyNioEndpoint extends NioEndpoint {

    @Override public Handler<NioChannel> getHandler() {
        return super.getHandler();
    }

    @Override public void setHandler(Handler<NioChannel> handler) {
        super.setHandler(handler);
    }

}
