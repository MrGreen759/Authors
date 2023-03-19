import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import finalData.CommentFull
import finalData.PostFull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

fun main() {
    val posts: List<Post>
    val authorsOfPosts: MutableList<Author> = mutableListOf()
    val comments: MutableList<List<Comment>> = mutableListOf()
    //val PostsWithAuthors: List<PostFull>

    val gson = Gson()
    val BASE_URL = "http://127.0.0.1:10999"
    val typeToken = object : TypeToken<List<Post>>() {}
    val typeToken2 = object : TypeToken<Author>() {}
    val typeToken3 = object : TypeToken<List<Comment>>() {}
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .build()

    // получаем список постов
    var request = Request.Builder()
        .url("${BASE_URL}/api/posts")
        .build()

    posts = client.newCall(request)
        .execute()
        .let { it.body?.string() ?: throw RuntimeException("body is null") }
        .let {
            gson.fromJson(it, typeToken.type)
        }

    println("---- Список постов ----")
    println(posts)
    println("-----------------------")

    // получаем список авторов постов
    println("---- Авторы постов ----")
    for (i in 0..posts.size-1) {
        request = Request.Builder()
            .url("${BASE_URL}/api/authors/" + posts[i].authorId)
            .build()
        authorsOfPosts.add(i, client.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let {
                gson.fromJson(it, typeToken2.type)
            })
        println(authorsOfPosts[i])
    }
    println("-----------------------")

    // получаем список комментариев. Каждый элемент списка - список комментариев к конкретному посту.
    println("---- Комментарии ----")
    for (i in 0..posts.size-1) {
        request = Request.Builder()
            .url("${BASE_URL}/api/posts/" + posts[i].id + "/comments")
            .build()
        comments.add(i, client.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let {
                gson.fromJson(it, typeToken3.type)
            } )
        println(comments[i])
    }
    println("---------------------")

    // получаем список авторов комментариев, сразу вносим в финальный список комментариев
    println("---- Комментарии с авторами ----")
    val commentsWithAuthors: List<CommentFull> = List(comments.size) {CommentFull(null, null)}
    var n = 0
    println("size of comments = " + comments.size)
    for (i in 0..comments.size-1) {
        for (j in 0..comments[i].size-1) {
            if (comments[i][j] != null) {
                request = Request.Builder()
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
            println(commentsWithAuthors[n-1])
        }
    }
    println("--------------------------------")

    // заполняем финальный список постов
    println("---- Посты с авторами и комментариями ----")
    val postsWithAuthors: List<PostFull> = List(posts.size) {PostFull(null, null, null)}
    n = 0
    for (i in 0..posts.size-1) {
        postsWithAuthors[i].post = posts[i]
        postsWithAuthors[i].postAuthor = authorsOfPosts[i]

        for (j in 0..commentsWithAuthors.size-1) {
            if (commentsWithAuthors[j].comment?.postId == posts[i].id) {
                postsWithAuthors[i].comments?.add(n, commentsWithAuthors[j])
                n++
            }
        }
        println(postsWithAuthors[i])
    }
    println("------------------------------------------")

}