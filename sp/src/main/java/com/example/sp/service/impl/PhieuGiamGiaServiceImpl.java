package com.example.sp.service.impl;

import com.example.sp.dto.promotion.PhieuGiamGiaRequest;
import com.example.sp.model.promotion.KhachHangPgg;
import com.example.sp.model.promotion.PhieuGiamGia;
import com.example.sp.repository.KhachHangPggRepository;
import com.example.sp.repository.PhieuGiamGiaRepository;
import com.example.sp.service.promotion.PhieuGiamGiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaServiceImpl implements PhieuGiamGiaService {

    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final KhachHangPggRepository khachHangPggRepository;

    @Override
    public Page<PhieuGiamGia> getAll(String keyword, String loaiGiam, Boolean trangThai, LocalDateTime tuNgay, LocalDateTime denNgay, Pageable pageable) {
        return phieuGiamGiaRepository.search(
                blankToNull(keyword),
                blankToNull(loaiGiam),
                trangThai,
                tuNgay,
                denNgay,
                withDefaultSort(pageable)
        );
    }

    @Override
    public PhieuGiamGia findById(Integer id) {
        return phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá"));
    }

    @Override
    @Transactional
    public PhieuGiamGia save(PhieuGiamGiaRequest request) {
        validate(request);

        PhieuGiamGia pgg = request.getId() == null ? new PhieuGiamGia() : findById(request.getId());
        Integer idCheck = request.getId() == null ? 0 : request.getId();

        String ma = blankToNull(request.getMaPgg());
        if (ma == null) {
            ma = generateCode("PGG");
        }
        if (phieuGiamGiaRepository.existsByMaPggAndIdNot(ma, idCheck)) {
            throw new RuntimeException("Mã phiếu giảm giá đã tồn tại");
        }

        pgg.setMaPgg(ma);
        pgg.setTenPgg(request.getTenPgg().trim());
        pgg.setLoaiGiam(request.getLoaiGiam());
        pgg.setGiaTri(request.getGiaTri());
        pgg.setGiaTriToiDa(request.getGiaTriToiDa());
        pgg.setDieuKienDonHang(request.getDieuKienDonHang());
        pgg.setNgayBatDau(request.getNgayBatDau());
        pgg.setNgayKetThuc(request.getNgayKetThuc());
        pgg.setSoLuong(request.getSoLuong());
        pgg.setSoLuongDaDung(request.getSoLuongDaDung() == null ? safeUsed(pgg) : request.getSoLuongDaDung());
        pgg.setTrangThai(request.getTrangThai() == null ? Boolean.TRUE : request.getTrangThai());

        PhieuGiamGia saved = phieuGiamGiaRepository.save(pgg);
        replaceKhachHangLinks(saved.getId(), request.getKhachHangIds());
        return saved;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        phieuGiamGiaRepository.softDeleteById(id);
    }

    @Override
    public List<Integer> getKhachHangIds(Integer idPgg) {
        try {
            return khachHangPggRepository.findIdKhByIdPgg(idPgg);
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    @Override
    public boolean validateVoucher(Integer idVoucher, Double tongTien) {
        if (idVoucher == null || tongTien == null) {
            return false;
        }
        PhieuGiamGia voucher = findById(idVoucher);
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(voucher.getTrangThai())) {
            return false;
        }
        if (voucher.getNgayBatDau() != null && now.isBefore(voucher.getNgayBatDau())) {
            return false;
        }
        if (voucher.getNgayKetThuc() != null && now.isAfter(voucher.getNgayKetThuc())) {
            return false;
        }
        if (voucher.getSoLuong() != null && voucher.getSoLuongDaDung() != null && voucher.getSoLuongDaDung() >= voucher.getSoLuong()) {
            return false;
        }
        return voucher.getDieuKienDonHang() == null || BigDecimal.valueOf(tongTien).compareTo(voucher.getDieuKienDonHang()) >= 0;
    }

    private void replaceKhachHangLinks(Integer idPgg, List<Integer> khachHangIds) {
        try {
            khachHangPggRepository.deleteByIdPgg(idPgg);
        } catch (DataAccessException ex) {
            if (khachHangIds == null || khachHangIds.isEmpty()) {
                return;
            }
            throw new RuntimeException("Bảng khach_hang_pgg chưa tồn tại nên chưa thể gán voucher cho khách hàng cụ thể.");
        }
        if (khachHangIds == null || khachHangIds.isEmpty()) {
            return;
        }
        try {
            khachHangIds.stream()
                    .distinct()
                    .forEach(idKh -> {
                        KhachHangPgg link = new KhachHangPgg();
                        link.setIdPgg(idPgg);
                        link.setIdKh(idKh);
                        khachHangPggRepository.save(link);
                    });
        } catch (DataAccessException ex) {
            throw new RuntimeException("Bảng khach_hang_pgg chưa tồn tại nên chưa thể gán voucher cho khách hàng cụ thể.");
        }
    }

    private Pageable withDefaultSort(Pageable pageable) {
        Sort sort = Sort.by(Sort.Order.desc("id"));
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged(sort);
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private void validate(PhieuGiamGiaRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu phiếu giảm giá không hợp lệ");
        }
        if (blankToNull(request.getTenPgg()) == null) {
            throw new RuntimeException("Tên phiếu giảm giá không được để trống");
        }
        if (blankToNull(request.getLoaiGiam()) == null) {
            throw new RuntimeException("Vui lòng chọn loại giảm");
        }
        if (request.getGiaTri() == null || request.getGiaTri().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá trị giảm phải lớn hơn 0");
        }
        if ("PHAN_TRAM".equals(request.getLoaiGiam()) && request.getGiaTri().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("Giá trị giảm phần trăm không được vượt quá 100");
        }
        if (request.getSoLuong() == null || request.getSoLuong() < 0) {
            throw new RuntimeException("Số lượng không hợp lệ");
        }
        if (request.getNgayBatDau() == null || request.getNgayKetThuc() == null) {
            throw new RuntimeException("Vui lòng nhập thời gian bắt đầu và kết thúc");
        }
        if (!request.getNgayKetThuc().isAfter(request.getNgayBatDau())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private Integer safeUsed(PhieuGiamGia pgg) {
        return pgg.getSoLuongDaDung() == null ? 0 : pgg.getSoLuongDaDung();
    }

    private String generateCode(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
