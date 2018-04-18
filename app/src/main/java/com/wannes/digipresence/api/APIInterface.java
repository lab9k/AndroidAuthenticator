package com.wannes.digipresence.api;

import com.wannes.digipresence.models.Campus;
import com.wannes.digipresence.models.Checkin;
import com.wannes.digipresence.models.CheckinPost;
import com.wannes.digipresence.models.Location;
import com.wannes.digipresence.models.Message;
import com.wannes.digipresence.models.Segment;
import com.wannes.digipresence.models.User;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface APIInterface {
    @GET("/API/campuses")
    Call<List<Campus>> doGetCampuses();

    @POST("/API/campus")
    Call<Campus> createCampus(@Body Campus campus);

    @POST("/API/segment")
    Call<Segment> createSegment(@Body Segment segment);

    @POST("/API/location")
    Call<Location> createlocation(@Body Location location);

    @POST("/API/checkin")
    Call<ResponseBody> createCheckin(@Body CheckinPost checkin);

    @PUT("/API/campus")
    Call<Campus> updateCampus(@Body Campus campus);

    @PUT("/API/segment")
    Call<Segment> updateSegment(@Body Segment segment);

    @PUT("/API/location")
    Call<Location> updateLocation(@Body Location location);

    @PUT("/API/user")
    Call<User> updateUser(@Body User user);

    @PUT("/API/message")
    Call<Message> updateMessage(@Body Message message);

    @GET("/API/user/phoneid/{phoneid}")
    Call<User> getUserByPhoneid(@Path("phoneid") String phoneid);

    @GET("/API/location/{id}")
    Call<Location> getLocationById(@Path("id") String id);
}
