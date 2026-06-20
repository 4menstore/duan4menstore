package com.example.sp.service.impl;

import com.example.sp.model.employee.NhanVien;
import com.example.sp.repository.NhanVienRepository;
import com.example.sp.service.employee.NhanVienService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NhanVienServiceImpl implements NhanVienService {

    private static final String PHONE_PATTERN = "^(03|05|07|08|09)\\d{8}$";

    private final NhanVienRepository nhanVienRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Override
    public Page<NhanVien> getAll(String keyword, String vaiTro, Boolean trangThai, Pageable pageable) {
        String key = isBlank(keyword) ? null : keyword.trim();
        String role = isBlank(vaiTro) ? null : vaiTro.trim();
        Pageable effectivePageable = withDefaultSort(pageable);
        return nhanVienRepository.search(key, role, trangThai, effectivePageable);
    }

    @Override
    public NhanVien findById(Integer id) {
        return nhanVienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
    }

    @Override
    @Transactional
    public NhanVien save(NhanVien nv) {
        validate(nv);

        if (nv.getId() == null) {
            String rawPassword = isBlank(nv.getMatKhau()) ? generateRandomPassword() : nv.getMatKhau();
            if (isBlank(nv.getMaNv())) {
                nv.setMaNv(generateCode("NV"));
            }
            nv.setMatKhau(passwordEncoder.encode(rawPassword));
            if (isBlank(nv.getVaiTro())) {
                nv.setVaiTro("Nhân viên");
            }
            if (nv.getTrangThai() == null) {
                nv.setTrangThai(true);
            }
            NhanVien saved = nhanVienRepository.save(nv);
            sendAccountEmail(saved, rawPassword);
            return saved;
        }

        NhanVien old = findById(nv.getId());
        old.setMaNv(isBlank(nv.getMaNv()) ? old.getMaNv() : nv.getMaNv().trim());
        old.setHoTen(nv.getHoTen());
        old.setSoDienThoai(nv.getSoDienThoai());
        old.setEmail(nv.getEmail());
        old.setCccd(nv.getCccd());
        old.setGioiTinh(nv.getGioiTinh());
        old.setNgaySinh(nv.getNgaySinh());
        old.setDiaChi(nv.getDiaChi());
        old.setVaiTro(nv.getVaiTro());
        old.setNgayVaoLam(nv.getNgayVaoLam());
        old.setTrangThai(nv.getTrangThai() == null ? old.getTrangThai() : nv.getTrangThai());
        if (!isBlank(nv.getMatKhau())) {
            old.setMatKhau(passwordEncoder.encode(nv.getMatKhau()));
        }
        return nhanVienRepository.save(old);
    }

    @Override
    public void delete(Integer id) {
        nhanVienRepository.deleteById(id);
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

    private void validate(NhanVien nv) {
        if (isBlank(nv.getHoTen())) {
            throw new RuntimeException("Họ tên không được để trống");
        }
        if (isBlank(nv.getEmail())) {
            throw new RuntimeException("Email không được để trống");
        }

        Integer id = nv.getId() == null ? 0 : nv.getId();

        if (!isBlank(nv.getMaNv())) {
            nv.setMaNv(nv.getMaNv().trim());
            if (nhanVienRepository.existsByMaNvAndIdNot(nv.getMaNv(), id)) {
                throw new RuntimeException("Mã nhân viên đã tồn tại");
            }
        }

        nv.setEmail(nv.getEmail().trim());
        if (!nv.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RuntimeException("Email không hợp lệ");
        }
        if (nhanVienRepository.existsByEmailAndIdNot(nv.getEmail(), id)) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        if (!isBlank(nv.getSoDienThoai())) {
            nv.setSoDienThoai(nv.getSoDienThoai().trim());
            if (!nv.getSoDienThoai().matches(PHONE_PATTERN)) {
                throw new RuntimeException("Số điện thoại không hợp lệ");
            }
        }

        if (!isBlank(nv.getCccd())) {
            nv.setCccd(nv.getCccd().trim());
            if (!nv.getCccd().matches("^\\d{12}$")) {
                throw new RuntimeException("CCCD phải có đúng 12 chữ số");
            }
            if (nhanVienRepository.existsByCccdAndIdNot(nv.getCccd(), id)) {
                throw new RuntimeException("CCCD đã tồn tại");
            }
        }
    }

    private String generateCode(String prefix) {
        return prefix + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String generateRandomPassword() {
        int password = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(password);
    }

    private void sendAccountEmail(NhanVien nhanVien, String rawPassword) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || isBlank(mailUsername)) {
            throw new RuntimeException("Chưa cấu hình SMTP. Vui lòng đặt MAIL_USERNAME và MAIL_PASSWORD để gửi email nhân viên.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(nhanVien.getEmail());
        message.setSubject("Thông tin tài khoản nhân viên - 4Men Store");
        message.setText("""
                Chào %s,

                Tài khoản nhân viên của bạn đã được tạo thành công.

                Email đăng nhập: %s
                Mật khẩu: %s

                Vui lòng đăng nhập và đổi mật khẩu sau lần đăng nhập đầu tiên.

                Trân trọng,
                4Men Store
                """.formatted(nhanVien.getHoTen(), nhanVien.getEmail(), rawPassword));

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể gửi email nhân viên: " + ex.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
