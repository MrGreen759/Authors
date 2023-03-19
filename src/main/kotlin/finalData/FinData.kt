package finalData

import Author
import Comment
import Post

data class PostFull (
    var post: Post?,
    var postAuthor: Author?,
    val comments: MutableList<CommentFull>?
    )

data class CommentFull(
    var comment: Comment?,
    var commentAuthor: Author?
    )