package com.hackathon.domain.shipper.entity;

import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipper extends BaseTimeEntity {

    @Id
    private Long id;

    private String companyName;
    private String contactName;
    private String phoneNumber;
    private String businessNumber;
    private String address;

    public static Shipper create(Long id) {
        Shipper shipper = new Shipper();
        shipper.id = id;
        return shipper;
    }

    public void update(String companyName, String contactName, String phoneNumber,
                       String businessNumber, String address) {
        this.companyName = companyName;
        this.contactName = contactName;
        this.phoneNumber = phoneNumber;
        this.businessNumber = businessNumber;
        this.address = address;
    }
}
