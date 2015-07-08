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
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by bemmanuel on 6/14/15.
 */
public class Helper {

    private static final String TAG = Helper.class.getSimpleName();

    private String mConsumerKey;
    private String mConsumerSecret;
    private String mToken;
    private String mTokenSecret;
    private boolean mIsOAuth10a;
    private String mTokenRequestUrl;
    private String mAuthorizationUrl;
    private String mAccessTokenUrl;
    private String mCallbackUrl;

    public Helper(@NonNull String consumerKey,
                  @NonNull String consumerSecret,
                  @Nullable String token,
                  @Nullable String tokenSecret,
                  @NonNull String tokenRequestUrl,
                  @NonNull String authorizationUrl,
                  @NonNull String accessTokenUrl,
                  @NonNull String callbackUrl) {
        mConsumerKey = consumerKey;
        mConsumerSecret = consumerSecret;
        mToken = token;
        mTokenSecret = tokenSecret;
        mTokenRequestUrl = tokenRequestUrl;
        mAuthorizationUrl = authorizationUrl;
        mAccessTokenUrl = accessTokenUrl;
        mCallbackUrl = callbackUrl;
    }

    @Nullable
    public String getToken() {
        return mToken;
    }

    @Nullable
    public String getSecret() {
        return mTokenSecret;
    }

    @NonNull
    public void getTokenSecret(@NonNull OkHttpClient client) {
        Request request = new RequestTokenRequestBuilder(mConsumerKey,
                mConsumerSecret,
                mCallbackUrl)
                .url(mTokenRequestUrl)
                .build();
        Response response;

        try {
            response = client.newCall(request)
                    .execute();

            if (response.isSuccessful() && response.body() != null) {
                String content = response.body().string();

                if (!TextUtils.isEmpty(content)) {
                    String[] keyValuePairs = content.split("&");

                    if (keyValuePairs.length > 1) {
                        for (String keyValuePair : keyValuePairs) {
                            String[] keyValuePairParts = keyValuePair.split("=");

                            if (keyValuePairParts.length > 1) {
                                String key = keyValuePairParts[0];
                                String value = keyValuePairParts[1];

                                if (RequestBuilder.OAuthParameter.oauth_token.name().equals(key)) {
                                    mToken = value;
                                } else if ("oauth_token_secret".equals(key)) {
                                    mTokenSecret = value;
                                } else if ("oauth_callback_confirmed".equals(key)) {
                                    mIsOAuth10a = Boolean.TRUE.toString().equals(value);
                                }
                            }
                        }
                    }
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "unsuccessful getting token" + response.toString());
                }
            }
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "failed getting token", e);
            }
        }
    }

    public String getTokenAuthorizationUrl(@NonNull OkHttpClient client) {
        getTokenSecret(client);
        HttpUrl.Builder builder = HttpUrl.parse(mAuthorizationUrl)
                .newBuilder()
                .addQueryParameter(RequestBuilder.OAuthParameter.oauth_token.name(), mToken);

        if (!mIsOAuth10a) {
            builder.addQueryParameter(RequestBuilder.OAuthParameter.oauth_callback.name(), mCallbackUrl);
        }

        return builder.build().toString();
    }

    public boolean getAccessToken(@NonNull OkHttpClient client, @NonNull String authenticatedUrl) {
        if (!TextUtils.isEmpty(authenticatedUrl)) {
            String verifier = getParamValue(authenticatedUrl, "oauth_verifier");

            if (!TextUtils.isEmpty(verifier)) {
                Request request = new AccessTokenRequestBuilder(mConsumerKey,
                        mConsumerSecret,
                        mToken,
                        mTokenSecret,
                        mIsOAuth10a ? verifier : null)
                        .url(mAccessTokenUrl)
                        .build();
                Response response;

                try {
                    response = client.newCall(request)
                            .execute();

                    if (response.isSuccessful() && response.body() != null) {
                        String content = response.body().string();

                        if (!TextUtils.isEmpty(content)) {
                            String[] keyValuePairs = content.split("&");

                            if (keyValuePairs.length > 1) {
                                for (String keyValuePair : keyValuePairs) {
                                    String[] keyValuePairParts = keyValuePair.split("=");

                                    if (keyValuePairParts.length > 1) {
                                        String key = keyValuePairParts[0];
                                        String value = keyValuePairParts[1];

                                        if (RequestBuilder.OAuthParameter.oauth_token.name().equals(key)) {
                                            mToken = value;
                                        } else if ("oauth_token_secret".equals(key)) {
                                            mTokenSecret = value;
                                        }
                                    }
                                }

                                return true;// TODO actually check mToken and mTokenSecret
                            }
                        }
                    }
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "failed getting access token", e);
                    }
                }
            }
        }

        return false;
    }

    public Request.Builder getBuilder() {
        return new SigningRequestBuilder(mConsumerKey, mConsumerSecret, mToken, mTokenSecret);
    }

    @Nullable
    public static String getParamValue(@Nullable String url, @NonNull String name) {
        if (TextUtils.isEmpty(url)) return null;

        name += "=";
        int nameIndex = url.indexOf(name);

        if (nameIndex < 0) return null;

        String value = url.substring(nameIndex + name.length());

        int nextParamIndex = value.indexOf("&");

        if (nextParamIndex >= 0) {
            value = value.substring(0, nextParamIndex);
        }

        return value;
    }
}
