package effectivemobile;

import effectivemobile.dto.CreateShortUrlRequest;
import effectivemobile.dto.ResponseError;
import effectivemobile.dto.ShortUrlResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class ShortUrlControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("tinyurl")
            .withUsername("username")
            .withPassword("password");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.base-url", () -> "http:://localhost");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createShortUrl_success() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://google.com");

        ResponseEntity<ShortUrlResponse> response =
                restTemplate.postForEntity("/api/urls", request, ShortUrlResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getShortCode().isEmpty());
    }

    @Test
    void redirect_success() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://google.com");

        ShortUrlResponse created =
                restTemplate.postForEntity("/api/urls", request, ShortUrlResponse.class).getBody();

        ResponseEntity<Void> redirect =
                restTemplate.getForEntity("/" + created.getShortCode(), Void.class);

        assertEquals(HttpStatus.FOUND, redirect.getStatusCode());
        assertEquals("https://google.com", Objects.requireNonNull(redirect.getHeaders().getLocation()).toString());
    }

    @Test
    void redirect_notFound() {
        ResponseEntity<ResponseError> response =
                restTemplate.getForEntity("/unknown111", ResponseError.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not Found", Objects.requireNonNull(response.getBody()).getError());
    }

    @Test
    void createShortUrl_invalidUrl() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("httttp:/googoel");

        ResponseEntity<ResponseError> response =
                restTemplate.postForEntity("/api/urls", request, ResponseError.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", Objects.requireNonNull(response.getBody()).getError());
    }

    @Test
    void createShortUrl_aliasConflict() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://google.com");
        request.setAlias("alias");

        restTemplate.postForEntity("/api/urls", request, ShortUrlResponse.class);

        ResponseEntity<ResponseError> response =
                restTemplate.postForEntity("/api/urls", request, ResponseError.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", Objects.requireNonNull(response.getBody()).getError());
    }
}
