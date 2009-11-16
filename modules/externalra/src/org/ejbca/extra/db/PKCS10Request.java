/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.extra.db;

import org.apache.commons.lang.BooleanUtils;


/**
 * Ext RA Requset SubMessage when requesting using a PKCS10 and expecting a PKCS10 Response containing a certifcate. 
 * @author philip
 * $Id: ExtRAPKCS10Request.java,v 1.3 2007-05-15 12:57:59 anatom Exp $
 */

public class PKCS10Request extends ExtRARequest {

	public static final float LATEST_VERSION = (float) 2.0;
	
	static final int CLASS_TYPE = 2;
	
	// Field constants
	private static final String PKCS10              = "PKCS10";
	private static final String CREATEOREDITEUSER   = "CREATEOREDITEUSER";
	
	
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Default constructor that should be used.
	 * @param pkcs10 the PKCS10 request
	 */
	public PKCS10Request(long requestId, String username, String subjectDN, String subjectAltName, 
            String email, String subjectDirectoryAttributes,
            String endEntityProfileName, String certificateProfileName,
            String cAName, String pkcs10){
        super(requestId, username, subjectDN, subjectAltName, email, subjectDirectoryAttributes, endEntityProfileName, certificateProfileName,cAName);
		data.put(CLASSTYPE, Integer.valueOf(CLASS_TYPE));
		data.put(VERSION, Float.valueOf(LATEST_VERSION));
		data.put(PKCS10, pkcs10);
		data.put(CREATEOREDITEUSER, "true");
	}

	/**
	 * Constructor used when loaded from a persisted state
	 */	
	public PKCS10Request(){}
	

	public float getLatestVersion() {
		return LATEST_VERSION;
	}
	
	/**
	 * Returns the PKCS10 used in this request.
	 * @return String with base64 encoded pkcs10 request
	 */
	public String getPKCS10(){
		return (String) data.get(PKCS10);
	}
	/**
	 * Returns the CREATEUSER used in this request, and converts it to boolean
	 * @return true or false
	 */
	public boolean createOrEditUser(){
		String ret = (String)data.get(CREATEOREDITEUSER);
		return BooleanUtils.toBoolean(ret);
	}
	/**
	 * Sets the CREATEUSER used in this request
	 * @return true or false
	 */
	public void setCreateOrEditUser(boolean createUser){
		String create = BooleanUtils.toStringTrueFalse(createUser);
		data.put(CREATEOREDITEUSER, create);
	}
	
	public void upgrade() {
		if(LATEST_VERSION != getVersion()){						
			data.put(VERSION, new Float(LATEST_VERSION));
		}
		
	}

}
