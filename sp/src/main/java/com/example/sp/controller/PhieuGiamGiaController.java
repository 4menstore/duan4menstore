package com.example.sp.controller;

import com.example.sp.dto.promotion.PhieuGiamGiaRequest;
import com.example.sp.model.customer.KhachHang;
import com.example.sp.model.promotion.PhieuGiamGia;
import com.example.sp.repository.KhachHangRepository;
import com.example.sp.service.promotion.PhieuGiamGiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/phieu-giam-gia")
@RequiredArgsConstructor
public class PhieuGiamGiaController {

    private final PhieuGiamGiaService phieuGiamGiaService;
    private final KhachHangRepository khachHangRepository;

    @GetMapping
    public Page<PhieuGiamGia> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loaiGiam,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) LocalDateTime tuNgay,
            @RequestParam(required = false) LocalDateTime denNgay,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return phieuGiamGiaService.getAll(keyword, loaiGiam, trangThai, tuNgay, denNgay, pageable);
    }

    @GetMapping("/{id}")
    public PhieuGiamGia getById(@PathVariable Integer id) {
        return phieuGiamGiaService.findById(id);
    }

    @PostMapping
    public PhieuGiamGia create(@RequestBody PhieuGiamGiaRequest request) {
        request.setId(null);
        return phieuGiamGiaService.save(request);
    }

    @PutMapping("/{id}")
    public PhieuGiamGia update(@PathVariable Integer id, @RequestBody PhieuGiamGiaRequest request) {
        request.setId(id);
        return phieuGiamGiaService.save(request);
    }

    @PatchMapping("/{id}/trang-thai")
    public PhieuGiamGia toggleStatus(@PathVariable Integer id) {
        PhieuGiamGia voucher = phieuGiamGiaService.findById(id);
        PhieuGiamGiaRequest request = toRequest(voucher);
        request.setTrangThai(!Boolean.TRUE.equals(voucher.getTrangThai()));
        request.setKhachHangIds(phieuGiamGiaService.getKhachHangIds(id));
        return phieuGiamGiaService.save(request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        phieuGiamGiaService.delete(id);
    }

    @GetMapping("/{id}/khach-hang-ids")
    public List<Integer> getKhachHangIds(@PathVariable Integer id) {
        return phieuGiamGiaService.getKhachHangIds(id);
    }

    @GetMapping("/khach-hang")
    public List<KhachHang> getKhachHang() {
        return khachHangRepository.findAll();
    }

    private PhieuGiamGiaRequest toRequest(PhieuGiamGia voucher) {
        PhieuGiamGiaRequest request = new PhieuGiamGiaRequest();
        request.setId(voucher.getId());
        request.setMaPgg(voucher.getMaPgg());
        request.setTenPgg(voucher.getTenPgg());
        request.setLoaiGiam(voucher.getLoaiGiam());
        request.setGiaTri(voucher.getGiaTri());
        request.setGiaTriToiDa(voucher.getGiaTriToiDa());
        request.setDieuKienDonHang(voucher.getDieuKienDonHang());
        request.setNgayBatDau(voucher.getNgayBatDau());
        request.setNgayKetThuc(voucher.getNgayKetThuc());
        request.setSoLuong(voucher.getSoLuong());
        request.setSoLuongDaDung(voucher.getSoLuongDaDung());
        request.setTrangThai(voucher.getTrangThai());
        return request;
    }
}
