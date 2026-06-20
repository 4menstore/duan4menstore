package com.example.sp.repository;

import com.example.sp.model.employee.NhanVien;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {

    @Query("""
        SELECT n FROM NhanVien n
        WHERE (:key IS NULL
               OR n.hoTen LIKE CONCAT('%', :key, '%')
               OR n.soDienThoai LIKE CONCAT('%', :key, '%')
               OR n.email LIKE CONCAT('%', :key, '%'))
          AND (:vaiTro IS NULL OR n.vaiTro = :vaiTro)
          AND (:trangThai IS NULL OR n.trangThai = :trangThai)
    """)
    Page<NhanVien> search(
            @Param("key") String key,
            @Param("vaiTro") String vaiTro,
            @Param("trangThai") Boolean trangThai,
            Pageable pageable
    );

    boolean existsByMaNv(String maNv);

    boolean existsByEmail(String email);

    boolean existsByCccd(String cccd);

    boolean existsByMaNvAndIdNot(String maNv, Integer id);

    boolean existsByEmailAndIdNot(String email, Integer id);

    boolean existsByCccdAndIdNot(String cccd, Integer id);

    Optional<NhanVien> findByEmail(String email);
}
