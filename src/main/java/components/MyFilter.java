//package components;
//
//import java.io.IOException;
//import java.util.Enumeration;
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.ServletRequest;
//import javax.servlet.ServletResponse;
//import org.springframework.security.web.FilterChainProxy;
//import org.springframework.stereotype.Component;
//
//@Component
//public class MyFilter extends FilterChainProxy {
//
//    @Override public void doFilter(ServletRequest request, ServletResponse response,
//        FilterChain chain) throws IOException, ServletException {
//
//        System.err.println(MyFilter.class.getName());
//
//        Enumeration<String> names = request.getAttributeNames();
//
//        while (names.hasMoreElements())
//            System.err.println(names.nextElement());
//
//        super.doFilter(request, response, chain);
//    }
//}
