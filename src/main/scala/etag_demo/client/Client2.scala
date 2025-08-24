package etag_demo.client

import etag_demo.common.Catalogue
import etag_demo.common.EndpointDefinitions.{getCatalogueWithETag, reset}
import zio.http.Header.ETag
import zio.http.endpoint.EndpointExecutor
import zio.http.{Client, Header}
import zio.{NonEmptyChunk, Ref, Scope, ZIO, ZIOAppDefault}

object Client2 extends ZIOAppDefault with EndpointLocatorResolver {

  override def run: ZIO[Scope, Unit, Unit] = (for {
    endpointLocator <- resolveEndpointLocator
    client <- ZIO.service[Client]
    executor = EndpointExecutor(client, endpointLocator)

    _ <- ZIO.log("calling reset endpoint")
    _ <- executor(reset())

    catalogueRef <- Ref.make[Option[Catalogue]](None)
    noOfBandwidthWastingCalls <- Ref.make[Int](0)
    noOfTimesWeHaveSavedSomeBandwidth <- Ref.make[Int](0)
    etagRef <- Ref.make[Option[ETag]](None)

    _ <- ZIO.foreachDiscard(1 to 250)(_ => for {
      currentEtag <- etagRef.get
      ifNoneMatchHeader: Option[Header.IfNoneMatch] = currentEtag match {
        case None => None
        case Some(etag) =>
          val renderedValue = etag.renderedValue
          val valueToUse = renderedValue.substring(1, renderedValue.length - 1)
          Some(Header.IfNoneMatch.ETags(NonEmptyChunk(valueToUse)))
      }
      _ <- ZIO.log("the header we are about to send: " + ifNoneMatchHeader)

      latestCatalogue <- executor(getCatalogueWithETag(ifNoneMatchHeader))
      _ <- ZIO.log("The latest catalogue retrieved: " + latestCatalogue)
      currentCatalogue <- catalogueRef.get
      _ <- (currentCatalogue, latestCatalogue) match {
        case (None, Left(_)) => for {
          _ <- ZIO.logError("This should never happen...")
          _ <- ZIO.fail(())
        } yield ()
        case (Some(_), Left(_)) => for {
          _ <- ZIO.log(s"\n\tLatest version is [${currentEtag.get}], but since we already have that server responded with 304...\n")
          _ <- noOfTimesWeHaveSavedSomeBandwidth.update(_ + 1)
        } yield ()
        case (None, Right((retrievedCatalogue, etag))) => for {
          _ <- ZIO.log("\n\tWe have just retrieved the catalogue for the first time!\n" + retrievedCatalogue)
          _ <- catalogueRef.set(Some(retrievedCatalogue))
          _ <- etagRef.set(Some(etag))
        } yield ()
        case (Some(_), Right((_, etag))) if etag == currentEtag.get => for {
          _ <- ZIO.log("\n\tWe have retrieved the latest catalogue but it seems we already had that version...\n")
          _ <- noOfBandwidthWastingCalls.update(_ + 1)
        } yield ()
        case (Some(_), Right((retrievedCatalogue, etag))) => for {
          _ <- ZIO.log("\n\tWe have retrieved a new catalogue!\n" + retrievedCatalogue)
          _ <- catalogueRef.set(Some(retrievedCatalogue))
          _ <- etagRef.set(Some(etag))
        } yield ()
      }
      _ <- ZIO.sleep(zio.Duration.fromMillis(50))
    } yield ())

    noOfBandwidthWastingCallsAsNumber <- noOfBandwidthWastingCalls.get
    noOfTimesWeHaveSavedSomeBandwidthAsNumber <- noOfTimesWeHaveSavedSomeBandwidth.get
    _ <- ZIO.log(s"\n\tWe have executed $noOfBandwidthWastingCallsAsNumber bandwidth wasting calls.")
    _ <- ZIO.log(s"\n\tWe have saved some bandwidth $noOfTimesWeHaveSavedSomeBandwidthAsNumber times.")
  } yield ()).provideSome(
    Client.default.orDie
  )
}
