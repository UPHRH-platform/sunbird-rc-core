package dev.sunbirdrc.registry.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDao {
    private Long id;

    private String eid;

    private String userId;

    private Long ets;

    private String ver;
    private String mid;

    private String courseName;

    private String telemetryId;
    private String type;
}
