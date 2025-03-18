/*
Copyright 2016 StepStone Services

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.wearezeta.auto.common.email.inbucket;

import com.wearezeta.auto.common.email.inbucket.models.Message;
import com.wearezeta.auto.common.email.inbucket.models.MessageInfo;
import com.wearezeta.auto.common.log.ZetaLogger;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;
import java.util.logging.Logger;

public class InbucketClient {

    private static final Logger log = ZetaLogger.getLog(InbucketClient.class.getSimpleName());

    protected InbucketService service;

    protected InbucketService simpleService;

    public InbucketClient(String baseUrl, String user, String password, boolean useProxy) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient().newBuilder();

        if (useProxy) {
            Authenticator authenticator = new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication("qa",
                            com.wearezeta.auto.common.credentials.Credentials.get("SOCKS_PROXY_PASSWORD").toCharArray()));
                }
            };
            Authenticator.setDefault(authenticator);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("socks.wire.link", 1080));
            clientBuilder.proxy(proxy);
        }

        class LoggingInterceptor implements Interceptor {
            @Override public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();

                long t1 = System.nanoTime();
                log.info(String.format("Sending request %s on %s%n%s", request.url(), chain.connection(),
                        request.headers()));

                Response response = chain.proceed(request);

                long t2 = System.nanoTime();
                log.info(String.format("Received response for %s in %.1fms%n%s", response.request().url(),
                        (t2 - t1) / 1e6d, response.headers()));

                return response;
            }
        }

        OkHttpClient okHttpClient = clientBuilder
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();

                    Request.Builder builder = originalRequest.newBuilder().header("Authorization",
                            Credentials.basic(user, password));

                    Request newRequest = builder.build();
                    return chain.proceed(newRequest);
                })
                .addInterceptor(new LoggingInterceptor())
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        service = retrofit.create(InbucketService.class);
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .build();
        simpleService = retrofit.create(InbucketService.class);
    }

    /**
     *  Method callss inbuckets GET /api/v1/mailbox/{mailboxName} endpoint
     * @param mailboxName name of mailbox
     * @return List of MailboxEntry
     * @throws IOException if something went wrong
     */
    public List<MessageInfo> getMailbox(String mailboxName) throws IOException {
        return service.getMailbox(mailboxName).execute().body();
    }

    /**
     *  Method calls inbuckets GET /api/v1/mailbox/{mailboxName}/{messageId} endpoint
     * @param mailboxName name of mailbox
     * @param messageId id of message
     * @return List of Message
     * @throws IOException if something went wrong
     */
    public Message getMessage(String mailboxName, String messageId) throws IOException {
        return service.getMessage(mailboxName, messageId).execute().body();
    }

    /**
     * Method calls inbuckets GET /api/v1/mailbox/{mailboxName}/{messageId}/source endpoint
     * @param mailboxName name of mailbox
     * @param messageId id of message
     * @return message source as string
     * @throws IOException if something went wrong
     */
    public String getMessageSource(String mailboxName, String messageId) throws IOException {
        return simpleService.getMessageSource(mailboxName, messageId).execute().body().string();
    }


    /**
     * Method calls inbuckets DELETE /api/v1/mailbox/{mailboxName} endpoint
     * @param mailboxName name of mailbox
     * @return "OK" string on success
     * @throws IOException on error
     */
    public String deleteMailbox(String mailboxName) throws IOException {
             return service.deleteMailbox(mailboxName).execute().body();
    }

    /**
     *  Method calls inbuckets DELETE /api/v1/mailbox/{mailboxName}/{messageId} endpoint
     * @param mailboxName name of mailbox
     * @param messageId id of message
     * @return "OK" string on success null on error
     * @throws IOException on error
     */
    public String deleteMessage(String mailboxName, String messageId) throws IOException {
        return service.deleteMessage(mailboxName,messageId).execute().body();
    }
}
