/*
 * $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007-2010  The State and University Library
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package dk.statsbiblioteket.doms.ingesters.radiotv;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The <code>{@link HotFolderScanner}</code> provides continous scanning and
 * reporting of file creations, modifications and deletions in a hot folder.
 * 
 * @author &lt;tsh@statsbiblioteket.dk&gt;
 */
public class HotFolderScanner {

    /**
     * The <code>Timer</code> which invokes the scanning at regular intervals.
     */
    private final Timer scannerDaemon;

    /**
     * The initial delay before the first scanning, in milliseconds.
     */
    private long scannerDelay;

    /**
     * The delay between each subsequent scanning, in milliseconds.
     */
    private long scannerPeriod;

    /**
     * Create a hot folder scanner instance which by default scans a specified
     * folder every 5 seconds. This interval can be changed by calling
     * {@link #setInitialScannerDelay(long)} and {@link #setScannerPeriod(long)}
     * .
     * 
     * @see #setInitialScannerDelay(long)
     * @see #setScannerPeriod(long)
     */
    public HotFolderScanner() {
        scannerDaemon = new Timer(true);
        scannerDelay = 5000;
        scannerPeriod = 5000;
        System.out.println("HotFolderScanner has been created");
    }

    /**
     * Set the initial delay before the first execution of the hot folder
     * scanner.
     * 
     * @param delayMillis
     *            Delay in milliseconds.
     */
    public void setInitialScannerDelay(long delayMillis) {
        scannerDelay = delayMillis;
    }

    /**
     * Set the delay before each subsequent execution of the hot folder scanner.
     * 
     * @param periodMillis
     *            Delay in milliseconds.
     */
    public void setScannerPeriod(long periodMillis) {
        scannerPeriod = periodMillis;
    }

    /**
     * Start a continuous scanning of the hot folder specified by
     * <code>hotFolderToScan</code> and report any file creations, modifications
     * and deletions to the <code>client</code>.
     * 
     * @param hotFolderToScan
     *            Full file path to the directory to scan.
     * @param client
     *            Reference to the client to report changes to.
     * @param stopFolder
     *            Full file path to the stop directory.
     */
    public void startScanning(File hotFolderToScan, File stopFolder,
            HotFolderScannerClient client) {
        final Calendar rightNow = Calendar.getInstance();
        final DateFormat dateFormat = DateFormat
                .getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
        System.out.println("HotFolderScanner has started scanning at "
                + dateFormat.format(rightNow.getTime()));
        // TODO: We could add a smart feature to let users choose between
        // different inspector types, however, that is not important right now.
        TimerTask scannerTask = new NonRecursiveHotFolderInspector(
                hotFolderToScan, stopFolder, client);
        scannerDaemon.scheduleAtFixedRate(scannerTask, scannerDelay,
                scannerPeriod);
    }
}
