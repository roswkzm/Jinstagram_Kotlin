package com.example.jinstagram.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.jinstagram.R
import com.example.jinstagram.navigation.model.AlarmDTO
import com.example.jinstagram.navigation.model.ContentDTO
import com.example.jinstagram.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {
    var contentUid : String? = null
    var destinationUid : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        contentUid = intent.getStringExtra("contentUid")    // DetailViewFragment 에서 넘어온 contentUid 값을 저장
        destinationUid = intent.getStringExtra("destinationUid")

        // comment_recyclerview와  Adapter를 연결시키기
        comment_recyclerview.adapter = CommentRecyclerviewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

        // 댓글에 해당하는 정보 매칭
        comment_btn_send?.setOnClickListener {
            var comment = ContentDTO.Comment()
            // 덧글쓴 회원의 정보
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment = comment_edit_message.text.toString()
            comment.timestamp = System.currentTimeMillis()

            // 해당 게시물의 comments에 위 댓글정보를 저장한다.
            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)
            commentAlarm(destinationUid!!, comment_edit_message.text.toString())
            comment_edit_message.setText("")
        }
    }

    // 댓글을 달았을 시 알람 구축
    fun commentAlarm(destinationUid : String, message : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.kind = 1
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        // 푸쉬 알람 설정
        var msg = FirebaseAuth.getInstance().currentUser?.email + " " +getString(R.string.alarm_comment) + " of " + message
        FcmPush.instance.sendMessage(destinationUid, "Jinstagram", msg)
    }

    // RecyclerView 어댑터 생성
   inner class CommentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var comments : ArrayList<ContentDTO.Comment> = arrayListOf()
        init {
            FirebaseFirestore.getInstance().collection("images").document(contentUid!!)
                .collection("comments").orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { value, error ->
                    comments.clear()
                    if(value == null)return@addSnapshotListener

                    // for문으로 스냅샷 하나씩 읽어오기
                    for (snapshot in value.documents!!){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)   // comments를 ConntentDTO.Comment로 캐스팅
                    }
                    notifyDataSetChanged()      // 저장
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)    // R.layout.item_comment를 뷰로 지정
            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return comments.size
        }

        // recyclerview로 뿌려지는 아이템에 정보 매칭
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView
            view.commentviewitem_textview_comment.text = comments[position].comment
            view.commentviewitem_textview_profile.text = comments[position].userId

            // 프로필 사진 매칭
            FirebaseFirestore.getInstance().collection("profileImages")
                .document(comments[position].uid!!)
                .get().addOnCompleteListener { task ->
                    // 성공적으로 받아왔다면
                    if (task.isSuccessful){
                        var url = task.result!!["image"]
                        // Glide로 사진뿌리
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewitem_imageview_profile)
                    }
                }
        }

    }
}