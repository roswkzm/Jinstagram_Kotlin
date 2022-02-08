package com.example.jinstagram.navigation.model

// 회원의 게시글 DTO
data class ContentDTO(
    var explain: String? = null,    // 사진 설명
    var imageUrl: String? = null,   // 사진 주소
    var uid: String? = null,       // 사진 올린사람의 uid
    var userId: String? = null,    // user의 email
    var timestamp: Long? = null,   // 사진 올린 시간
    var favoriteCount: Int = 0,    // 좋아요 갯수
    var favorites: Map<String, Boolean> = HashMap()     // 중복 좋아요 방지 Boolean
    )

{
    // 회원 덧글 DTO
    data class Comment(
        var uid: String? = null,        // 덧글 쓴 회원 uid
        var userId: String? = null,     // 덧글 쓴 회원 Email
        var comment: String? = null,    // 덧글 내용
        var timestamp: Long? = null     // 덧글 쓴 시간
    )
}
