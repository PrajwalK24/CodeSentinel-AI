package com.codesentinel;

import com.codesentinel.model.Role;
import com.codesentinel.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CodeSentinelApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeSentinelApplication.class, args);
    }

    @Bean
    CommandLineRunner seedUsers(UserService userService) {
        return args -> {
            userService.upsertSeedUser("Prajwal Admin", "prajwaladmin", "prajwalkulloli18@gmail.com", "Prajwal@1234", Role.ADMIN);
            userService.createSeedUser("Developer User", "developer", "dev@codesentinel.com", "Dev@1234", Role.DEVELOPER);
        };
    }
}
