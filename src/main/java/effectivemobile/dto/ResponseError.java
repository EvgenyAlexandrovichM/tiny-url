package effectivemobile.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResponseError {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
