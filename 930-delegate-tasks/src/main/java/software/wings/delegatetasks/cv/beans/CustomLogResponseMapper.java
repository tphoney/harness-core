package software.wings.delegatetasks.cv.beans;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomLogResponseMapper {
    private String fieldName;
    private String fieldValue;
    private List<String> jsonPath;
    private List<String> regexs;
    private String timestampFormat;
}
