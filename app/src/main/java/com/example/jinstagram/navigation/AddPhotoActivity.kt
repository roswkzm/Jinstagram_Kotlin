package com.example.jinstagram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.jinstagram.R
import com.example.jinstagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    val PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage? = null
    var photoUri : Uri? = null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // 스토리지 초기화
        storage = FirebaseStorage.getInstance()

        // 회원 정보
        auth = FirebaseAuth.getInstance()
        // Firestore 정보
        firestore = FirebaseFirestore.getInstance()

        // 앨범접근
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)

        // 사진 업로드 버튼 클릭
        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM){
            if (resultCode == Activity.RESULT_OK){
                // 사진을 선택하면 이미지의 경로가 이쪽으로 넘어온다(사진을 정상적으로 선택하였을 시)
                photoUri = data?.data
                addphoto_image.setImageURI(photoUri)
            }else{
                // 취소버튼을 눌렀을 때 작동하는 부분
                finish()
            }
        }
    }
    fun contentUpload(){
        // 파일이름 만들기
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"

        // Firebase Storage에 파일 업로드하기
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)
        storageRef?.putFile(photoUri!!)?.addOnCompleteListener{
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                var contentDTO = ContentDTO()
                // ContentDTO 정보넣
                contentDTO.imageUrl = uri.toString()    // 사진 주소
                contentDTO.uid = auth?.currentUser?.uid     // 회원 uid
                contentDTO.userId = auth?.currentUser?.email    // 회원 email
                contentDTO.explain = addphoto_edit_explain.text.toString()  // 사진설명
                contentDTO.timestamp = System.currentTimeMillis()       // 사진 등록 시간

                // contentDTO의 정보를 firestore의 "images"라는 컬렉션에 넣어서 정보를 관리한다.
                firestore?.collection("images")?.document()?.set(contentDTO)

                setResult(Activity.RESULT_OK)

                finish()
            }
        }
    }
}