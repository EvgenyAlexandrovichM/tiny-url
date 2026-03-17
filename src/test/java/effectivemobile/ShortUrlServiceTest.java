package effectivemobile;

import effectivemobile.dto.CreateShortUrlRequest;
import effectivemobile.dto.ShortUrlResponse;
import effectivemobile.entity.ShortUrl;
import effectivemobile.exception.AliasAlreadyExistsException;
import effectivemobile.exception.InvalidUrlFormatException;
import effectivemobile.exception.ShortUrlNotFoundException;
import effectivemobile.repository.ShortUrlRepository;
import effectivemobile.service.ShortUrlService;
import effectivemobile.util.ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private ShortCodeGenerator generator;

    @InjectMocks
    private ShortUrlService service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
    }

    @Test
    void createShortUrl_withoutAlias_success() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();

        request.setOriginalUrl("https://google.com");

        when(generator.generate()).thenReturn("abc123");
        when(repository.existsByShortCode("abc123")).thenReturn(false);
        when(repository.existsByAlias("abc123")).thenReturn(false);

        ShortUrlResponse response = service.createShortUrl(request);

        assertEquals("abc123", response.getShortCode());
        assertEquals("http://localhost:8080/abc123", response.getShortUrl());
    }

    @Test
    void createShortUrl_withAlias_success() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://google.com");
        request.setAlias("gOoOoOoOogle.com");

        when(repository.existsByAlias("goooooooogle.com")).thenReturn(false);

        ShortUrlResponse response = service.createShortUrl(request);

        assertEquals("goooooooogle.com", response.getShortCode());
        assertEquals("http://localhost:8080/goooooooogle.com", response.getShortUrl());
    }

    @Test
    void createShortUrl_withTtl_success() {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://google.com");
        request.setTtlSeconds(60L);

        when(generator.generate()).thenReturn("abc123");
        when(repository.existsByShortCode("abc123")).thenReturn(false);
        when(repository.existsByAlias("abc123")).thenReturn(false);

        ShortUrlResponse response = service.createShortUrl(request);

        assertEquals("abc123", response.getShortCode());
    }

    @Test
    void resolveOriginalUrl_byShortCode_success() {
        ShortUrl url = ShortUrl.builder()
                .shortCode("abc123")
                .originalUrl("https://google.com")
                .build();

        when(repository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        String result = service.resolveOriginalUrl("abc123");

        assertEquals("https://google.com", result);
    }

    @Test
    void resolveOriginalUrl_byAlias_success() {
        ShortUrl url = ShortUrl.builder()
                .alias("gOoOoOoOogle.com")
                .originalUrl("https://google.com")
                .build();

        when(repository.findByShortCode("goooooooogle.com")).thenReturn(Optional.empty());
        when(repository.findByAlias("goooooooogle.com")).thenReturn(Optional.of(url));

        String result = service.resolveOriginalUrl("goooooooogle.com");

        assertEquals("https://google.com", result);
    }

    @Test
    void createShortUrl_invalidUrl_throws() {
        CreateShortUrlRequest req = new CreateShortUrlRequest();
        req.setOriginalUrl("htp:/bad");

        assertThrows(InvalidUrlFormatException.class,
                () -> service.createShortUrl(req));
    }

    @Test
    void createShortUrl_aliasExists_throws() {
        CreateShortUrlRequest req = new CreateShortUrlRequest();
        req.setOriginalUrl("https://google.com");
        req.setAlias("gOoOoOoOogle.com");

        when(repository.existsByAlias("goooooooogle.com")).thenReturn(true);

        assertThrows(AliasAlreadyExistsException.class,
                () -> service.createShortUrl(req));
    }

    @Test
    void resolveOriginalUrl_notFound_throws() {
        when(repository.findByShortCode("unknown")).thenReturn(Optional.empty());
        when(repository.findByAlias("unknown")).thenReturn(Optional.empty());

        assertThrows(ShortUrlNotFoundException.class,
                () -> service.resolveOriginalUrl("unknown"));
    }

    @Test
    void resolveOriginalUrl_expired_throws() {
        ShortUrl url = ShortUrl.builder()
                .shortCode("gOoOoOoOogle.com")
                .originalUrl("https://google.com")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(repository.findByShortCode("goooooooogle.com")).thenReturn(Optional.of(url));

        assertThrows(ShortUrlNotFoundException.class,
                () -> service.resolveOriginalUrl("goooooooogle.com"));
    }

    @Test
    void normalizeUrl_lowercaseSchemeAndHost() {
        CreateShortUrlRequest req = new CreateShortUrlRequest();
        req.setOriginalUrl("HTTP://GOOGLE.COM/Path");

        when(generator.generate()).thenReturn("abc123");
        when(repository.existsByShortCode("abc123")).thenReturn(false);
        when(repository.existsByAlias("abc123")).thenReturn(false);

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);

        service.createShortUrl(req);

        verify(repository).save(captor.capture());

        ShortUrl saved = captor.getValue();

        assertEquals("http://google.com/Path", saved.getOriginalUrl());

    }


}
