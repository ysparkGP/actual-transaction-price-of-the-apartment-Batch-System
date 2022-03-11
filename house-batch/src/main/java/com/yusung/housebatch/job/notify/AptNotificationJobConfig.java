package com.yusung.housebatch.job.notify;

import com.yusung.housebatch.adapter.FakeSendService;
import com.yusung.housebatch.core.Entity.Apt;
import com.yusung.housebatch.core.Entity.AptNotification;
import com.yusung.housebatch.core.Repository.AptNotificationRepository;
import com.yusung.housebatch.core.Repository.LawdRepository;
import com.yusung.housebatch.core.dto.AptDto;
import com.yusung.housebatch.core.dto.NotificationDto;
import com.yusung.housebatch.core.service.AptDealService;
import com.yusung.housebatch.job.validator.DealDateParameterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class AptNotificationJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job aptNotificationJob(Step aptNotificationStep){
        return jobBuilderFactory.get("aptNotificationJob")
                .incrementer(new RunIdIncrementer())
                .validator(new DealDateParameterValidator())
                .start(aptNotificationStep)
                .build();
    }

    @Bean
    @JobScope
    public Step aptNotificationStep(RepositoryItemReader<AptNotification> aptNotificationRepositoryItemReader,
                                    ItemProcessor<AptNotification, NotificationDto> aptNotificationProcessor,
                                    ItemWriter<NotificationDto> aptNotificationWriter){
        return stepBuilderFactory.get("aptNotificationStep")
                .<AptNotification, NotificationDto>chunk(10)
                .reader(aptNotificationRepositoryItemReader)
                .processor(aptNotificationProcessor)
                .writer(aptNotificationWriter)
                .build();

    }

    @Bean
    @StepScope
    public RepositoryItemReader<AptNotification> aptNotificationRepositoryItemReader(
            AptNotificationRepository aptNotificationRepository
    ){
        return new RepositoryItemReaderBuilder<AptNotification>()
                .name("aptNotificationRepositoryItemReader")
                .repository(aptNotificationRepository)
                .methodName("findByEnabledIsTrue")
                .pageSize(10)
                .arguments(Arrays.asList())
                .sorts(Collections.singletonMap("aptNotificationId",
                        Sort.Direction.DESC))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<AptNotification, NotificationDto> aptNotificationProcessor(
            @Value("#{jobParameters['dealDate']}") String dealDeate,
            AptDealService aptDealService,
            LawdRepository lawdRepository
    ){
        return aptNotification -> {

            List<AptDto> aptDtoList = aptDealService.findByGuLawdCdAndDealDate(aptNotification.getGuLawdCd(), LocalDate.parse(dealDeate));

            if(aptDtoList.isEmpty()){
                return null;
            }

            String guName = lawdRepository.findByLawdCd(aptNotification.getGuLawdCd()+"00000")
                    .orElseThrow().getLawdDong();

            return NotificationDto.builder()
                    .email(aptNotification.getEmail())
                    .guName(guName)
                    .count(aptDtoList.size())
                    .aptDeals(aptDtoList)
                    .build();
        };
    }

    @Bean
    @StepScope
    public ItemWriter<NotificationDto> aptNotificationWriter(FakeSendService fakeSendService){
        return items -> {
            items.forEach(item -> fakeSendService.send(item.getEmail(), item.toMessage()));
        };
    }
}
