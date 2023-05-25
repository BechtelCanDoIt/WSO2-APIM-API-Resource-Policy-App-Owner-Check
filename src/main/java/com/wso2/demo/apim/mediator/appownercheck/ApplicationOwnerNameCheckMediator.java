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
import org.wso2.carbon.apimgt.common.gateway.util.JWTUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

import java.util.HashMap;
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
    
    @Override
    public boolean mediate(MessageContext context) {
        setups();

        boolean ownerFound = false;

        // Cast the message context to access transport headers
        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) context)
                .getAxis2MessageContext();

        // Access the JWT token from the transport headers
        Map<String, Object> headers = (Map<String, Object>) axis2MessageContext
                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        String jwtToken = (String) headers.get("Authorization");

        // Extract the "sub" field from the JWT token
        // Note: You will need to implement the logic to extract the "sub" field from
        // the JWT token.
        String sub = extractSubFromJwt(jwtToken, context);      

        // Get the provided Application Owners to pass through for this Resource
        ownerFound = checkOwners(sub, context);

        // All done and responding back our true/false response
        return generateResponse(ownerFound, context);
    }

    private boolean checkOwners(String sub, MessageContext context) {
        Gson gson = new Gson();
        String rawOwners = (String) context.getProperty("apiOwnerDetails");

        // Define the type for the list
        Type listType = new TypeToken<List<String>>() {}.getType();

        // Convert JSON array to List
        List<String> list = gson.fromJson(rawOwners, listType);

        //Manual santity check
        debugWorkingValues(sub, rawOwners);

        // Check if a value is present
        if (list.contains(sub)) {
            // Value is present in the array
            return true;
        }
        return false;
    }

    private void setups() {
        this.setShortDescription(this.getMediatorName());
        this.setDescription(this.getMediatorName());
        log.debug("Entered mediate of " + this.getShortDescription());
    }

    private void debugWorkingValues(String sub, String owners) {
        log.debug("API/Application Owners: " + owners);
        log.debug("Application Subscriber Per JWT: " + sub);
    }

    // Implement the logic to extract the "sub" field from the JWT token
    private String extractSubFromJwt(String jwtToken, MessageContext context) {
        try {
            Map<String, String> claimsMap = JWTUtil.getJWTClaims(jwtToken);// do we need to decrypt?
            return (String) claimsMap.get("sub");
        } catch (Exception e) {
            handleException("Exception in extractSubFromJwt.", e, context);
        }
        return null;
    }

    private boolean generateResponse(boolean ownerFound, MessageContext context) {
        if (ownerFound){ return true;}
        context.setFaultResponse(true);
        return false;
    }
}
