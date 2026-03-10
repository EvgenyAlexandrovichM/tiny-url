package effectivemobile.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShortUrlResponse {

    private String shortCode;
    private String shortUrl;
}
