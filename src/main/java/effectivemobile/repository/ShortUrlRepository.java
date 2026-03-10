package effectivemobile.repository;

import effectivemobile.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    Optional<ShortUrl> findByAlias(String alias);

    boolean existsByShortCode(String shortCode);

    boolean existsByAlias(String alias);

}
