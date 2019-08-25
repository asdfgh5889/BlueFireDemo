package co.raisense.bluetoothdemo.callback;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface GetService {

    @FormUrlEncoded
    @POST("checkIncome.php")
    Call<String> sendData(
            @Field("data") String data
    );
}
