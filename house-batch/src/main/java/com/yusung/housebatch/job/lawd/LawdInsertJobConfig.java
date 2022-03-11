package com.yusung.housebatch.job.lawd;


import com.yusung.housebatch.core.Entity.Lawd;
import com.yusung.housebatch.core.service.LawdService;
import com.yusung.housebatch.job.validator.FilePathParameterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import static com.yusung.housebatch.job.lawd.LawdFieldSetMapper.*;

/*
동코드 마이그레이션 배치작업
 */

@Configuration
@RequiredArgsConstructor
@Slf4j
public class LawdInsertJobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final LawdService lawdService;

    @Bean
    public Job lawdInsertJob(Step lawdInsertStep){
        return jobBuilderFactory.get("lawdInsertJob")
                .incrementer(new RunIdIncrementer())
                .validator(new FilePathParameterValidator())
                .start(lawdInsertStep)
                .build();
    }

    @JobScope
    @Bean
    public Step lawdInsertStep(FlatFileItemReader<Lawd> lawdFileItemReader,
                               ItemWriter<Lawd> lawdItemWriter){
        return stepBuilderFactory.get("lawdInsertStep")
                .<Lawd, Lawd>chunk(1000)
                .reader(lawdFileItemReader)
                .writer(lawdItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Lawd> lawdFileItemReader(@Value("#{jobParameters['filePath']}") String filePath){
        return new FlatFileItemReaderBuilder<Lawd>()
                .name("lawdFileItemReader")
                .delimited()
                .delimiter("\t")
                // fieldSetMapper 와 함께 파일 각각의 필드를 엔티티에 매핑
                .names(LAWD_CD, LAWD_DONG, EXIST)
                .linesToSkip(1)
                .fieldSetMapper(new LawdFieldSetMapper())
                // ClassPathResource 는 resources 폴더에 있는 파일들에 대한 파일 이름, File 객체, URL,
                // URI 등 리소스와 관련된 정보를 제공
                .resource(new ClassPathResource(filePath))
                .build();
    }

    @Bean
    @StepScope
    public ItemWriter<Lawd> lawdItemWriter(){

        return items -> items.forEach(lawdService::upsert);
    }
}
