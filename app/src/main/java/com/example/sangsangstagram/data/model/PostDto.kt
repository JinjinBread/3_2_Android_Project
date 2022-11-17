package com.example.sangsangstagram.data.model

import java.util.*

data class PostDto(
    val uuid: String = "",
    val writerUuid: String = "",
    val content: String = "",
    val dateTime: Date = Date(),
    val imageUrl: String = ""
)
