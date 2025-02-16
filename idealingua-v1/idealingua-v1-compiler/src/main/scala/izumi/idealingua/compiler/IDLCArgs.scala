package izumi.idealingua.compiler

import java.io.File
import java.nio.file.{Path, Paths}

import izumi.fundamentals.platform.cli.model.raw.RawEntrypointParams
import izumi.fundamentals.platform.cli.model.schema.{GlobalArgsSchema, ParserDef, ParserSchema, ParserSchemaFormatter, RoleParserSchema}
import izumi.fundamentals.platform.cli.{CLIParser, ParserFailureHandler}

case class LanguageOpts(
                         id: String,
                         withRuntime: Boolean,
                         manifest: Option[File],
                         credentials: Option[File],
                         extensions: List[String],
                         overrides: Map[String, String],
                       )


case class IDLCArgs(
                     source: Path,
                     overlay: Path,
                     target: Path,
                     languages: List[LanguageOpts],
                     init: Option[Path],
                     versionOverlay: Option[Path],
                     overrides: Map[String, String],
                     publish: Boolean = false
                   )

object IDLCArgs {
  def default: IDLCArgs = IDLCArgs(
    Paths.get("source")
    , Paths.get("overlay")
    , Paths.get("target")
    , List.empty
    , None
    , None
    , Map.empty
  )

  object P extends ParserDef {
    final val sourceDir = arg("source", "s", "source directory", "<path>")
    final val targetDir = arg("target", "t", "target directory", "<path>")
    final val overlayDir = arg("overlay", "o", "overlay directory", "<path>")
    final val overlayVersionFile = arg("overlay-version", "v", "version file", "<path>")
    final val define = arg("define", "d", "define value", "const.name=value")
    final val publish = flag("publish", "p", "build and publish generated code")


  }

  object LP extends ParserDef {
    final val manifest = arg("manifest", "m", "manifest file", "<path>")
    final val credentials = arg("credentials", "cr", "credentials file", "<path>")
    final val extensionSpec = arg("extensions", "e", "extensions spec", "{* | -AnyvalExtension;-CirceDerivationTranslatorExtension}")
    final val noRuntime = flag("disable-runtime", "nr", "don't include builtin runtime")
    final val define = arg("define", "d", "define value", "const.name=value")
  }

  object IP extends ParserDef {}

  def parseUnsafe(args: Array[String]): IDLCArgs = {
    val parsed = new CLIParser().parse(args) match {
      case Left(value) =>
        ParserFailureHandler.TerminatingHandler.onParserError(value)
      case Right(value) =>
        value
    }

    val globalArgs = GlobalArgsSchema(P, None, None)

    val roleHelp = ParserSchemaFormatter.makeDocs(ParserSchema(globalArgs, Seq(
      RoleParserSchema("init", IP, Some("setup project template. Invoke as :init <path>"), None, freeArgsAllowed = true),
      RoleParserSchema("scala", LP, Some("scala target"), None, freeArgsAllowed = false),
      RoleParserSchema("go", LP, Some("go target"), None, freeArgsAllowed = false),
      RoleParserSchema("csharp", LP, Some("C#/Unity target"), None, freeArgsAllowed = false),
      RoleParserSchema("typescript", LP, Some("Typescript target"), None, freeArgsAllowed = false),
    )))

    if (parsed.roles.isEmpty || parsed.roles.exists(_.role == "help")) {
      println(roleHelp)
    }

    val init = parsed.roles.find(_.role == "init").map {
      r =>
        new File(r.freeArgs.head).toPath
    }

    val parameters = parsed.globalParameters
    val src = P.sourceDir.findValue(parameters).asPath.getOrElse(Paths.get("./source"))
    val target = P.targetDir.findValue(parameters).asPath.getOrElse(Paths.get("./target"))
    assert(src.toFile.getCanonicalPath != target.toFile.getCanonicalPath)
    val overlay = P.overlayDir.findValue(parameters).asPath.getOrElse(Paths.get("./overlay"))
    val overlayVersion = P.overlayVersionFile.findValue(parameters).asPath
    val publish = P.publish.hasFlag(parameters)
    val defines = parseDefs(parameters)

    val internalRoles = Seq("init", "help")

    val languages = parsed.roles.filterNot(r => internalRoles.contains(r.role)).map {
      role =>
        val parameters = role.roleParameters
        val runtime = !LP.noRuntime.hasFlag(parameters)
        val manifest = LP.manifest.findValue(parameters).asFile
        val credentials = LP.credentials.findValue(parameters).asFile
        val defines = parseDefs(parameters)
        val extensions = LP.extensionSpec.findValue(parameters).map(_.value.split(',')).toList.flatten

        LanguageOpts(
          role.role,
          runtime,
          manifest,
          credentials,
          extensions,
          defines
        )
    }

    IDLCArgs(
      src,
      overlay,
      target,
      languages.toList,
      init,
      overlayVersion,
      defines,
      publish
    )
  }

  private def parseDefs(parameters: RawEntrypointParams) = {
    LP.define.findValues(parameters).map {
      v =>
        val parts = v.value.split('=')
        parts.head -> parts.tail.mkString("=")
    }.toMap
  }

}
