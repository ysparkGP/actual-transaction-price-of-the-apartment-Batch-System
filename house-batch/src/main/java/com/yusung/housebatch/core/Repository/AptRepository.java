package com.yusung.housebatch.core.Repository;

import com.yusung.housebatch.core.Entity.Apt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AptRepository extends JpaRepository<Apt, Long> {

    Optional<Apt> findAptByAptNameAndJibun(String aptName, String jibun);
}
