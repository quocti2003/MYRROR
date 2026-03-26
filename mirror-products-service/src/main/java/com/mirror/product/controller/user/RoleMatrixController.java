package com.mirror.product.controller.user;

import com.mirror.product.entity.user.RoleMatrix;
import com.mirror.product.repository.user.RoleMatrixRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/roles/matrix")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoleMatrixController {

    private final RoleMatrixRepository roleMatrixRepository;

    @GetMapping
    public ResponseEntity<List<RoleMatrix>> getRoleMatrix() {
        List<RoleMatrix> matrix = roleMatrixRepository.findAll();
        return ResponseEntity.ok(matrix);
    }
}
