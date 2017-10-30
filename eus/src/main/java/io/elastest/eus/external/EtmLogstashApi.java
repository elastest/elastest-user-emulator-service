package io.elastest.eus.external;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface EtmLogstashApi {
    @POST("/")
    Call<ResponseBody> sendMessages(@Body RequestBody body);

}
