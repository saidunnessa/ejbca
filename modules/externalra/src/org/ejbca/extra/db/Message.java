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

import java.security.PrivateKey;
import java.util.Collection;
import java.util.Date;



/**
 * Message generated by hbm2java
 */

public class Message  implements java.io.Serializable {


	private static final long serialVersionUID = 1L;
	// Constants
	public static final Integer STATUS_WAITING   = Integer.valueOf(1);
	public static final Integer STATUS_INPROCESS = Integer.valueOf(2);
	public static final Integer STATUS_PROCESSED = Integer.valueOf(3);

    // Fields    

	/** A unique id, messageId.hashCode+type.hashCode, set in constuctor */
	private String uniqueId;
	/** A unique message id. User defined for example username, scep transactionId or similar */
	private String messageid;
	/** Type of message one of MessageHome.MESSAGETYPE_XX */
	private Integer type;
	/** Status of processing. One of Message.STATUS_XX */
	private Integer status;
	/** The time the extra message was stored */
	private long createtime;
	/** When the extra message was modified */
	private long modifytime;
	/** The message itself, serialized */
	private String message;


    // Constructors

    /** default constructor */
    public Message() {
    }
    
    /** constructor with id */
    public Message(String messageid, Integer type) {
    	setUniqueId("" + messageid.hashCode() + type.hashCode()); 
        setMessageid(messageid);
        setType(type);
        long currenttime = new Date().getTime();
        setCreatetime(currenttime);
        setModifytime(currenttime);
        setStatus(Message.STATUS_WAITING);
    }
    
    public void update(SubMessages submessages, Integer status){
    	setSubMessages(submessages);
    	setStatus(status);
    	setModifytime(new Date().getTime());
    }

    

   
    // Property accessors

    public String getUniqueId() {
        return this.uniqueId;
    }
    
    private void setUniqueId(String uniqueId){
    	this.uniqueId = uniqueId;
    }
    
    public String getMessageid() {
        return this.messageid;
    }
    
    private void setMessageid(String messageid){
    	this.messageid = messageid;
    }
    
    public Integer getType() {
        return this.type;
    }
    
    private void setType(Integer type){
    	this.type = type;
    }
    
    public Integer getStatus() {
        return this.status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }

    public long getCreatetime() {
        return this.createtime;
    }
    
    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getModifytime() {
        return this.modifytime;
    }
    
    public void setModifytime(long modifytime) {
        this.modifytime = modifytime;
    }

    private String getMessage() {
        return this.message;
    }
    
    private void setMessage(String message) {
        this.message = message;
    }
   
    public boolean equals(Object other) {
        if (this == other) return true;
        if ( !(other instanceof Message) ) return false;

        String id = ((Message)other).getUniqueId();
        if ( (id != null) && (this.uniqueId != null) && id.equals(this.uniqueId) ) return true;
        if ( (id == null) && (this.uniqueId == null) ) return true;
        return false;
    }

    public int hashCode() {
        return this.uniqueId.hashCode();
    }
    

    /**
     * Method that retrieves the message field.
     * @return a byte[] array containing the message.
     */
    
    public SubMessages getSubMessages( PrivateKey userKey, Collection cACertChain, Collection crls){
       SubMessages retval = new SubMessages();
       retval.load(getMessage(),  userKey, cACertChain, crls);
       return retval;
    }
    
    /**
     * Method to set the message field, takes a bytearray as input
     * and stores it in database as base64 encoded string.
     */
    public void setSubMessages(SubMessages submessages){
       setMessage(submessages.save());    	
    }





}