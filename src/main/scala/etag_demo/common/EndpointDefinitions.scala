package etag_demo.common

import zio.ZNothing
import zio.http.codec.{Doc, HeaderCodec, HttpCodec, StatusCodec}
import zio.http.endpoint.AuthType.None
import zio.http.endpoint.Endpoint
import zio.http.{Header, RoutePattern}

object EndpointDefinitions {

  val exampleCatalogue =
    Catalogue(
      77,
      Map(
        "Fish and Chips" -> 2200,
        "Sushi Platter" -> 4000,
        "Tonkotsu Ramen" -> 2500,
        "Chicken Curry" -> 1900,
        "Beef Tacos" -> 1200,
      )
    )

  val getCatalogue: Endpoint[Unit, Unit, ZNothing, Catalogue, None] =
    Endpoint((RoutePattern.GET / "catalogue") ?? Doc.p("Retrieves the catalogue"))
      .outCodec(StatusCodec.Ok ++ HttpCodec.content[Catalogue].examples(("example1" , exampleCatalogue)))

  val getCatalogueWithETag: Endpoint[Unit, Option[Header.IfNoneMatch], ZNothing, Either[Header.ETag, (Catalogue, Header.ETag)], None] =
    Endpoint((RoutePattern.GET / "catalogueWithETag") ?? Doc.p("Retrieves the catalogue - with ETag support"))
      .inCodec(HeaderCodec.ifNoneMatch.optional)
      .outCodec(
        StatusCodec.Ok ++
        HttpCodec.content[Catalogue].examples(("example1" , exampleCatalogue)) ++
        HttpCodec.header(Header.ETag).examples(("example1", Header.ETag.Strong("77")))
      )
      .outCodec(StatusCodec.NotModified ++ HttpCodec.header(Header.ETag).examples(("example1", Header.ETag.Strong("77"))))

  val reset: Endpoint[Unit, Unit, ZNothing, Unit, None] =
    Endpoint((RoutePattern.POST / "reset") ?? Doc.p("Resets the catalogue generation"))
      .out[Unit].outCodec[Unit](HttpCodec.empty)

}
