package com.yusung.housebatch.job.apt;


import com.yusung.housebatch.adapter.ApartmentApiResource;
import com.yusung.housebatch.core.Repository.LawdRepository;
import com.yusung.housebatch.core.dto.AptDealDto;
import com.yusung.housebatch.core.service.AptDealService;
import com.yusung.housebatch.job.validator.FilePathParameterValidator;
import com.yusung.housebatch.job.validator.LawdCdParameterValidator;
import com.yusung.housebatch.job.validator.YearMonthParameterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.CompositeJobParametersValidator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AptDealInsertJobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private final ApartmentApiResource apartmentApiResource;


    @Bean
    public Job aptDealInsertJob(
            Step guLawdCdStep,
            Step aptDealInsertStep
    ) {
        return jobBuilderFactory.get("aptDealInsertJob")
                .incrementer(new RunIdIncrementer())
//                .validator(new FilePathParameterValidator())
                .validator(new YearMonthParameterValidator())
                .start(guLawdCdStep)
                // 데이터 여부에 따른 분기 처리
                .on("CONTINUABLE").to(aptDealInsertStep).next(guLawdCdStep)
                .from(guLawdCdStep).on("*").end()
                .end()
//                .next(aptDealInsertStep)
                .build();
    }

    // 구 코드와 년월을 파라미터로 입력받기 위해서 CompositeJobParametersValidator 를 사용하였지만
    // 이제는 DB 에서 구 코드를 꺼내오므로 사용할 필요가 없어짐
//    private JobParametersValidator aptDealJobParameterValidator() {
//        // Validator 여러개
//        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
//        validator.setValidators(Arrays.asList(
//                new YearMonthParameterValidator()
//                new LawdCdParameterValidator()
//        ));
//        return validator;
//    }

    @Bean
    @JobScope
    public Step guLawdCdStep(Tasklet guLawdCdTasklet) {
        return stepBuilderFactory.get("guLawdCdStep")
                .tasklet(guLawdCdTasklet)
                .build();
    }


    @Bean
    @StepScope
    public Tasklet guLawdCdTasklet(LawdRepository lawdRepository) {
        return new GuLawdTasklet(lawdRepository);
    }

//    @Bean
//    @JobScope
//    public Step contextPrintStep(Tasklet contextPrintTasklet) {
//        return stepBuilderFactory.get("contextPrintStep")
//                .tasklet(contextPrintTasklet)
//                .build();
//    }
//
//    @Bean
//    @StepScope
//    public Tasklet contextPrintTasklet(
//            @Value("#{jobExecutionContext['guLawdCd']}") String guLawdCd
//    ) {
//        return ((contribution, chunkContext) -> {
//            System.out.println("[contextPrintStep] guLawdCd = " + guLawdCd);
//            return RepeatStatus.FINISHED;
//        });
//    }

    @Bean
    @JobScope
    public Step aptDealInsertStep(StaxEventItemReader<AptDealDto> aptDealResourceReader,
                                  ItemWriter<AptDealDto> aptDealWriter) {
        return stepBuilderFactory.get("aptDealInsertStep")
                .<AptDealDto, AptDealDto>chunk(10)
                .reader(aptDealResourceReader)
                .writer(aptDealWriter)
                .build();
    }

    @Bean
    @StepScope
    public StaxEventItemReader<AptDealDto> aptDealResourceReader(
//            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{jobParameters['yearMonth']}") String yearMonth,
            @Value("#{jobExecutionContext['guLawdCd']}") String guLawdCd,
            Jaxb2Marshaller aptDealDtoMarshaller
    ) {
        return new StaxEventItemReaderBuilder<AptDealDto>()
                .name("aptDealResourceReader")
// XML 파일      .resource(new ClassPathResource(filePath))
                .resource(apartmentApiResource.getResource(guLawdCd, YearMonth.parse(yearMonth)))
                .addFragmentRootElements("item")
                // XML 파일을 객체에 매핑
                .unmarshaller(aptDealDtoMarshaller)
                .build();
    }

    /*
    Unmarshalling: XML Schema 를 읽어서 자바 오브젝트로 만드는 일
    Marshalling: 반대로 자바 오브젝트를 XML 로 변환하는 일
    JAXB: 언마샬링과 마샬링을 지원해주는 Java API
     */

    @Bean
    @StepScope
    public Jaxb2Marshaller aptDealDtoMarshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(AptDealDto.class);

        return jaxb2Marshaller;
    }

    @Bean
    @StepScope
    public ItemWriter<AptDealDto> aptDealWriter(AptDealService aptDealService) {
        return items -> {
            items.forEach(aptDealService::upsert);
            System.out.println("============= Writing Completed ==============");
        };
    }
}
