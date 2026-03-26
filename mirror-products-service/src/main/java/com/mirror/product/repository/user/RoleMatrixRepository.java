package com.mirror.product.repository.user;

import com.mirror.product.entity.user.RoleMatrix;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleMatrixRepository extends JpaRepository<RoleMatrix, Long> {
    Optional<RoleMatrix> findByRoleName(String roleName);
}
