package com.example.jinstagram.navigation.model

data class AlarmDTO(
    var destinationUid : String? = null,
    var userId : String? = null,
    var uid : String? = null,
    var kind : Int? = null,
    // 0 -> 좋아요 알람
    // 1 -> 댓글 알람
    // 2 -> 팔로우 알람
    var message : String? = null,
    var timestamp : Long? = null
)