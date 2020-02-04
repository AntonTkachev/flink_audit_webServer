import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object WebServer extends LazyLogging {

  def main(args: Array[String]) {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val requestHandler: HttpRequest => HttpResponse = {
      case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
        HttpResponse(entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          "<html><body>Hello, it's WebServer fro Flink Audit!</body></html>"))

      case HttpRequest(POST, Uri.Path("/startTaskManager"), header, _, _) =>
        val startTime = header.find(_.is("StartTime".toLowerCase())).get.value()
        val logStr = s"TaskManager was start in: $startTime!"
        logger.info(logStr)
        HttpResponse(entity = logStr)

      case HttpRequest(POST, Uri.Path("/submittedJob"), _, _, _) =>
        val logStr = "Job was submitted!"
        logger.info(logStr)
        HttpResponse(entity = logStr)

      case HttpRequest(POST, Uri.Path("/removeJob"), _, _, _) =>
        val logStr = "Job was removed!"
        logger.info(logStr)
        HttpResponse(entity = logStr)

      case HttpRequest(POST, Uri.Path("/cancelJob"), _, _, _) =>
        val logStr = "Job was canceled!"
        logger.info(logStr)
        HttpResponse(entity = logStr)

      case r: HttpRequest =>
        r.discardEntityBytes() // important to drain incoming HTTP Entity stream
        HttpResponse(404, entity = "Unknown resource!")
    }

    val host = "localhost"
    val port = 8080

    val bindingFuture = Http().bindAndHandleSync(requestHandler, host, port)
    println(s"Server online at http:/$host:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

  }
}
