/**
 *  Copyright 2008 ThimbleWare Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.robin.im.netty.protocol;

/**
 * The payload object holding the parsed message.
 */
public final class CommandMessage {



    public int type;  //0=heartbeat,1=message
    public String message;

    private CommandMessage(int type) {
        this.type = type;
    }

    public static CommandMessage command(int type) {
        return new CommandMessage(type);
    }
    
    public boolean isHeartBeat(){
    	return type==0;
    }
}