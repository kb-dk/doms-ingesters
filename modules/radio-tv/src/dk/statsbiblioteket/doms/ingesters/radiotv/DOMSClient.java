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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import dk.statsbiblioteket.doms.centralWebservice.CentralWebservice;
import dk.statsbiblioteket.doms.centralWebservice.CentralWebserviceService;
import dk.statsbiblioteket.doms.centralWebservice.InvalidCredentialsException;
import dk.statsbiblioteket.doms.centralWebservice.MethodFailedException;

/**
 * @author tsh
 * 
 */
public class DOMSClient {

    private final CentralWebservice domsAPI = null;
        
    public void login(URL domsWSAPIEndpoint, String userName, String password) {
	final CentralWebservice domsAPI = new CentralWebserviceService(
		domsWSAPIEndpoint, new QName(
			"http://central.doms.statsbiblioteket.dk/",
			"CentralWebserviceService")).getCentralWebservicePort();

	Map<String, Object> domsAPILogin = ((BindingProvider) domsAPI)
		.getRequestContext();
	domsAPILogin.put(BindingProvider.USERNAME_PROPERTY, userName);
	domsAPILogin.put(BindingProvider.PASSWORD_PROPERTY, password);
    }
    
    /**
     * Create a new file object.
     * @param fileURL
     * @throws MethodFailedException
     * @throws InvalidCredentialsException
     */
    public String createFileObject(URL fileURL) throws MethodFailedException, InvalidCredentialsException {
	final String pid = domsAPI.newObject("doms:Template_RadioTVFile");

	//TODO: Add file URL to object;
	// brug domsAPI.addFileFromPermanentURL()
	
	// createFileObject() is a bad choice of name.
	
	return pid;
    }
    
    /**
     * Get an existing file object.
     * @param fileURL
     */
    public void getFileObject(URL fileURL) {
	
    }
}
