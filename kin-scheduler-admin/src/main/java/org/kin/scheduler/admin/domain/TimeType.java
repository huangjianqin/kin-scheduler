package org.kin.scheduler.admin.domain;

import org.kin.framework.utils.TimeUtils;
import org.kin.scheduler.admin.core.TaskScheduleKeeper;

import java.text.ParseException;
import java.util.Date;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-03-08
 */
public enum TimeType {
    /**
     * 绝对时间
     */
    ABSOLUTE {
        @Override
        public long parseTime(String timeStr) throws Exception {
            return TimeUtils.parseDateTime(timeStr).getTime();
        }

        @Override
        public boolean validTimeFormat(String timeStr) {
            try {
                return Objects.nonNull(TimeUtils.getDatetimeFormat().parse(timeStr));
            } catch (ParseException e) {

            }
            return false;
        }
    },
    /**
     * cron表达式
     */
    CRON {
        @Override
        public long parseTime(String timeStr) throws Exception {
            return new CronExpression(timeStr).getNextValidTimeAfter(new Date(System.currentTimeMillis() + TaskScheduleKeeper.PRE_READ_MS)).getTime();
        }

        @Override
        public boolean validTimeFormat(String timeStr) {
            return CronExpression.isValidExpression(timeStr);
        }
    },
    ;

    public abstract long parseTime(String timeStr) throws Exception;

    public abstract boolean validTimeFormat(String timeStr);

    public static TimeType[] VALUES = values();

    public static TimeType getByName(String type) {
        for (TimeType timeType : VALUES) {
            if (timeType.name().equals(type)) {
                return timeType;
            }
        }

        throw new IllegalStateException("unknown TimeType: ".concat(type));
    }
}
