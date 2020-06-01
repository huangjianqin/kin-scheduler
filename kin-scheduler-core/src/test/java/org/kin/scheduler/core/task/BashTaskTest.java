package org.kin.scheduler.core.task;

import org.kin.scheduler.core.task.handler.domain.GlueType;
import org.kin.scheduler.core.task.handler.params.GlueParam;

/**
 * @author huangjianqin
 * @date 2020-05-31
 */
public class BashTaskTest extends TaskTest {
    public static void main(String[] args) throws InterruptedException {
        new BashTaskTest().run();
    }

    @Override
    TaskDescription<?> generateTaskDescription() {
        GlueParam glueParam = new GlueParam();
        glueParam.setType(GlueType.BASH.name());
        glueParam.setCommand("ls -la");
        return TaskDescription.createTmpTask(glueParam, TaskExecStrategy.COVER_EARLY, 0);
    }
}
