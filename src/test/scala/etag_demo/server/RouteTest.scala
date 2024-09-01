package etag_demo.server

import etag_demo.common.Catalogue
import zio.http.Status.Ok
import zio.http.{Header, Path, Request, Status, URL}
import zio.test.{TestAspect, ZIOSpecDefault, assertTrue}
import zio.{Ref, ZEnvironment}

object RouteTest extends ZIOSpecDefault {

  val catalogue: Catalogue = Catalogue(version = 4, Map("item1" -> 2500))
  val routesWithoutSomeDeps = Main.getCatalogueRoute.toRoutes

  def spec = suite("Testing the GET route")(

    test("when request DOES NOT contain header 'If-None-Match' - we always get back the actual content") {
      val req = Request.get(URL(Path.root./("catalogue")))
      for {
        ref <- Ref.make(catalogue)
        app = routesWithoutSomeDeps.provideEnvironment(ZEnvironment(ref))
        response <- app.runZIO(req)
        responseBodyAsString <- response.body.asString
        _ <- zio.Console.printLine("\t==>\t" + responseBodyAsString)
      } yield assertTrue(response.status == Ok)
    },
    test("when request DOES contain header 'If-None-Match' and it is the same as the Etag of the response - we get back 304 and empty body") {
      val req = Request.get(URL(Path.root./("catalogue"))).addHeader(Header.IfNoneMatch.parse("4").getOrElse(???))
      for {
        ref <- Ref.make(catalogue)
        app = routesWithoutSomeDeps.provideEnvironment(ZEnvironment(ref))
        response <- app.runZIO(req)
      } yield assertTrue(response.status == Status.NotModified && response.body.isEmpty)
    },
    test("when request DOES contain header 'If-None-Match' but it is different than the Etag of the response - we get back 200 and the actual body") {
      val req = Request.get(URL(Path.root./("catalogue"))).addHeader(Header.IfNoneMatch.parse("etag-that-is-different").getOrElse(???))
      for {
        ref <- Ref.make(catalogue)
        app = routesWithoutSomeDeps.provideEnvironment(ZEnvironment(ref))
        response <- app.runZIO(req)
        responseBodyAsString <- response.body.asString
        _ <- zio.Console.printLine("\t==>\t" + responseBodyAsString)
      } yield assertTrue(response.status == Status.Ok)
    }
  ) @@ TestAspect.sequential
}
