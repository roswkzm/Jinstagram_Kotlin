package com.example.jinstagram.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.jinstagram.LoginActivity
import com.example.jinstagram.MainActivity
import com.example.jinstagram.R
import com.example.jinstagram.navigation.model.ContentDTO
import com.example.jinstagram.navigation.model.FollowDTO
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
    companion object{   // static의 개념으로 PICK_PROFILE_FROM_ALBUM 선언
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid")    // MainActivity 화면에서 넘어온 값을 받아옴
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid     // uid와 currentUserUid의 비교를 통해 자신의 UserFragment인지 상대방의 것인지 확인

        if (uid == currentUserUid){
            // 회원 프사 설정 ( 회원이 선택한 사진의 결과를 MainActivity에 넘김)
            fragmentView?.account_iv_profile?.setOnClickListener {
                var photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)
            }
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
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }

        // RecyclerViewAdapter 연결
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        // 그리드 레이아웃을 사용하여 3개씩 뜰 수 있도록 함
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity, 3)

        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

    //화면의 팔로워, 팔로잉 바인딩
    fun getFollowerAndFollowing(){
        // 현재 들어와있는 User에 대한 팔로워 팔로잉 부분
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { value, error ->
            if(value == null) return@addSnapshotListener
            var followDTO = value.toObject(FollowDTO::class.java)
            // 대상의 팔로잉 숫자가 비어있지 않다면 팔로잉 숫자 표시
            if(followDTO?.followingCount != null){
                fragmentView?.account_tv_following_count?.text = followDTO?.followingCount.toString()
            }

            // 팔로워 숫자 부분
            if (followDTO?.followerCount != null){
                fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount.toString()

                if (followDTO?.followers?.containsKey(currentUserUid)){     // 내가 팔로워 중이면
                    fragmentView?.account_btn_follow_signout?.text = "팔로우 취소"
                }else{
                    if(uid != currentUserUid) {     // 내가 팔로워 중이 아니라면
                        fragmentView?.account_btn_follow_signout?.text = "팔로우"
                    }
                }
            }
        }
    }


    // 팔로우 버튼을 눌렀을 경우에 대한 함수
    /*
    팔로워 : 누가 나를 팔로우 함
    팔로잉 : 내가 누구를 팔로우 함
    If 상대 나를 팔로우 했다면

    내 DB 부분
    팔로워 숫자 +1
    팔로워 -> 상대계정 추가

    상대 DB부분
    팔로잉 숫자 +1
    팔로잉 -> 내계정 추가
     */
    fun requestFollow(){
        // 여기서 uid는 DetailViewFragment에서 넘어온 대상의 uid이다.

        // 내 계정은 누가 팔로잉 하고 있는지에 대한 부분
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        // DB 삽입부
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)   // followDTO에 나의 firestore 캐스팅

            // 만약 나를 팔로 한사람이 없다면(followDTO에 아무정보도 없다 라는 조건임)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1      // 팔로잉 숫자 1로 등록
                followDTO!!.followings[uid!!] = true     // 나를 팔로우 한사람에 대상의 uid를 등록

                transaction.set(tsDocFollowing,followDTO)       // db에 저장
                return@runTransaction
            }

            // 나를 팔로우 한 사람이 있고 만약 대상이 나를 팔로우 했다면
            if (followDTO.followings.containsKey(uid)){     //
                // 팔로잉을 취소한다
                followDTO?.followingCount = followDTO?.followingCount -1    // 팔로잉 숫자를 1줄임
                followDTO?.followings?.remove(uid)       // 팔로잉을 취소하면 상대방 팔로우에서 나를 삭제
            }else{
                // 팔로잉을 한다.
                followDTO?.followingCount = followDTO?.followingCount +1    // 팔로잉 숫자를 1 늘림
                followDTO?.followings[uid!!] = true  // // 팔로잉을 취소하면 상대방 팔로우에서 나를 삭제
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }


        // 상대방 계정은 누구를 팔로우 하고 있는지에 대한 부분(위에서는 내 DB 여기서는 대상의 DB를 다룰꺼임)
        // 이유 내계정에 팔로잉이 증가했다면 상대방의 계정에는 팔로우가 증가해야하기 때문
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            // 만약 상대방이 팔로잉 한 사람이 없다면
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1       // 팔로워 숫자 1
                followDTO!!.followers[currentUserUid!!] = true  // 팔로워에 자신의 uid 등

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            // 상대방 계정에 내가 팔로우를 했을 경우
            if(followDTO!!.followers.containsKey(currentUserUid)){
                // 팔로우 취소
                followDTO!!.followerCount = followDTO!!.followerCount -1     // 상대방 팔로우 숫자 -1
                followDTO!!.followers.remove(currentUserUid!!)       // 상대방 팔로워에서 내 uid 제거
            }else{      // 상대방 계정을 내가 팔로우 안했을 경우
                followDTO!!.followerCount = followDTO!!.followerCount +1     // 상대방 팔로우 숫자 +1
                followDTO!!.followers[currentUserUid!!]  = true      // 상대방 팔로워에서 내 uid 추가
            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    // 회원의 프사 연결부분
    fun getProfileImage(){
        if(activity == null){
            return
        }
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { value, error ->
            if(value == null) return@addSnapshotListener    // 값이 없을경우 바로 리턴
            if(value.data != null){
                var url = value?.data!!["image"]    // 값 저장을 HashMap으로 했으므로 해당 uid에 저장되어있는 키를 이용해서 값을 꺼낸다.
                Glide.with(this).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
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