package com.example.sp.repository;

import com.example.sp.dto.promotion.SanPhamChiTietPromotionView;
import com.example.sp.model.promotion.DotGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, Integer> {

    @Query("""
        SELECT d FROM DotGiamGia d
        WHERE (:keyword IS NULL OR d.maDotGiamGia LIKE CONCAT('%', :keyword, '%') OR d.tenDotGiamGia LIKE CONCAT('%', :keyword, '%'))
          AND (:trangThai IS NULL OR d.trangThai = :trangThai)
          AND (:tuNgay IS NULL OR d.ngayBatDau >= :tuNgay)
          AND (:denNgay IS NULL OR d.ngayKetThuc <= :denNgay)
    """)
    Page<DotGiamGia> search(
            @Param("keyword") String keyword,
            @Param("trangThai") Boolean trangThai,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay,
            Pageable pageable
    );

    boolean existsByMaDotGiamGiaAndIdNot(String maDotGiamGia, Integer id);

    @Query(value = """
        SELECT ct.id_spct AS idSpct,
               ct.ma_chi_tiet_san_pham AS maSpct,
               sp.ten_sp AS tenSp,
               ct.gia_ban AS giaBan,
               ct.so_luong_ton AS soLuongTon
        FROM chi_tiet_san_pham ct
        INNER JOIN san_pham sp ON ct.id_san_pham = sp.id_sp
        WHERE ct.trang_thai = 1
        ORDER BY sp.ten_sp, ct.ma_chi_tiet_san_pham
    """, nativeQuery = true)
    List<SanPhamChiTietPromotionView> findSanPhamChiTietKichHoat();
}
