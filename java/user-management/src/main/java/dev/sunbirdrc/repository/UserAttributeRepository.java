package dev.sunbirdrc.repository;

import dev.sunbirdrc.dto.UserAttributeModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAttributeRepository extends JpaRepository<UserAttributeModel,String> {
    @Query(value = "SELECT * FROM user_attribute WHERE name=:fieldName and value=:fieldValue OFFSET :offset LIMIT :limit",nativeQuery = true)
    List<UserAttributeModel> findUserByAttribute(@Param("fieldName") String fieldName, @Param("fieldValue") String fieldValue,
                                                 @Param("offset") int offset, @Param("limit") int limit);
}
