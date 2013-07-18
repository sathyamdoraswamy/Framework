package com.frameworkserver;

import java.util.ArrayList;

/**
 * This interface is used while creating a class whose object is passed to the setCallbackObject function in Framework.java
 * @author sathyam
 */
public interface Events {

    /**
     * The function which is invoked on reception of new records when callback is set
     * @param obj object formed from the set of records received and current records in the database
     */
    public void doJob(Object obj);
}
