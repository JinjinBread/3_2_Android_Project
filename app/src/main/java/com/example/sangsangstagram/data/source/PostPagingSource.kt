package com.example.sangsangstagram.data.source

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.sangsangstagram.data.PostRepository
import com.example.sangsangstagram.data.model.LikeDto
import com.example.sangsangstagram.data.model.PostDto
import com.example.sangsangstagram.data.model.UserDto
import com.example.sangsangstagram.domain.model.Post
import kotlinx.coroutines.tasks.await

class PostPagingSource(
    private val getWriterUuids: suspend () -> List<String>
) : PagingSource<QuerySnapshot, Post>() {

    private val currentUserId = Firebase.auth.currentUser!!.uid
    private val likeCollection = Firebase.firestore.collection("likes")
    private val userCollection = Firebase.firestore.collection("users")

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Post>): QuerySnapshot? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun load(
        params: LoadParams<QuerySnapshot>
    ): LoadResult<QuerySnapshot, Post> {
        val db = Firebase.firestore
        val postCollection = db.collection("posts")
        val queryPostsByFollower = postCollection
            .whereIn("writerUuid", getWriterUuids())
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .limit(PostRepository.PAGE_SIZE.toLong())

        return try {
            val currentPage = params.key ?: queryPostsByFollower.get().await()
            if (currentPage.isEmpty) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }
            val lastVisiblePost = currentPage.documents[currentPage.size() - 1]
            val nextPage = queryPostsByFollower.startAfter(lastVisiblePost).get().await()
            val postDtos = currentPage.toObjects(PostDto::class.java)
            val posts = postDtos.map { postDto ->
                val likes = likeCollection.whereEqualTo("postUuid", postDto.uuid).get().await()
                    .toObjects(LikeDto::class.java)
                val writer = userCollection.document(postDto.writerUuid).get().await()
                    .toObject(UserDto::class.java)
                val meLiked = likes.any { like -> like.userUuid == currentUserId }

                Post(
                    uuid = postDto.uuid,
                    writerUuid = writer!!.uuid,
                    writerName = writer.name,
                    writerProfileImageUrl = writer.profileImageUrl,
                    content = postDto.content,
                    imageUrl = postDto.imageUrl,
                    likeCount = likes.size,
                    meLiked = meLiked,
                    isMine = postDto.writerUuid == currentUserId,
                    bookMarkChecked = false,
                    time = postDto.dataTime
                )
            }

            LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = nextPage
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}