// Parse error at line 10, column 9 near ` `: Unexpected shape type: with
$version: "2.0"

namespace smithy.example

@mixin
string MixinString

string MixedString
    with [MixinString]