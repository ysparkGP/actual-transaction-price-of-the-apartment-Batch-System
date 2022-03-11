package com.yusung.housebatch.job.apt;

import com.yusung.housebatch.core.Repository.LawdRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

/*
    ExecutionContext 에 저장할 데이터
    1. guLawdCdList - 구 코드 리스트
    2. guLawdCd - 구 코드 -> 다음 스텝에서 활용할 값
    3. itemCount - 남아있는 구 코드의 갯수
*/

@RequiredArgsConstructor
public class GuLawdTasklet implements Tasklet {

    private final LawdRepository lawdRepository;
    private List<String> guLawdCdList;
    private int itemCount;

    private static final String KEY_GU_LAWD_CD = "guLawdCd";
    private static final String KEY_ITEM_COUNT = "itemCount";
    private static final String KEY_GU_LAWD_LIST = "guLawdCdList";

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        ExecutionContext executionContext = getExecutionContext(chunkContext);

        initList(executionContext);
        initItemCount(executionContext);

        if (itemCount == 0) {
            contribution.setExitStatus(ExitStatus.COMPLETED);
            return RepeatStatus.FINISHED;
        }

        itemCount--;
        executionContext.putString(KEY_GU_LAWD_CD, guLawdCdList.get(itemCount));
        executionContext.putInt(KEY_ITEM_COUNT, itemCount);

        contribution.setExitStatus(new ExitStatus("CONTINUABLE"));
        return RepeatStatus.FINISHED;
    }

    private ExecutionContext getExecutionContext(ChunkContext chunkContext) {

//        chunkContext.getStepContext().getJobExecutionContext()  => 읽기 전용 jobContext

        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
        // step 에서 step 간의 데이터를 전달해주기 위해서 jobExecution 의 컨텍스트 사용
        return stepExecution.getJobExecution().getExecutionContext();

    }

    private void initList(ExecutionContext executionContext) {
        // 데이터가 있으면 다음 스텝을 실행하도록 하고, 데이터가 없으면 종료되도록 한다.
        // 데이터가 있으면 -> CONTINUABLE
        if (executionContext.containsKey(KEY_GU_LAWD_LIST)) {
            guLawdCdList = (List<String>) executionContext.get(KEY_GU_LAWD_LIST);
        } else {
            guLawdCdList = lawdRepository.findDistinctGuLawdCd();
            executionContext.put(KEY_GU_LAWD_LIST, guLawdCdList);
            executionContext.putInt(KEY_ITEM_COUNT, guLawdCdList.size());
        }
    }

    private void initItemCount(ExecutionContext executionContext) {
        if (executionContext.containsKey(KEY_ITEM_COUNT)) {
            itemCount = executionContext.getInt(KEY_ITEM_COUNT);
        } else {
            itemCount = guLawdCdList.size();
        }
    }
}
