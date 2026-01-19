package com.tasteam.domain.restaurant.entity;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.tasteam.domain.common.BaseTimeEntity;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "restaurant_address")
@Comment("음식점의 주소 정보를 관리하는 테이블")
public class RestaurantAddress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "sido", length = 20)
    @Comment("시/도")
    private String sido;

    @Column(name = "sigungu", length = 30)
    @Comment("시/군/구")
    private String sigungu;

    @Column(name = "eupmyeondong", length = 30)
    @Comment("읍/면/동")
    private String eupmyeondong;

    @Column(name = "postal_code", length = 16)
    @Comment("우편번호")
    private String postalCode;
}
