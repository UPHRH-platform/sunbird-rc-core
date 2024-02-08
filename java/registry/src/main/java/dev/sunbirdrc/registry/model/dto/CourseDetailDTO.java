package dev.sunbirdrc.registry.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseDetailDTO {
    @JsonProperty("councilName")
    private String councilName;

    @JsonProperty("courseName")
    private String courseName;

    @JsonProperty("activityName")
    private String activityName;

    @JsonProperty("entityName")
    private String entityName;

    @JsonProperty("courseType")
    private String courseType;
}
