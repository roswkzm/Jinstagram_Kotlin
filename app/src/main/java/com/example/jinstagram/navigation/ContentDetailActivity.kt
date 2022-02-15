package com.example.jinstagram.navigation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.jinstagram.R
import com.example.jinstagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_content_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*
import java.util.ArrayList

var uid : String? = null
var userId : String ? = null
var timestamp : Long? = null
var documentId : String? = null
var firestore : FirebaseFirestore? = null
class ContentDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_detail)

        firestore = FirebaseFirestore.getInstance()


        uid = intent.getStringExtra("destinationUid")
        userId = intent.getStringExtra("userEmail")
        timestamp = intent.getLongExtra("timestamp",0)

        // 회원 프사 매핑
        FirebaseFirestore.getInstance().collection("profileImages").document(uid!!).addSnapshotListener { value, error ->
            if(value == null) return@addSnapshotListener
            if (value.data != null){
                var url = value.data!!["image"]
                Glide.with(this).load(url).apply(RequestOptions().circleCrop()).into(contentdetail_iv_profile)
            }
        }

        contentdetail_tv_userId.text = userId   // 회원이름 매핑

        FirebaseFirestore.getInstance().collection("images").whereEqualTo("uid", uid).whereEqualTo("timestamp", timestamp).get().addOnSuccessListener { documents ->
            if (documents == null ) return@addOnSuccessListener

            for(document in documents){
                documentId = document.id
                Log.d("ㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇㅇ", documentId!!)
                Glide.with(this).load(document.data["imageUrl"]).into(contentdetail_iv_contentImage)
                contentdetail_tv_imageExplain.text = document.data["explain"].toString()
                if (document.data["favorites"].toString().contains(FirebaseAuth.getInstance().currentUser!!.uid)){
                    conetntdetail_iv_favorite.setImageResource(R.drawable.ic_favorite)
                }else{
                    conetntdetail_iv_favorite.setImageResource(R.drawable.ic_favorite_border)
                }
            }
        }


        conetntdetail_iv_favorite.setOnClickListener {
            favoriteEvent()
        }

        conetntdetail_iv_comment.setOnClickListener {
            var intent = Intent(this, CommentActivity::class.java)
            intent.putExtra("contentUid", documentId)
            intent.putExtra("destinationUid", uid)
            startActivity(intent)
        }
    }

    fun favoriteEvent(){
        var tsDoc = firestore?.collection("images")?.document(documentId!!)
        // 데이터를 입력하기 위해 transaction을 불러와야함
        firestore?.runTransaction { transaction ->

            var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)  // contentDto 캐스팅
            // 회원이 좋아요를 누른상태라면
            if (contentDTO!!.favorites.containsKey(FirebaseAuth.getInstance().currentUser!!.uid)){
                contentDTO?.favoriteCount = contentDTO?.favoriteCount -1    // 좋아요 -1
                contentDTO?.favorites.remove(FirebaseAuth.getInstance().currentUser!!.uid)   // 좋아요 누른 회원 아이디 제거
                conetntdetail_iv_favorite.setImageResource(R.drawable.ic_favorite_border)
            }else{  // 좋아요가 눌리지 않았다면
                contentDTO?.favoriteCount = contentDTO?.favoriteCount +1    // 좋아요 +1
                contentDTO?.favorites[FirebaseAuth.getInstance().currentUser!!.uid!!] = true // uid 추가
                conetntdetail_iv_favorite.setImageResource(R.drawable.ic_favorite)
            }
            // 트랜잭션의 결과를 서버로 되돌려줌
            transaction.set(tsDoc, contentDTO)
        }
    }
}