package com.yusung.housebatch.job.lawd;

import com.yusung.housebatch.BatchTestConfig;
import com.yusung.housebatch.core.service.LawdService;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBatchTest
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = {LawdInsertJobConfig.class, BatchTestConfig.class})
public class LawdInsertJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @MockBean
    private LawdService lawdService;

    @Test
    public void success() throws Exception {
        // when
        JobParameters parameters = new JobParameters(Maps.newHashMap("filePath", new JobParameter("TEST_LAWD_CODE.txt")));
        JobExecution execution = jobLauncherTestUtils.launchJob(parameters);

        // then
        assertEquals(execution.getExitStatus(), ExitStatus.COMPLETED);
        // Mock 객체의 upsert 가 3번 호출되는 것을 확인
        verify(lawdService, times(3)).upsert(any());
    }

    @Test
    public void fail_whenFileNotFound() throws Exception {
        // when
        JobParameters parameters = new JobParameters(Maps.newHashMap("filePath", new JobParameter("NOT_EXIST.txt")));
        //JobExecution execution = jobLauncherTestUtils.launchJob(parameters);

        // then
        Assertions.assertThrows(JobParametersInvalidException.class, ()->jobLauncherTestUtils.launchJob(parameters));
        verify(lawdService, never()).upsert(any());
    }
}
