package com.example.jinstagram.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.jinstagram.LoginActivity
import com.example.jinstagram.MainActivity
import com.example.jinstagram.R
import com.example.jinstagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {
    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid")    // MainActivity 화면에서 넘어온 값을 받아옴
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid     // uid와 currentUserUid의 비교를 통해 자신의 UserFragment인지 상대방의 것인지 확인

        if (uid == currentUserUid){
            // 내 UserFragment 화면일 경우 로그아웃 버튼이 되며 로그인 페이지로 이동
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            // 다른 상대방의 UserFragment일 경우
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)     // 팔로우 버튼으로 텍스트 변경
            var mainactivity = (activity as MainActivity)
            // 상단의 툴바에 상대방의 id가 표시됨
            mainactivity.toolbar_username?.text = arguments?.getString("userId")
            // 뒤로가기 버튼을 누르면 홈화면으로 이동
            mainactivity.toolbar_btn_back?.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            // 상단의 툴바 로고를 사라지게 만듬
            mainactivity.toolbar_title_image?.visibility = View.GONE
            // 상단의 툴바의 유저이름과 뒤로가기 버튼을 보이게 만듬
            mainactivity.toolbar_username?.visibility = View.VISIBLE
            mainactivity.toolbar_btn_back?.visibility = View.VISIBLE
        }

        // RecyclerViewAdapter 연결
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        // 그리드 레이아웃을 사용하여 3개씩 뜰 수 있도록 함
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity, 3)
        return fragmentView
    }

    // RecyclerView가 사용할 Adapter 생성
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(querySnapshot == null) return@addSnapshotListener    // querySnapshot 이 null일 경우 종료

                // 데이터 가져오기
                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                // content에 uid가 일치하는 데이터가 담겨있으므로 그 size를 포스트 갯수로 설정
                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3    // 라사이클 뷰의 폭의 3분의 1값
            var imageview = ImageView(parent.context)
            // 이미지 뷰의 크기를 width * width 로 한다.
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

    }
}