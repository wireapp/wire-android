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
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

interface InbucketService {

    @GET("api/v1/mailbox/{name}")
    Call<List<MessageInfo>> getMailbox(@Path("name") String mailboxName);

    @GET("api/v1/mailbox/{name}/{id}")
    Call<Message> getMessage(@Path("name") String mailboxName, @Path("id") String messageId);

    @GET("api/v1/mailbox/{name}/{id}/source")
    Call<ResponseBody> getMessageSource(@Path("name") String mailboxName, @Path("id") String messageId);

    @DELETE("api/v1/mailbox/{name}")
    Call<String> deleteMailbox(@Path("name") String mailboxName);

    @DELETE("api/v1/mailbox/{name}/{id}/source")
    Call<String> deleteMessage(@Path("name") String mailboxName, @Path("id") String messageId);
}
