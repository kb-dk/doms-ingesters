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

import java.net.URL;

/**
 * @author &lt;tsh@statsbiblioteket.dk&gt;
 */
public class DOMSLoginInfo {

    private final String login;
    private final String password;

    private final URL domsWSAPIUrl;

    public DOMSLoginInfo(URL domsWSAPIUrl, String login, String password) {
        this.login = login;
        this.password = password;
        this.domsWSAPIUrl = domsWSAPIUrl;
    }

    /**
     * @return the login
     */
    public String getLogin() {
        return login;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the domsWSAPIUrl
     */
    public URL getDomsWSAPIUrl() {
        return domsWSAPIUrl;
    }
}
