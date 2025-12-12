package test.java.com.example.ecommerce.controller;

import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private UserRepository userRepository;

    @Test
    public void testRegisterAndLogin() {
        User user = new User();
        user.setEmail("testuser@example.com");
        user.setPassword("testpass");
        user.setTier("BRONZE");
        webTestClient.post().uri("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated();

        webTestClient.post().uri("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).value(token -> {
                assert token != null && token.length() > 10;
            });
    }
}
