import com.twitter.finagle
import com.twitter.finagle.Service
import com.twitter.finagle.http._
import com.twitter.finagle.http.service.HttpResponseClassifier
import com.twitter.util.{Await, Future}

object HttpClient {
  class InvalidRequest extends Exception

  def main(args: Array[String]): Unit = {
    val client: Service[Request, Response] = finagle.Http.client.withSessionQualifier.noFailFast
      .withResponseClassifier(HttpResponseClassifier.ServerErrorsAsFailures)
      .withSessionQualifier
      .consecutiveFailuresFailureAccrual(2)
      .withSessionPool
      .maxSize(1)
      .newService("localhost:8080")

    var list: Seq[Future[Response]] = Seq.empty

    for (_ <- 1 to 8) {
      val future: Future[Response] = makeInvalidRequest(client)
      list = list.+:(future)
    }
    val endFuture = list
      .reduce((x, y) => (x join y).map(it => it._1))
      .ensure {
        client.close()
      }
    Await.result(endFuture)
  }

  private[this] def makeValidRequest(client: Service[Request, Response]) = {
    val authorizedRequest = Request(Version.Http11, Method.Get, "/request/valid")

    client(authorizedRequest).onSuccess { response =>
      val responseString = response.contentString
      println("))) Received result for authorized request: " + responseString)
    }
  }

  private[this] def makeInvalidRequest(client: Service[Request, Response]) = {
    val invalidRequest = Request(Version.Http11, Method.Get, "/request/invalid")

    client(invalidRequest)
      .onSuccess(it => println(s"Response status ${it.status}"))
  }
}
