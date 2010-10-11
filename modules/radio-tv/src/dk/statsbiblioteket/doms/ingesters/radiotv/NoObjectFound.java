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

/**
 * Exception indicating that the thrower could not find an object in the DOMS.
 * 
 * @author &lt;tsh@statsbiblioteket.dk&gt;
 */
public class NoObjectFound extends Exception {

    /**
     * Version UUID for serialisation.
     */
    private static final long serialVersionUID = -3639478367508534640L;

    /**
     * @see java.lang.Exception#Exception()
     */
    public NoObjectFound() {
        super();
    }

    /**
     * @see java.lang.Exception#Exception(String)
     */
    public NoObjectFound(String message) {
        super(message);
    }

    /**
     * @see java.lang.Exception#Exception(Throwable)
     */
    public NoObjectFound(Throwable cause) {
        super(cause);
    }

    /**
     * @see java.lang.Exception#Exception(String, Throwable)
     */
    public NoObjectFound(String message, Throwable cause) {
        super(message, cause);
    }
}
