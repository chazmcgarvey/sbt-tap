This sbt plug-in provides reporting of test success and failure for tests run by
[simple build tool](https://github.com/sbt/sbt)
in [TAP](http://search.cpan.org/perldoc?TAP%3A%3AParser%3A%3AGrammar) format.

All the test results will be generated in one file, the path of which may be
specified in the `SBT_TAP_OUTPUT` environment variable.  If unspecified in the
environment, the output file path defaults to `test-results/test.tap`.

To use:

1. Add this plug-in to your sbt project by creating a file
   `project/project/Plugins.scala` that looks something like this:

   ```scala
   import sbt._
   // sets up other project dependencies when building our root project
   object Plugins extends Build {
     lazy val root = Project("root", file(".")) dependsOn(tapListener)
     lazy val tapListener = RootProject(uri("git://github.com/mkhettry/sbt-tap.git"))
   }
   ```

2. In your `build.sbt` file, add the `SbtTapListener` to the sequence of test
   listeners.

   ```scala
   testListeners += SbtTapReporting()
   ```

3. Optionally, in a UNIX environment, you can set up a named pipe for
   collecting the TAP report, for your test harness.

   ```sh
   #!/bin/sh

   pipe="$PWD/test.tap"    # set where to make the pipe

   rm -f "$pipe"           # clear the path for the new pipe
   mkfifo "$pipe"          # make the pipe
   cat "$pipe" &           # redirect the report to stdout

   SBT_TAP_OUTPUT="$pipe" sbt test 2>&1 >/dev/null

   rm -f "$pipe"           # all done - remove the pipe
   ```
