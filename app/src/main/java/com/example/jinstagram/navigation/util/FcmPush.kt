package com.example.jinstagram.navigation.util

import android.util.Log
import com.example.jinstagram.navigation.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class FcmPush {

    var JSON = "application/json; charset=utf-8".toMediaType()
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAAsTfnt2s:APA91bEZfdy4u51JDhbPb-cB2wSbgOIhKP9lZsmDB_Q8YN3hKaqI2dQIt9OveOMQZRTT3En39kqEki9ks_SJN-9RSYyp8dHV08lqeyoHObSDPg9tqaTHe2o2rED6yem-Tv26nq-I2Qgs"
    var gson : Gson? = null
    var okHttpClient : OkHttpClient? = null

    companion object{       // 어디에서든 접근 가능하도록 싱글톤 패턴으로 만듬
        var instance = FcmPush()
    }

    init {      // 객체 초기화
        gson = Gson()
        okHttpClient = OkHttpClient()
    }
    fun sendMessage(destinationUid : String, title : String, message : String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                var token = task.result.get("pushToken").toString()

                var pushDTO = PushDTO()
                pushDTO.to = token      // 메시지 받을 사람의 토큰값 저장
                pushDTO.notification.title = title      // 제목
                pushDTO.notification.body = message     // 본문


                var body = RequestBody.create(JSON, gson?.toJson(pushDTO)!!)
                var request = Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key=${serverKey}")
                    .url(url)
                    .post(body)
                    .build()

                okHttpClient?.newCall(request)?.enqueue(object : Callback{
                    // 요청 실패시
                    override fun onFailure(call: Call, e: IOException) {
                    }
                    // 요청 성공시
                    override fun onResponse(call: Call, response: Response) {
                        println(response.body?.string())
                    }

                })
            }
        }
    }
}