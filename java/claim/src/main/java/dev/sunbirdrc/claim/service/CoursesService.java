package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.dto.CourseDetailDTO;
import dev.sunbirdrc.claim.entity.Courses;
import dev.sunbirdrc.claim.exception.InvalidInputException;
import dev.sunbirdrc.claim.exception.ResourceNotFoundException;
import dev.sunbirdrc.claim.model.EntityType;
import dev.sunbirdrc.claim.repository.CourseDetailsRepository;
import dev.sunbirdrc.claim.repository.CoursesRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CoursesService {

    private CoursesRepository coursesRepository;

    @Autowired
    private CourseDetailsRepository courseDetailsRepository;

    @Autowired
    public CoursesService(CoursesRepository coursesRepository) {
        this.coursesRepository = coursesRepository;
    }

    public List<Courses> getAllCourses() {
        return coursesRepository.findAll();
    }

    public Optional<Courses> getCourseById(Long id) {
        return coursesRepository.findById(id);
    }

    public Optional<Courses> getCourseByCourse(String category) {
        return coursesRepository.findByCategory(category);
    }

    // find Couse Name
    public List<String> getCourseByCategory(String category) {
        return coursesRepository.findByFieldName(category);
    }

    //find TemplateKey
    public String getCourseTemplateKey(String courseName) {
        return coursesRepository.findByCouseName(courseName);
    }

    public String getCourseTemplateKey(String courseName, String requestType) {
        return coursesRepository.findByCouseNameWithRequestType(courseName, requestType);
    }
    public Courses createCourse(Courses course) {
        return coursesRepository.save(course);
    }

    /**
     * @param courseDetailDTO
     * @return
     */
    public String getCourseKey(CourseDetailDTO courseDetailDTO) {
        if (courseDetailDTO != null && !StringUtils.isEmpty(courseDetailDTO.getCouncilName())
                && courseDetailDTO.getEntityName() != null) {
            log.info(">>>>>>>>>>>>> course details: " + courseDetailDTO);

            Optional<String> courseKey = Optional.empty();

            if (EntityType.StudentForeignVerification.equals(courseDetailDTO.getEntityName())) {
                courseKey = courseDetailsRepository.findCourseKeyByCouncilAndCategory(courseDetailDTO.getCouncilName(),
                        courseDetailDTO.getEntityName().name());

            } else if (EntityType.StudentGoodstanding.equals(courseDetailDTO.getEntityName())) {
                courseKey = courseDetailsRepository.findCourseKeyByCouncilAndCategory(courseDetailDTO.getCouncilName(),
                        courseDetailDTO.getEntityName().name());

            } else if (EntityType.DiplomaFromUP.equals(courseDetailDTO.getEntityName())) {
                if (!StringUtils.isEmpty(courseDetailDTO.getCourseName())
                        && !StringUtils.isEmpty(courseDetailDTO.getCourseType())) {

                    courseDetailDTO.setActivityName(courseDetailDTO.getActivityName() != null ? courseDetailDTO.getActivityName() : "");

                    courseKey = courseDetailsRepository.findCourseKeyForDiploma(courseDetailDTO.getCouncilName(),
                            courseDetailDTO.getCourseName(), courseDetailDTO.getEntityName().name(), courseDetailDTO.getActivityName());
                }

            } else if (EntityType.StudentFromUP.equals(courseDetailDTO.getEntityName())
                    || EntityType.StudentOutsideUP.equals(courseDetailDTO.getEntityName())) {

                if (!StringUtils.isEmpty(courseDetailDTO.getCourseName())
                        && !StringUtils.isEmpty(courseDetailDTO.getActivityName())
                        && !StringUtils.isEmpty(courseDetailDTO.getCourseType())) {

                    courseKey = courseDetailsRepository.findCourseKeyForGeneralCategory(courseDetailDTO.getCouncilName(),
                            courseDetailDTO.getCourseName(), courseDetailDTO.getActivityName(),
                            courseDetailDTO.getEntityName().name(), courseDetailDTO.getCourseType().toUpperCase());
                }else {
                    throw new InvalidInputException("Invalid input exception while getting course key from " +
                            "general(UP & outside) category");
                }
            }

            if (courseKey.isPresent()) {
                return courseKey.get();
            } else {
                throw new ResourceNotFoundException("Unable to fine course key in DB");
            }
        } else {
            throw new InvalidInputException("Invalid input exception while getting course key");
        }
    }


}
