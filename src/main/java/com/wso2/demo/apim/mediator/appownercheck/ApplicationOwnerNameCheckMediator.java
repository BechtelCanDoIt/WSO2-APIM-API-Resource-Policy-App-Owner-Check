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
//import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.carbon.apimgt.common.gateway.util.JWTUtil;
//import org.wso2.carbon.apimgt.impl.utils.APIUtil;

import java.util.HashMap;
import java.util.Map;

public class ApplicationOwnerNameCheckMediator extends AbstractMediator {
    /////////////////////////////////////////////////////////////////////
    // Use Case: Ensure that the API Application Owner from the JWT token
    // details: sub name match the given API Resource Policy
    // Attribute sub-name value.
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

    @Override
    public boolean mediate(MessageContext context) {
        setups();
        
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

        // Option A: Access the policy attributes
        // Option A: Will not work in a mediator
        // Note: You will need to implement the logic to access the policy attributes.
        Map<String, String> policyAttributes = getPolicyAttributes(context);



        // Ensure we have required values
        if (jwtToken != null && sub != null 
            && policyAttributes != null 
            && !jwtToken.isEmpty() && !sub.isEmpty() 
            && policyAttributes.size() > 0
            ) {
            debugWorkingValues(sub, policyAttributes);
            // Check if the "sub" name matches one of the values in the policy attributes.
            // If they do not match, generate an unauthorized application fault code and
            // message.
            //
            // Note: Modern programming practices would utilize Assert functionality. 
            // however Mediators that throw expections cause problems for the service
            // since it is looking for a true/false result. So to accomodate this
            // we are doing simple checks, logging, and returning false if we are 
            // missing a dependency.

            for (Map.Entry<String, String> entry : policyAttributes.entrySet()) {
                if (entry.getValue().trim().equalsIgnoreCase(sub.trim())) {
                    return true;
                }
            }
        } else {
            log.error("Assert failure");
            deepDebug(jwtToken, sub, policyAttributes);
        }
        return generateUnauthorizedFault(context);
    }

    private void setups() {
        this.setShortDescription(this.getMediatorName());
        this.setDescription(this.getMediatorName());
    }

    private void deepDebug(String jwtToken, String sub, Map<String, String> policyAttributes) {
        if (jwtToken == null) {
            log.error("jwtToken is null.");
        }
        if (sub == null) {
            log.error("sub is null.");
        }
        if (policyAttributes == null) {
            log.error("policyAttributes is null.");
        }
        if (jwtToken.isEmpty()) {
            log.error("jwtToken is empty.");
        }
        if (sub.isEmpty()) {
            log.error("sub is empty.");
        }
        if (policyAttributes.size() == 0) {
            log.error("policyAttributes size equals zero.");
        }
    }

    private void debugWorkingValues(String sub, Map<String, String> policyAttributes) {
        // Log the policy attribute names and values and target owner/sub value.
        for (Map.Entry<String, String> entry : policyAttributes.entrySet()) {
            log.debug("Policy Attribute Name: " + entry.getKey() + ", Value: " + entry.getValue());
        }
        log.debug("sub: " + sub);
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

    // Implement the logic to access the policy attributes
    /**
     * @see https://github.com/wso2/product-apim/tree/v4.1.0/modules/distribution/resources/operation_policies
     * 
     * Ideally we'd utilize the policy attributes for keeping a list of application owners that can pass.
     * However these items are not avaialble at the mediation level in a clean way. To use this 
     * approach will require lots of custom code that is out of scope for this project.
     * 
     * @param context
     * @return
     */
    private Map<String, String> getPolicyAttributes(MessageContext context) {
        // TODO: Implement this method
        return new HashMap<>();
    }

    // Implement the logic to generate an unauthorized application fault code and
    // message
    private boolean generateUnauthorizedFault(MessageContext context) {
        context.setFaultResponse(true);
        return false;
    }
}
