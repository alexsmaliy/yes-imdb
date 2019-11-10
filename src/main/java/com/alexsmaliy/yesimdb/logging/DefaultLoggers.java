package com.alexsmaliy.yesimdb.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultLoggers {
    private DefaultLoggers() { /* constants class */ }
    public static final Logger REQUEST_ERROR_LOGGER = LoggerFactory.getLogger("request-error");
}
