import java.io.{PrintWriter, StringWriter, File, FileWriter}
import sbt._
import org.scalatools.testing.{Event => TEvent, Result => TResult}

import java.util.concurrent.atomic.AtomicInteger

object SbtTapReporting extends Plugin {
  def apply() = new SbtTapListener
}

/**
 * Listens to sbt test listener events and writes them to a tap compatible file. Results for all groups
 * go to a single file although it might be desirable to generate one tap file per group.
 * <p>
 * sbt runs tests in parallel and the protocol does not seem to provide a way to match a group to a test event. It
 * does look line one thread calls startGroup/testEvent/endGroup sequentially and using thread local to keep
 * the current active group might be one way to go.
 */
class SbtTapListener extends TestsListener {
  var testId = new AtomicInteger(0)
  var fileWriter: FileWriter = _

  override def doInit = {
    val filename = scala.util.Properties.envOrElse("SBT_TAP_OUTPUT", "test-results/test.tap")
    val file = new File(filename)
    new File(file.getParent).mkdirs
    fileWriter = new FileWriter(file)
    writeTap("TAP", "version", 13)
  }

  def startGroup(name: String) =
    writeTapDiag("start", name)

  def endGroup(name: String, result: TestResult.Value) =
    writeTapDiag("end", name, "with result", result.toString.toLowerCase)

  def endGroup(name: String, t: Throwable) = {
    writeTapDiag("end", name)
    writeTapDiag(stackTraceForError(t))
  }

  def testEvent(event: TestEvent) = {
    event.detail.foreach { e: TEvent =>
      val description = e.testName.replaceAll("#", "%23")
      e.result match {
        case TResult.Success =>
          writeTap("ok", testId.incrementAndGet, "-", description)
        case TResult.Skipped =>
          writeTap("ok", testId.incrementAndGet, "-", description, "# SKIP")
        case TResult.Error | TResult.Failure =>
          writeTap("not ok", testId.incrementAndGet, "-", description)
          // TODO: It would be nice if we could report the exact line in the test where this happened.
          writeTapDiag(stackTraceForError(e.error))
      }
    }
  }

  override def doComplete(finalResult: TestResult.Value) = {
    writeTap("1.." + testId.get)
    fileWriter.close
  }

  private def writeTap(s: Any*) = {
    fileWriter.write(s.mkString("", " ", "\n"))
    fileWriter.flush
  }

  private def writeTapDiag(s: Any*) =
    writeTap("#", s.mkString("", " ", "\n").trim.replaceAll("\\n", "\n# "))

  private def stackTraceForError(t: Throwable): String = {
    val sw = new StringWriter
    val printWriter = new PrintWriter(sw)
    t.printStackTrace(printWriter)
    sw.toString
  }
}
