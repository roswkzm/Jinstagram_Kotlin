package com.example.jinstagram.navigation.model

data class FollowDTO(
    var followerCount : Int = 0,     // 팔로워 수
    var followers : MutableMap<String, Boolean> = HashMap(),     // 중복 팔로워 방지

    var followingCount : Int = 0,   // 팔로잉  수
    var followings : MutableMap<String, Boolean> = HashMap()    // 팔로잉 중복 방지
)