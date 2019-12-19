//package controller;
//
//import java.security.Principal;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping
//public class TestController {
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    @GetMapping("/test")
//    public String hello(Principal principal) {
//        System.err.println(principal.getName());
//        UserDetails currentUser
//            = (UserDetails) ((Authentication) principal).getPrincipal();
//        return "Hello " + currentUser.getUsername()+"!";
//    }
//}