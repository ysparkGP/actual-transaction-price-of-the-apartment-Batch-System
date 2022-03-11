package com.yusung.housebatch.core.Entity;

import com.yusung.housebatch.core.dto.AptDealDto;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "apt")
@EntityListeners(AuditingEntityListener.class)
public class Apt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aptId;

    @Column(nullable = false)
    private String aptName;

    @Column(nullable = false)
    private String jibun;

    @Column(nullable = false)
    private String dong;

    @Column(nullable = false)
    private String guLawdCd;

    @Column(nullable = false)
    private Integer builtYear;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Apt from(AptDealDto dto){
        Apt apt = new Apt();

        apt.setAptName(dto.getAptName().trim());
        apt.setJibun(dto.getJibun().trim());
        apt.setDong(dto.getDong().trim());
        apt.setGuLawdCd(dto.getRegionalCode().trim());
        apt.setBuiltYear(dto.getBuiltYear());

        return apt;
    }

}
