dexdeps -- DEX external dependency dump


This tool dumps a list of fields and methods that a DEX file uses but does
not define.  When combined with a list of public APIs, it can be used to
determine whether an APK is accessing fields and calling methods that it
shouldn't be.  It may also be useful in determining whether an application
requires a certain minimum API level to execute.

Basic usage:

  dexdeps [options] <file.{dex,apk,jar}> ...

For zip archives (including .jar and .apk), dexdeps will look for a
"classes.dex" entry.

Supported options are:

  --format={brief,xml}

    Specifies the output format.

    "brief" produces one line of output for each field and method.  Field
    and argument types are shown as descriptor strings.

    "xml" produces a larger output file, readable with an XML browser.  Types
    are shown in a more human-readable form (e.g. "[I" becomes "int[]").

  --just-classes

    Indicates that output should only include a list of classes, as
    opposed to also listing fields and methods.
