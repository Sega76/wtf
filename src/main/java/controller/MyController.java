package controller;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MyController {

    @GetMapping(path = "/test")
    public String test(@Autowired HttpServletRequest request) {
        return "test";

    }
}
