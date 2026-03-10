package effectivemobile.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateShortUrlRequest {

    @NotBlank
    @Size(max = 2048)
    private String originalUrl;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,64}$")
    private String alias;

    @Min(value = 1)
    private Long ttlSeconds;
}
