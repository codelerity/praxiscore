/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2018 Neil C Smith.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 * 
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */

package org.praxislive.audio;


/**
 *
 * 
 */
public class ClientRegistrationException extends Exception {

    /**
     * Creates a new instance of <code>ClientRegistrationException</code> without detail message.
     */
    public ClientRegistrationException() {
    }


    /**
     * Constructs an instance of <code>ClientRegistrationException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ClientRegistrationException(String msg) {
        super(msg);
    }
    
    
    public ClientRegistrationException(Throwable cause) {
        super(cause);
    }
    
    public ClientRegistrationException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
}
