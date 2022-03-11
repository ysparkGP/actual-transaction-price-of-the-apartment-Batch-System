package com.yusung.housebatch.job.notify;


import com.yusung.housebatch.BatchTestConfig;
import com.yusung.housebatch.adapter.FakeSendService;
import com.yusung.housebatch.core.Entity.AptNotification;
import com.yusung.housebatch.core.Entity.Lawd;
import com.yusung.housebatch.core.Repository.AptNotificationRepository;
import com.yusung.housebatch.core.Repository.LawdRepository;
import com.yusung.housebatch.core.dto.AptDto;
import com.yusung.housebatch.core.service.AptDealService;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.*;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {AptNotificationJobConfig.class, BatchTestConfig.class})
@ExtendWith(SpringExtension.class)
public class AptNotificationJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    // H2 DB 를 이용 (Paging 쿼리는 좀 까다로워서)
    @Autowired
    private AptNotificationRepository aptNotificationRepository;

    @MockBean
    private AptDealService aptDealService;

    @MockBean
    private FakeSendService fakeSendService;

    @MockBean
    private LawdRepository lawdRepository;

    // H2 DB 데이터 제거
    @AfterEach
    public void tearDown(){
        aptNotificationRepository.deleteAll();
    }

    @Test
    public void success() throws Exception{
        // given
        LocalDate dealDate = LocalDate.now().minusDays(1);
        String guLawdCd = "11110";
        String email = "test@naver.com";
        String anotherEmail = "stet@naver.com";

        givenAptNotification(email, guLawdCd,true);
        givenAptNotification(anotherEmail,guLawdCd,false);
        givenLawdCd(guLawdCd);
        givenAptDeal(guLawdCd, dealDate);

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(
                new JobParameters(Maps.newHashMap("dealDate", new JobParameter(dealDate.toString())))
        );

        // then
        Assertions.assertEquals(jobExecution.getExitStatus(), ExitStatus.COMPLETED);
        verify(fakeSendService, times(1)).send(eq(email), anyString());
        verify(fakeSendService, never()).send(eq(anotherEmail),anyString());

    }

    private void givenAptNotification(String email, String guLawdCd, boolean enabled){
        AptNotification notification = new AptNotification();
        notification.setEmail(email);
        notification.setGuLawdCd(guLawdCd);
        notification.setEnabled(enabled);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());

        aptNotificationRepository.save(notification);
    }

    private void givenLawdCd(String guLawdCd){
        String lawdCd = guLawdCd + "00000";
        Lawd lawd = new Lawd();
        lawd.setLawdCd(lawdCd);
        lawd.setLawdDong("경기도 성남시 분당구");
        lawd.setExist(true);
        lawd.setCreatedAt(LocalDateTime.now());
        lawd.setUpdatedAt(LocalDateTime.now());
        when(lawdRepository.findByLawdCd(lawdCd))
                .thenReturn(Optional.of(lawd));
    }

    private void givenAptDeal(String guLawdCd, LocalDate dealDate) {
        when(aptDealService.findByGuLawdCdAndDealDate(guLawdCd, dealDate))
                .thenReturn(Arrays.asList(
                        new AptDto("Spring 아파트", 200000000L),
                        new AptDto("Java 아파트",150000000L)
                ));
    }
}
