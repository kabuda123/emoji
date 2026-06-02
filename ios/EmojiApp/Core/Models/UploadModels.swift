import Foundation

struct UploadPolicyRequest: Encodable {
    let fileName: String
    let contentType: String
}

struct UploadPolicy: Decodable {
    let objectKey: String
    let uploadUrl: String
    let method: String
    let headers: [String: String]
    let expiresInSeconds: Int
}