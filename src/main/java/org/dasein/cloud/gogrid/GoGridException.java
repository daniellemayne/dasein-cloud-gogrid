/**
 * ========= CONFIDENTIAL =========
 *
 * Copyright (C) 2012 enStratus Networks Inc - ALL RIGHTS RESERVED
 *
 * ====================================================================
 *  NOTICE: All information contained herein is, and remains the
 *  property of enStratus Networks Inc. The intellectual and technical
 *  concepts contained herein are proprietary to enStratus Networks Inc
 *  and may be covered by U.S. and Foreign Patents, patents in process,
 *  and are protected by trade secret or copyright law. Dissemination
 *  of this information or reproduction of this material is strictly
 *  forbidden unless prior written permission is obtained from
 *  enStratus Networks Inc.
 * ====================================================================
 */
package org.dasein.cloud.gogrid;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * Represents an error passed to Dasein Cloud by GoGrid either because of an error in GoGrid or an error with
 * the request.
 * <p>Created by George Reese: 10/13/12 4:05 PM</p>
 * @author George Reese
 * @version 2012.09 initial version
 * @since 2012.09
 */
public class GoGridException extends CloudException {
    static private final Logger logger = GoGrid.getLogger(GoGridException.class);
    static private final Logger wire = GoGrid.getWireLogger(GoGridException.class);

    static public class ParsedException {
        public int code;
        public String message;
        public String providerCode;
        public CloudErrorType type;

        public ParsedException(@Nonnull HttpResponse response) {
            code = response.getStatusLine().getStatusCode();
            providerCode = toCode(code);
            message = "";
            type = CloudErrorType.GENERAL;

            try {
                HttpEntity entity = response.getEntity();

                if( entity != null ) {
                    String json = EntityUtils.toString(entity);

                    if( wire.isDebugEnabled() ) {
                        wire.debug(json);
                    }
                    message = json;
                    try {
                        JSONObject ob = new JSONObject(json);

                        if( ob.has("list") ) {
                            JSONArray list = ob.getJSONArray("list");

                            if( list != null && list.length() > 0 ) {
                                JSONObject error = list.getJSONObject(0);

                                if( error.has("message") ) {
                                    message = error.getString("message");
                                }
                                if( error.has("errorcode") ) {
                                    providerCode = error.getString("errorcode");
                                }
                            }
                        }
                    }
                    catch( JSONException ignore ) {
                        // ignore parsing errors, probably html or xml
                    }
                }
            }
            catch( Throwable e ) {
                logger.error("Failed to parse error from GoGrid: " + e.getMessage());
            }
        }

        private @Nonnull String toCode(int code) {
            switch( code ) {
                case 400: return "IllegalArgument";
                case 401: return "Unauthorized";
                case 403: return "AuthenticationFailed";
                case 404: return "NotFound";
                case 500: return "UnexpectedError";
            }
            return String.valueOf(code);
        }
    }

    public GoGridException(@Nonnull ParsedException exception) {
        super(exception.type, exception.code, exception.providerCode, exception.message);
    }
}
