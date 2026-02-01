package etag_demo.server

import etag_demo.common.{Catalogue, EndpointDefinitions}
import zio.http.Header.ETag.Strong
import zio.http.endpoint.openapi.{OpenAPI, OpenAPIGen, SwaggerUI}
import zio.http.{Route, Routes, Server}
import zio.{&, Duration, Fiber, Ref, Scope, ZEnvironment, ZIO, ZIOAppDefault}

object Main extends ZIOAppDefault {

  override def run: ZIO[Scope, Throwable, Unit] = for {
    catalogueRef <- Ref.make(CatalogueGenerator.data(1))
    updateFiberId <- updateCatalogueEffect.provideEnvironment(ZEnvironment(catalogueRef)).fork
    fiberIdRef <- Ref.make(updateFiberId)
    routes = Routes(getCatalogueRoute, getCatalogueRouteWithETag, resetRoute)
      .provideEnvironment(ZEnvironment(catalogueRef).add(fiberIdRef)) ++ openAPIRoute
    _ <- Server.serve(routes).provide(Server.default)
    _ <- ZIO.never
  } yield ()

  private[server] val getCatalogueRoute: Route[Ref[Catalogue], Nothing] = EndpointDefinitions.getCatalogue.implement(_ =>
    for {
      ref <- ZIO.service[Ref[Catalogue]]
      currentCatalogue <- ref.get
    } yield currentCatalogue
  )

  private[server] val getCatalogueRouteWithETag: Route[Ref[Catalogue], Nothing] = EndpointDefinitions.getCatalogueWithETag.implement(nonMatchHeader =>
    for {
      ifNonMatchValue <- nonMatchHeader match {
        case Some(headerValue) =>
          zio.Console.printLine("If-None-Match header is present: " + headerValue).orDie.as(Some(headerValue.renderedValue))
        case _ => zio.Console.printLine("If-None-Match header is not present.").orDie.as(None)
      }
      ref <- ZIO.service[Ref[Catalogue]]
      currentCatalogue <- ref.get
      s304 = ifNonMatchValue.contains(currentCatalogue.version.toString)
      _ <- zio.Console.printLine("s304: " + s304).orDie
      etag = Strong(currentCatalogue.version.toString)
      toReturn = if (s304) Left(etag) else Right((currentCatalogue, etag))
      _ <- zio.Console.printLine("About to reply with: " + toReturn).orDie
    } yield toReturn
  )

  private val resetRoute: Route[Ref[Fiber.Runtime[Nothing, Unit]] & Ref[Catalogue], Nothing] = EndpointDefinitions.reset.implement(_ => for {
    _ <- zio.Console.printLine("\t\t\t... reset ...").orDie
    fiberIdRef <- ZIO.service[Ref[zio.Fiber.Runtime[Nothing, Unit]]]
    catalogueRef <- ZIO.service[Ref[Catalogue]]
    _ <- fiberIdRef.get.flatMap(_.interrupt)
    newFiberId <- updateCatalogueEffect.provideEnvironment(ZEnvironment(catalogueRef)).forkDaemon
    _ <- fiberIdRef.set(newFiberId)
  } yield ())

  private val openAPI: OpenAPI = OpenAPIGen.fromEndpoints(title = "ETag Example", version = "1.0",
    EndpointDefinitions.reset,
    EndpointDefinitions.getCatalogue,
    EndpointDefinitions.getCatalogueWithETag
  )

 private val openAPIRoute = SwaggerUI.routes("docs", openAPI)

  private val updateCatalogueEffect: ZIO[Ref[Catalogue], Nothing, Unit] = for {
    catalogueRef <- ZIO.service[Ref[Catalogue]]
    _ <- ZIO.foreachDiscard(1 to CatalogueGenerator.size) {
      round =>
        for {
          _ <- catalogueRef.set(CatalogueGenerator.data(round))
          _ <- ZIO.sleep(Duration.fromMillis(500))
        } yield ()
    }
  } yield ()
}
