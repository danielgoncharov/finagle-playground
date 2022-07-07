import com.twitter.finagle.Http
import com.twitter.finagle.http._
import com.twitter.util.{Await, Future}

object HttpServer {

  private def router = new HttpMuxer()
    .withHandler(
      "/request/valid",
      (request: Request) => {
        Future.value(Response.apply(request.version, Status.Ok))
      }
    )
    .withHandler(
      "/request/invalid",
      (request: Request) => {
        println(s"Serving invalid request ${System.currentTimeMillis()}")
        Future.value(Response.apply(request.version, Status.ServiceUnavailable))
      }
    )

  def main(args: Array[String]): Unit = {
    val server = Http.server
      .serve(
        ":8080",
        router
      )
    Await.ready(server)
  }
}
