package dev.sunbirdrc.claim.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eid")
    private String eid;

    @Column(name = "userId")
    private String userId;

    @Column(name = "ets")
    private Long ets;

    @Column(name = "ver")
    private String ver;

    @Column(name = "mid")
    private String mid;

    @Column(name = "courseName")
    private String courseName;

    @Column(name ="telemetryId")
    private String telemetryId;
    @Column(name ="type")
    private String type;
}
