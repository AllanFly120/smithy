$version: "2.0"

namespace com.foo

@requestCompression(
    encodings: ["brotli", "gzip", "custom"]
)
operation RequestCompressionOperation {
    input := {
        @required
        member: StreamingBlob
    }
}

@streaming
blob StreamingBlob
