package etag_demo.client

import zio.ZIO
import zio.http.URL
import zio.http.endpoint.EndpointLocator

trait EndpointLocatorResolver {

  private val defaultUrl: zio.http.URL = URL.decode("http://localhost:8080").toOption.get

  protected val resolveEndpointLocator: ZIO[Any, Unit, EndpointLocator] = for {
    custom <- zio.System.envOrOption("SERVER_URL", None).orDie

    url <- custom match {
      case None => ZIO.succeed(defaultUrl)
      case Some(urlString) =>
        URL.decode(urlString) match {
          case Left(_) => ZIO.logError(s"Provided SERVER_URL value [$urlString] is invalid.") *> ZIO.fail(())
          case Right(value) => ZIO.succeed(value)
        }
    }
    endpointLocator = EndpointLocator.fromURL(url)
  } yield endpointLocator
}
