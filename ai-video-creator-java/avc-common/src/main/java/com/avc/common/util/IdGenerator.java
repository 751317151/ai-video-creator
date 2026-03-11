package com.avc.common.util;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {}

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String jobId() {
        return "job_" + uuid();
    }
}
