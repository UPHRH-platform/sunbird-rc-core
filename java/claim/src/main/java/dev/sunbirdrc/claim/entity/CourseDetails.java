package dev.sunbirdrc.claim.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "councilName")
    private String councilName;

    @Column(name = "courseName")
    private String courseName;

    @Column(name = "activityName")
    private String activityName;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "courseKey")
    private String courseKey;

    @Column(name = "entityName")
    private String entityName;

    @Column(name = "courseType")
    private String courseType;

}
