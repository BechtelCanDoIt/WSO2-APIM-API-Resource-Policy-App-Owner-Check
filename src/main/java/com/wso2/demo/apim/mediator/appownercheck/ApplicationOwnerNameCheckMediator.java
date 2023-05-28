package com.wso2.demo.apim.mediator.appownercheck;
/*
 * Copyright (c) 2023, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wso2.carbon.apimgt.common.gateway.util.JWTUtil;

//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//import java.lang.reflect.Type;
//import java.util.List;

import java.util.Map;

public class ApplicationOwnerNameCheckMediator extends AbstractMediator {
    /////////////////////////////////////////////////////////////////////
    // Use Case: Ensure that the API Application Owner from the JWT token
    // details: sub name match the ownwers list from the policy file.
    /////////////////////////////////////////////////////////////////////
    // DISCLAMER: This demo is for a very specific use case. It is not a
    // match for 99.9% of the time where the API Product is
    // the correct choice.
    //////////////////////////////////////////////////////////////////////
    // DISCLAMER: This is not production ready and should only be
    // viewed as a POC/Demo starting place to write your
    // own code from. This has NOT been battle tested.
    //////////////////////////////////////////////////////////////////////
    // ASSUMPTION: Token type will be a JWT token. Not checking so far.
    //////////////////////////////////////////////////////////////////////

    /* TODO 
        - add null checks
        - check that expected data is present - mock asserts
        - validate we have a JWT token
        - validate that policy apiOwnerDetails string has well formed json array
        - enhance debugging ability such as trace
        - enhance fault logic to generate an unauthorized application fault code and message that makes sense for this mediator
    */

    //Setting up variables per Class Mediator requirements
    //@see https://ei.docs.wso2.com/en/latest/micro-integrator/references/mediators/class-Mediator/
    private static final Log log = LogFactory.getLog(ApplicationOwnerNameCheckMediator.class);
    private String ownerList = "";

    @Override
    public boolean mediate(MessageContext context) {
        setups();

        boolean ownerFound = false;

        // Extract the "sub" field from the JWT token
        // Note: You will need to implement the logic to extract the "sub" field from
        // the JWT token.
        String sub = extractSubFromJwt(context);      

        // Get the provided Application Owners to pass through for this Resource
        ownerFound = checkOwners(sub, context);

        // All done and responding back our true/false response
        return generateResponse(ownerFound, context);
    }

    private boolean checkOwners(String sub, MessageContext context) {
        //Manual santity check
        debugWorkingValues(sub, getOwnerList());
        
        //verify that the data sub value in JWT token matches 
        //one of the names in the API Resource Owners List
        return getOwnerList().contains(sub);

        //OR if you want to use a JSON Array instead of comma delimited
        /*
        Gson gson = new Gson();
        String rawOwners = (String) context.getProperty("apiOwnerDetails");

        //Manual santity check
        debugWorkingValues(sub, rawOwners);
        //debugWorkingValues(sub, this.ownersJson);

        // Define the type for the list
        Type listType = new TypeToken<List<String>>() {}.getType();

        // Convert JSON array to List
        List<String> list = gson.fromJson(rawOwners, listType);
        //List<String> list = gson.fromJson(this.ownersJson, listType);

        //debugging
        log.info("Exiting mediate of " + this.getShortDescription());

        // Check if a value is present
        if (list.contains(sub)) {
            // Value is present in the array
            return true;
        }
        return false;
        */
    }

    private void setups() {
        this.setShortDescription(this.getMediatorName());
        this.setDescription(this.getMediatorName());
        log.info("Entered mediate of " + this.getShortDescription());
    }

    private void debugWorkingValues(String sub, String owners) {
        log.info("API/Resource Owners: " + owners);
        log.info("Application Subscriber Per JWT: " + sub);
    }

    // Implement the logic to extract the "sub" field from the JWT token
    private String extractSubFromJwt(MessageContext context) {
        try {
            // Cast the message context to access transport headers
            org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) context)
            .getAxis2MessageContext();

            // Access the JWT token from the transport headers
            Map<String, Object> headers = (Map<String, Object>) axis2MessageContext
              .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            
            if (headers == null){
                throw new NullPointerException("ERROR: headers is null for some reason.");
            }  

            //This is what I expected from try-it
            String jwtToken = (String) headers.get("Authorization");
            //This is what I get by the time I goes through dev portal try-it
            if (jwtToken == null){
                jwtToken = (String) headers.get("X-JWT-Assertion");
            }

            if (jwtToken == null){
                for (Map.Entry<String, Object> entry: headers.entrySet()){
                    log.error("TRANSPORT_HEADER: " + entry.getKey() + " : " + entry.getValue());
                }
                throw new NullPointerException("jwtToken is null for some reason. Check headers.");
            }

            Map<String, String> claimsMap = JWTUtil.getJWTClaims(jwtToken);// do we need to decrypt?
            return (String) claimsMap.get("sub");
        } catch (Exception e) {
            handleException("Exception in extractSubFromJwt.", e, context);
        }
        return null;
    }

    private boolean generateResponse(boolean ownerFound, MessageContext context) {
        if (ownerFound){ 
            return true;
        } 
        context.setFaultResponse(true);
        return false;
    }

    /**
     * @return String return the ownerList
     */
    public String getOwnerList() {
        return this.ownerList;
    }

    /**
     * @param ownerList the ownerList to set
     * NOTE: This is auto set based on the 
     * API Resource Policy Attribute value list.
     */
    public void setOwnerList(String ownerList) {
        this.ownerList = ownerList.trim();
    }

    public String getType() {
        return null;
    }

    public void setTraceState(int traceState) {
        traceState = 0;
    }

    public int getTraceState() {
        return 0;
    }

}
