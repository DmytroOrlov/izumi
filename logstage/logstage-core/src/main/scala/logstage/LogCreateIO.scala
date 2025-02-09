package logstage

import izumi.functional.mono.SyncSafe
import izumi.fundamentals.reflection.CodePositionMaterializer
import izumi.logstage.api.Log.{Context, CustomContext, Entry, Message}

trait LogCreateIO[F[_]] {
  def createEntry(logLevel: Level, message: Message)(implicit pos: CodePositionMaterializer): F[Entry]
  def createContext(logLevel: Level, customContext: CustomContext)(implicit pos: CodePositionMaterializer): F[Context]
}

object LogCreateIO {
  def apply[F[_]: LogCreateIO]: LogCreateIO[F] = implicitly

  implicit def logCreateIOSyncSafeInstance[F[_]: SyncSafe]: LogCreateIO[F] = new LogCreateIOSyncSafeInstance[F](SyncSafe[F])

  class LogCreateIOSyncSafeInstance[F[_]](protected val F: SyncSafe[F]) extends LogCreateIO[F] {
    override def createEntry(logLevel: Level, message: Message)(implicit pos: CodePositionMaterializer): F[Entry] = {
      F.syncSafe(Entry.create(logLevel, message)(pos))
    }

    override def createContext(logLevel: Level, customContext: CustomContext)(implicit pos: CodePositionMaterializer): F[Context] = {
      F.syncSafe(Context.recordContext(logLevel, customContext)(pos))
    }
  }
}
