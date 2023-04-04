import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import finalData.CommentFull
import finalData.PostFull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext

val BASE_URL = "http://127.0.0.1:10999"
val gson = Gson()

suspend fun main() {
    val postsWithAuthors: List<PostFull>
    var posts: List<Post> = listOf()
    var authorsOfPosts: MutableList<Author> = mutableListOf()
    var comments: MutableList<List<Comment>> = mutableListOf()
    var commentsWithAuthors: List<CommentFull> = mutableListOf()
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .build()

    // сетевые запросы
    with(CoroutineScope(EmptyCoroutineContext)) {
        launch {
            posts = getPosts(client)
            async { authorsOfPosts = getPostAuthors(client, posts) }.await()
            async { comments = getComments(client, posts) }.await()
            commentsWithAuthors = getCommentAuthors(client, comments)
        }.join()
    }

    // сборка финального списка постов
    postsWithAuthors = getPostsWithAuthoirs(posts, authorsOfPosts, commentsWithAuthors)

    println(postsWithAuthors)

}

// получаем список постов
private fun getPosts(client: OkHttpClient): List<Post> {
    val typeToken = object : TypeToken<List<Post>>() {}
    val request = Request.Builder()
        .url("${BASE_URL}/api/posts")
        .build()

    return client.newCall(request)
        .execute()
        .let { it.body?.string() ?: throw RuntimeException("body is null") }
        .let {
            gson.fromJson(it, typeToken.type)
        }
}

// получаем список авторов постов
private suspend fun getPostAuthors(client: OkHttpClient, posts: List<Post>): MutableList<Author> {
    val typeToken2 = object : TypeToken<Author>() {}
    val authors: MutableList<Author> = mutableListOf()
    for (i in 0..posts.size-1) {
        val request = Request.Builder()
            .url("${BASE_URL}/api/authors/" + posts[i].authorId)
            .build()
        authors.add(i, client.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let {
                gson.fromJson(it, typeToken2.type)
            })
//        println(authors[i])
    }
    return authors
}

// получаем список из списков комментариев к каждому посту
private suspend fun getComments(client: OkHttpClient, posts: List<Post>): MutableList<List<Comment>> {
    val typeToken3 = object : TypeToken<List<Comment>>() {}
    val comments: MutableList<List<Comment>> = mutableListOf()
    for (i in 0..posts.size-1) {
        val request = Request.Builder()
            .url("${BASE_URL}/api/posts/" + posts[i].id + "/comments")
            .build()
        comments.add(i, client.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let {
                gson.fromJson(it, typeToken3.type)
            } )
//        println(comments[i])
    }
    return comments
}

// получаем авторов комментариев и строим список комментариев с авторами
private fun getCommentAuthors(client: OkHttpClient, comments: MutableList<List<Comment>>): List<CommentFull> {
    val commentsWithAuthors: List<CommentFull> = List(comments.size) {CommentFull(null, null)}
    val typeToken2 = object : TypeToken<Author>() {}
    var n = 0
//    println("size of comments = " + comments.size)
    for (i in 0..comments.size-1) {
        for (j in 0..comments[i].size-1) {
            if (comments[i][j] != null) {
                val request = Request.Builder()
                    .url("${BASE_URL}/api/authors/" + comments[i][j].authorId)
                    .build()
                commentsWithAuthors[n].comment = comments[i][j]
                commentsWithAuthors[n].commentAuthor = client.newCall(request)
                    .execute()
                    .let { it.body?.string() ?: throw RuntimeException("body is null") }
                    .let {
                        gson.fromJson(it, typeToken2.type)
                    }
                n++
            }
//            println(commentsWithAuthors[n-1])
        }
    }
    return commentsWithAuthors
}

// собираем список постов с авторами и комментариями. Комменатрии - также с авторами
private fun getPostsWithAuthoirs(posts: List<Post>, authors: List<Author>, commentsFull: List<CommentFull>): List<PostFull> {
    val postsWithAuthors: List<PostFull> = List(posts.size) {PostFull(null, null, ArrayList<CommentFull>())}
    var n = 0
    for (i in 0..posts.size-1) {
        postsWithAuthors[i].post = posts[i]
        postsWithAuthors[i].postAuthor = authors[i]
        for (j in 0..commentsFull.size-1) {
            if (commentsFull[j].comment?.postId == posts[i].id) {
                postsWithAuthors[i].comments?.add(commentsFull[j])
                n++
            }
        }
//        println(postsWithAuthors[i])
    }
    return postsWithAuthors
}