package com.example.sp.service.impl;

import com.example.sp.model.customer.KhachHang;
import com.example.sp.repository.KhachHangRepository;
import com.example.sp.service.customer.KhachHangService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KhachHangServiceImpl implements KhachHangService {

    private static final String PHONE_PATTERN = "^(03|05|07|08|09)\\d{8}$";

    private final KhachHangRepository khachHangRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Page<KhachHang> getAll(String keyword, Boolean trangThai, Pageable pageable) {
        String key = isBlank(keyword) ? null : keyword.trim();
        Pageable effectivePageable = withDefaultSort(pageable);
        return khachHangRepository.search(key, trangThai, effectivePageable);
    }

    @Override
    public KhachHang findById(Integer id) {
        return khachHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
    }

    @Override
    @Transactional
    public KhachHang save(KhachHang kh) {
        validate(kh);

        if (kh.getId() == null) {
            if (isBlank(kh.getMaKh())) {
                kh.setMaKh(generateCode("KH"));
            }
            if (kh.getTrangThai() == null) {
                kh.setTrangThai(true);
            }
            if (!isBlank(kh.getMatKhau()) && !isBCryptHash(kh.getMatKhau())) {
                kh.setMatKhau(passwordEncoder.encode(kh.getMatKhau()));
            }
            return khachHangRepository.save(kh);
        }

        KhachHang old = findById(kh.getId());
        old.setMaKh(isBlank(kh.getMaKh()) ? old.getMaKh() : kh.getMaKh().trim());
        old.setTenKhachHang(kh.getTenKhachHang());
        old.setTenTaiKhoan(kh.getTenTaiKhoan());
        old.setSoDienThoai(kh.getSoDienThoai());
        old.setEmail(kh.getEmail());
        old.setDiaChi(kh.getDiaChi());
        old.setGioiTinh(kh.getGioiTinh());
        old.setNgaySinh(kh.getNgaySinh());
        if (!isBlank(kh.getMatKhau())) {
            old.setMatKhau(isBCryptHash(kh.getMatKhau()) ? kh.getMatKhau() : passwordEncoder.encode(kh.getMatKhau()));
        }
        old.setTrangThai(kh.getTrangThai() == null ? old.getTrangThai() : kh.getTrangThai());
        return khachHangRepository.save(old);
    }

    @Override
    public void delete(Integer id) {
        khachHangRepository.deleteById(id);
    }

    private Pageable withDefaultSort(Pageable pageable) {
        Sort sort = Sort.by(Sort.Order.desc("trangThai"), Sort.Order.desc("id"));
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged(sort);
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private void validate(KhachHang kh) {
        if (isBlank(kh.getTenKhachHang())) {
            throw new RuntimeException("Tên khách hàng không được để trống");
        }

        Integer id = kh.getId() == null ? 0 : kh.getId();

        if (!isBlank(kh.getMaKh())) {
            kh.setMaKh(kh.getMaKh().trim());
            if (khachHangRepository.existsByMaKhAndIdNot(kh.getMaKh(), id)) {
                throw new RuntimeException("Mã khách hàng đã tồn tại");
            }
        }

        if (!isBlank(kh.getSoDienThoai())) {
            kh.setSoDienThoai(kh.getSoDienThoai().trim());
            if (!kh.getSoDienThoai().matches(PHONE_PATTERN)) {
                throw new RuntimeException("Số điện thoại không hợp lệ");
            }
            if (khachHangRepository.existsBySoDienThoaiAndIdNot(kh.getSoDienThoai(), id)) {
                throw new RuntimeException("Số điện thoại đã tồn tại");
            }
        }

        if (!isBlank(kh.getEmail())) {
            kh.setEmail(kh.getEmail().trim());
            if (!kh.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new RuntimeException("Email không hợp lệ");
            }
            if (khachHangRepository.existsByEmailAndIdNot(kh.getEmail(), id)) {
                throw new RuntimeException("Email đã được sử dụng");
            }
        }

        if (!isBlank(kh.getTenTaiKhoan())) {
            kh.setTenTaiKhoan(kh.getTenTaiKhoan().trim());
            if (khachHangRepository.existsByTenTaiKhoanIgnoreCaseAndIdNot(kh.getTenTaiKhoan(), id)) {
                throw new RuntimeException("Tên tài khoản đã được sử dụng");
            }
        }
    }

    private String generateCode(String prefix) {
        return prefix + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isBCryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
