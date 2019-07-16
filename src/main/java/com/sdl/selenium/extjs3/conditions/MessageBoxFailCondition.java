package com.sdl.selenium.extjs3.conditions;

import com.google.common.base.Strings;
import com.sdl.selenium.conditions.FailCondition;
import com.sdl.selenium.conditions.MessageBoxCondition;
import com.sdl.selenium.extjs3.window.MessageBox;

public class MessageBoxFailCondition extends FailCondition implements MessageBoxCondition {

    private boolean contains = false;

    public MessageBoxFailCondition(String message) {
        super(message);
    }

    public MessageBoxFailCondition(String message, String className) {
        super(message, className);
    }

    public MessageBoxFailCondition(String message, int priority) {
        this(message);
        setPriority(priority);
    }

    public MessageBoxFailCondition(String message, int priority, String className) {
        this(message, className);
        setPriority(priority);
    }

    public MessageBoxFailCondition(String message, boolean contains) {
        this(message);
        this.contains = contains;
    }

    public MessageBoxFailCondition(String message, boolean contains, String className) {
        this(message, className);
        this.contains = contains;
    }

    public MessageBoxFailCondition(String message, boolean contains, int priority) {
        this(message, contains);
        setPriority(priority);
    }

    public MessageBoxFailCondition(String message, boolean contains, int priority, String className) {
        this(message, contains, className);
        setPriority(priority);
    }

    @Override
    public boolean execute() {
        return execute(MessageBox.getMessage());
    }


    @Override
    public boolean execute(final String boxMessage) {
        boolean executed = false;
        if (contains) {
            if (boxMessage != null && boxMessage.contains(getMessage())) {
                executed = true;
            }
        } else if (getMessage().equals(boxMessage)) {
            executed = true;
        }
        if (executed) {
            this.setResultMessage(boxMessage);
        }
        return executed;
    }

    public String toString() {
        String msg = getClassName();
        if (Strings.isNullOrEmpty(msg)) {
            msg = "MessageBoxFailCondition@" + getMessage();
        } else {
            msg += ":MessageBoxFailCondition@" + getMessage();
        }
        return msg;
    }
}