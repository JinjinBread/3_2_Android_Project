package com.example.sangsangstagram.data.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.sangsangstagram.data.model.UserDto
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.tasks.await

class UserPagingSource(
    private val userQuery: Query
) : PagingSource<QuerySnapshot, UserDto>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, UserDto>): QuerySnapshot? {
        return null
    }

    override suspend fun load(
        params: LoadParams<QuerySnapshot>
    ): LoadResult<QuerySnapshot, UserDto> {
        return try {
            val currentPage = params.key ?: userQuery.get().await()
            if (currentPage.isEmpty) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }
            val lastVisiblePost = currentPage.documents[currentPage.size() - 1]
            val nextPage = userQuery.startAfter(lastVisiblePost).get().await()
            val userDto = currentPage.toObjects<UserDto>()

            LoadResult.Page(
                data = userDto,
                prevKey = null,
                nextKey = nextPage
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}