package org.kin.scheduler.core.task;

/**
 * @author huangjianqin
 * @date 2020-05-31
 */
public class PrintTaskTest extends TaskTest {
    public static void main(String[] args) throws InterruptedException {
        new PrintTaskTest().run();
    }

    @Override
    TaskDescription<?> generateTaskDescription() {
        return TaskDescription.createTmpTask("测试", TaskExecStrategy.COVER_EARLY, 0);
    }
}
