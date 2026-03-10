package effectivemobile.service;

import effectivemobile.dto.CreateShortUrlRequest;
import effectivemobile.dto.ShortUrlResponse;
import effectivemobile.entity.ShortUrl;
import effectivemobile.exception.AliasAlreadyExistsException;
import effectivemobile.exception.InvalidUrlFormatException;
import effectivemobile.exception.ShortCodeGenerationException;
import effectivemobile.exception.ShortUrlNotFoundException;
import effectivemobile.repository.ShortUrlRepository;
import effectivemobile.util.ShortCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ShortUrlService {

    private final ShortUrlRepository repository;
    private final ShortCodeGenerator shortCodeGenerator;

    @Value("${app.base-url}")
    private String baseUrl;

    public ShortUrlResponse createShortUrl(CreateShortUrlRequest request) {
        log.info("Creating short URL for={}", request.getOriginalUrl());

        String normalized = normalizeUrl(request.getOriginalUrl());
        validateUrl(normalized);

        String code = resolveShortCode(request);
        LocalDateTime expiresAt = resolveTtl(request);

        log.debug("Generated short code={}", code);
        ShortUrl entity = ShortUrl.builder()
                .shortCode(code)
                .alias(request.getAlias())
                .originalUrl(normalized)
                .expiresAt(expiresAt)
                .build();
        repository.save(entity);
        log.info("Short URL saved with id={} and code={}", entity.getId(), code);

        return ShortUrlResponse.builder()
                .shortCode(code)
                .shortUrl(baseUrl + "/" + code)
                .build();
    }

    @Transactional(readOnly = true)
    public String resolveOriginalUrl(String shortCodeOrAlias) {
        log.info("Resolving short code={}", shortCodeOrAlias);

        ShortUrl entity = repository.findByShortCode(shortCodeOrAlias)
                .or(() -> repository.findByAlias(shortCodeOrAlias))
                .orElseThrow(() -> {
                    log.warn("Short code not found={}", shortCodeOrAlias);
                    return new ShortUrlNotFoundException("Short URL not found");
                });

        if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Short URL expired code/alias={}", shortCodeOrAlias);
            throw new ShortUrlNotFoundException("Short URL expired");
        }
        log.debug("Resolved short code={} to URL={}", shortCodeOrAlias, entity.getOriginalUrl());
        return entity.getOriginalUrl();
    }

    private String resolveShortCode(CreateShortUrlRequest request) {
        String alias = normalizeAlias(request.getAlias());
        if (alias != null) {
            if (repository.existsByAlias(alias)) {
                log.warn("Alias={} already exists", request.getAlias());
                throw new AliasAlreadyExistsException("Alias already taken");
            }
            return alias;
        }
        return generateUniqueCode();
    }

    private LocalDateTime resolveTtl(CreateShortUrlRequest request) {
        if (request.getTtlSeconds() == null) {
            return null;
        }
        return LocalDateTime.now().plusSeconds(request.getTtlSeconds());
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 5; i++) {
            String code = shortCodeGenerator.generate();
            if (!repository.existsByShortCode(code) && !repository.existsByAlias(code)) {
                return code;
            }
            log.warn("Collision detected for code={}, retrying...", code);
        }
        log.error("Failed to generate unique short code after retries");
        throw new ShortCodeGenerationException("Failed to generate unique short code");
    }

    private String normalizeUrl(String url) {
        try {
            URL parsed = new URL(url);
            URI uri = new URI(
                    parsed.getProtocol().toLowerCase(),
                    parsed.getUserInfo(),
                    parsed.getHost().toLowerCase(),
                    parsed.getPort(),
                    parsed.getPath(),
                    parsed.getQuery(),
                    null
            );

            String normalized = uri.toASCIIString();
            log.debug("Normalized URL={} -> {}", url, normalized);
            return normalized;

        } catch (Exception e) {
            log.warn("Failed to normalize URL={}", url);
            throw new InvalidUrlFormatException("Invalid URL format");
        }
    }

    private String normalizeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }
        return alias.toLowerCase();
    }


    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);

            if (uri.getScheme() == null ||
                    (!uri.getScheme().equalsIgnoreCase("http") &&
                            !uri.getScheme().equalsIgnoreCase("https"))) {
                log.warn("Invalid scheme in URL={}", url);
                throw new InvalidUrlFormatException("Only HTTP/HTTPS Urls are allowed");
            }

            if (url.length() > 2048) {
                log.warn("URL too long={}", url);
                throw new InvalidUrlFormatException("URL is too long");
            }

            if (uri.getHost() == null) {
                log.warn("URL missing host={}", url);
                throw new InvalidUrlFormatException("URL must contain a valid host");
            }

        } catch (URISyntaxException e) {
            log.warn("Invalid URL syntax={}", url);
            throw new InvalidUrlFormatException("Invalid URL format");
        }
    }
}
