package com.example.jinstagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jinstagram.R
import com.example.jinstagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

// 1번째 프래그 먼트( 사용자들의 게시물 확인)
class DetailViewFragment : Fragment() {
    var firestore : FirebaseFirestore? = null
    var uid :String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        // 프래그먼트의 RecyclerView에 adapter 매핑
        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()
        init {
            // DB에 접근해서 데이터를 받아오기
            // firestore 접근 객체 초기화
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()
                // for문으로 스냅샷에 넘어오는 데이터를 전부 읽어옴
                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()  // 값이 새로고침 되도록 함
            }
        }

        // recyclerView의 레이아웃 설정
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        // 갯수
        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        // 데이터 매핑
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolder = (holder as CustomViewHolder).itemView

            // 회원이름
            viewHolder.detailviewitem_profile_textview.text = contentDTOs[position].userId
            // 회원이 올린 사진
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewHolder.detailviewitem_imageview_content)
            // 사진에 대한 설명
            viewHolder.detailviewitem_explain_textview.text = contentDTOs[position].explain
            // 좋아요 갯수
            viewHolder.detailviewitem_favoritecounter_textview.text = "Likes ${contentDTOs[position].favoriteCount}"
            // 회원프사
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(viewHolder.detailviewitem_profile_image)
            // 좋아요 버튼 클릭시
            viewHolder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }
            // 만약 ContentDto의 MutableMap에 내 uid가 포함되어 있을경우( 좋아요 누른경우)
            if (contentDTOs[position].favorites.containsKey(uid)){
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{      // 좋아요 안누른경우
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }
        }

        // 좋아요 버튼 누를시 수행구문
        fun favoriteEvent(position: Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            // 데이터를 입력하기 위해 transaction을 불러와야함
            firestore?.runTransaction { transaction ->

                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)  // contentDto 캐스팅
                // 회원이 좋아요를 누른상태라면
                if (contentDTO!!.favorites.containsKey(uid)){
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount -1    // 좋아요 -1
                    contentDTO?.favorites.remove(uid)   // 좋아요 누른 회원 아이디 제거
                }else{  // 좋아요가 눌리지 않았다면
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount +1    // 좋아요 +1
                    contentDTO?.favorites[uid!!] = true // uid 추
                }
                // 트랜잭션의 결과를 서버로 되돌려줌
                transaction.set(tsDoc, contentDTO)
            }
        }
    }
}