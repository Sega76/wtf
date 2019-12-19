//package app;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.annotation.Bean;
//import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.core.authority.AuthorityUtils;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//
//@EnableWebSecurity
//@SpringBootApplication
//@EnableGlobalMethodSecurity(prePostEnabled = true)
//public class CertAuthServer extends WebSecurityConfigurerAdapter {
//    public static void main(String[] args) {
//        SpringApplication.run(CertAuthServer.class, args);
//    }
//
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http.authorizeRequests().anyRequest().authenticated()
//            .and().csrf().disable()
//            .x509()
//            .subjectPrincipalRegex("CN=(.*?),")
//            .userDetailsService(userDetailsService());
//    }
//
//    @Bean
//    public UserDetailsService userDetailsService() {
//        return new UserDetailsService() {
//            @Override
//            public UserDetails loadUserByUsername(String username) {
//                if (username.equals("Alex.Pashnin")) {
//                    return new User(username, "",
//                        AuthorityUtils
//                            .commaSeparatedStringToAuthorityList("ROLE_USER"));
//                }
//                return null;
//            }
//        };
//    }
//}