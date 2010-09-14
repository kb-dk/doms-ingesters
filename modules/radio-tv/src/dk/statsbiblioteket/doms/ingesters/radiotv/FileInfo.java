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

import java.net.URI;
import java.net.URL;

/**
 * @author tsh
 * 
 */
public class FileInfo {

    private final String fileName;
    private final URL fileLocation;
    private final String md5Sum;
    private final URI fileFormatURI;

    /**
     * Create a <code>FileInfo</code> instance based on a file name, file
     * location URL, a MD5 checksum hex string and a file format URI.
     * 
     * @param fileName
     *            The name of the file.
     * @param fileLocation
     *            The complete <code>URL</code> describing the location of the
     *            file data.
     * @param md5Sum
     *            The MD5 checksum of the file in the form of a hex string.
     * @param fileFormatURI
     *            A PRONOM <code>URI</code> which identifies the format of the
     *            file.
     */
    public FileInfo(String fileName, URL fileLocation, String md5Sum,
	    URI fileFormatURI) {
	this.fileName = fileName;
	this.fileLocation = fileLocation;
	this.md5Sum = md5Sum;
	this.fileFormatURI = fileFormatURI;
    }

    /**
     * Get the name of the file.
     * 
     * @return A string containing the file name.
     */
    public String getFileName() {
	return fileName;
    }

    /**
     * Get the <code>URL</code> location of the file data.
     * 
     * @return A <code>URL</code> of the file data.
     */
    public URL getFileLocation() {
	return fileLocation;
    }

    /**
     * Get a MD5 checksum hex string for the file described by this
     * <code>FileInfo</code> object.
     * 
     * @return The MD5 checksum of the file described by this
     *         <code>FileInfo</code> object.
     */
    public String getMd5Sum() {
	return md5Sum;
    }

    /**
     * Get the PRONOM format URI identifying the data format used in the file
     * described by this <code>FileInfo</code> object.
     * 
     * @return The file format <code>URI</code> for the file described by this
     *         <code>FileInfo</code>.
     */
    public URI getFileFormatURI() {
	return fileFormatURI;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
	return "FileInfo [fileFormatURI=" + fileFormatURI + ", fileLocation="
	        + fileLocation + ", fileName=" + fileName + ", md5Sum="
	        + md5Sum + "]";
    }
}
