package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmployeeEmployeeNumber(String employeeNumber);
    Optional<User> findByEmployeeIdNumber(String idNumber);
    Optional<User> findByResetToken(String resetToken);
    Optional<User> findByEmployeeId(Long employeeId);
    List<User> findByRoleIn(java.util.Collection<String> roles);
    List<User> findByEmployeeFullNameContainingIgnoreCase(String query);

    @Query("SELECT u FROM User u WHERE u.employee IS NOT NULL AND u.id <> :excludeUserId " +
            "AND (:query IS NULL OR LOWER(u.employee.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:branchId IS NULL OR u.employee.branch.id = :branchId) " +
            "ORDER BY u.employee.fullName")
    List<User> searchRecipients(@Param("query") String query,
                                @Param("branchId") Long branchId,
                                @Param("excludeUserId") Long excludeUserId);
}
