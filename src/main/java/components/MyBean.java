package components;

import java.util.EventObject;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MyBean {
    @EventListener
    public void handleEvent (EventObject e) {
        System.out.println("-- RequestHandledEvent --");
        System.out.println(e);
    }
}