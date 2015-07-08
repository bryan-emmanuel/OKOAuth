/*
 * Copyright 2015 Bryan Emmanuel
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
package com.piusvelte.okoauth;

import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

/**
 * Created by bemmanuel on 6/14/15.
 */
public class SigningRequestBuilder extends RequestBuilder {

    @NonNull
    private String mToken;
    @NonNull
    private String mTokenSecret;

    public SigningRequestBuilder(@NonNull String consumerKey,
                                 @NonNull String consumerSecret,
                                 @NonNull String token,
                                 @NonNull String tokenSecret) {
        super(consumerKey, consumerSecret);
        mToken = token;
        mTokenSecret = tokenSecret;
    }

    @Override
    protected void onAddOAuthParameters(@NonNull Set<String> parameters) {
        parameters.add(OAuthParameter.oauth_token.name() + "=" + mToken);
    }

    @Override
    protected SecretKeySpec createKey() throws UnsupportedEncodingException {
        String keyData = URLEncoder.encode(mConsumerSecret, "UTF-8") + "&" + URLEncoder.encode(mTokenSecret, "UTF-8");
        byte[] keyBytes = keyData.getBytes("UTF-8");
        return new SecretKeySpec(keyBytes, "HmacSHA1");
    }
}
