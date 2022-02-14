package com.example.jinstagram.navigation.model

data class PushDTO(
    var to : String? = null,         // 메시지를 받을 사람
    var notification : Notification = Notification()
){
    data class Notification(
        var title : String? = null,     // 메시지 이름
        var body : String? = null       // 메시지 내용
    )
}