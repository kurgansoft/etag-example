package etag_demo.server

import etag_demo.common.Catalogue
import zio.http.Status.Ok
import zio.http.{Header, Path, Request, Status, URL}
import zio.test.{Gen, TestAspect, ZIOSpecDefault, assertTrue, check}
import zio.{Ref, ZEnvironment}

object RouteTest extends ZIOSpecDefault {

  def mapGenerator(numberOfItems: Int): Gen[Any, Map[String, Int]] = for {
    items <- Gen.listOfN(numberOfItems)(Gen.alphaNumericStringBounded(1,5))
    prices <- Gen.listOfN(numberOfItems)(Gen.int(500, 20000))
  } yield items.zip(prices).toMap

  val catalogueGen: Gen[Any, Catalogue] = for {
    version <- Gen.fromIterable(1 to 10)
    numberOfItems <- Gen.fromIterable(1 to 10)
    map <- mapGenerator(numberOfItems)
  } yield Catalogue(version, map)

  val routesUnderTest = Main.getCatalogueRouteWithETag.toRoutes

  def spec = suite("Testing the GET route")(

    test("when request DOES NOT contain header 'If-None-Match' - we always get back the actual content") {
      val req = Request.get(URL(Path.root./("catalogueWithETag")))
      check(catalogueGen)(catalogue =>
        for {
          ref <- Ref.make(catalogue)
          app = routesUnderTest.provideEnvironment(ZEnvironment(ref))
          response <- app.runZIO(req)
        } yield assertTrue(response.status == Ok && !response.body.isEmpty)
      )
    },
    test("when request DOES contain header 'If-None-Match' and it is the same as the Etag of the response - we get back 304 and empty body") {
      check(catalogueGen)(catalogue => {
        val req = Request.get(URL(Path.root./("catalogueWithETag"))).addHeader(Header.IfNoneMatch.parse(catalogue.version.toString).getOrElse(???))
        for {
          ref <- Ref.make(catalogue)
          app = routesUnderTest.provideEnvironment(ZEnvironment(ref))
          response <- app.runZIO(req)
        } yield assertTrue(response.status == Status.NotModified && response.body.isEmpty)
      })
    },
    test("when request DOES contain header 'If-None-Match' but it is different than the Etag of the response - we get back 200 and the actual body") {
      val req = Request.get(URL(Path.root./("catalogueWithETag"))).addHeader(Header.IfNoneMatch.parse("etag-that-is-different").getOrElse(???))
      check(catalogueGen)(catalogue =>
        for {
          ref <- Ref.make(catalogue)
          app = routesUnderTest.provideEnvironment(ZEnvironment(ref))
          response <- app.runZIO(req)
        } yield assertTrue(response.status == Status.Ok && !response.body.isEmpty)
      )
    }
  ) @@ TestAspect.sequential @@ TestAspect.samples(1000)
}
