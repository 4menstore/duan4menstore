package com.example.sp.model.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dia_chi")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DiaChi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dc")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kh")
    private KhachHang khachHang;

    @Column(name = "ma_dia_chi")
    private String maDiaChi;

    @Column(name = "ten_dia_chi")
    private String tenDiaChi;

    @Column(name = "thanh_pho")
    private String thanhPho;

    @Column(name = "quan")
    private String quan;

    @Column(name = "phuong")
    private String phuong;

    @Column(name = "dia_chi_cu_the")
    private String diaChiCuThe;

    @Column(name = "mac_dinh")
    private Boolean macDinh;
}