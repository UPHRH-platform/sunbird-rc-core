package dev.sunbirdrc.registry.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class EventInternal {
    private  String eid;
    private String userId;
    private  Long ets;
    private  String ver;
    private  String mid;
    private  String telemetryId;
    private  String type;
    private  String courseName;

}
