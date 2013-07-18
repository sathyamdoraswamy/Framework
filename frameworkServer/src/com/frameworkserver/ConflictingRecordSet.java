package com.frameworkserver;

import java.util.ArrayList;

/**
 * This class is used as a return type in functions
 * @author sathyam
 */
public class ConflictingRecordSet {

    /**
     * an array of type 'ServerRecord' indicating the current set of records
     */
    public ServerRecord[] currentRecords;
    /**
     * an arraylist of an array of records of type 'ServerRecord' conflicting with the current set of records
     */
    public ArrayList<ServerRecord[]> conflictingRecordsSet;
}
