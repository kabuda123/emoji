import Foundation

struct APIErrorPayload: Decodable, Error {
    let code: String
    let message: String
    let details: [String: String]?
}

enum APIClientError: LocalizedError {
    case invalidURL
    case invalidResponse
    case missingData
    case server(APIErrorPayload)
    case transport(Error)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "The request URL is invalid."
        case .invalidResponse:
            return "The server response is invalid."
        case .missingData:
            return "The server returned no payload."
        case .server(let payload):
            return payload.message
        case .transport(let error):
            return error.localizedDescription
        }
    }
}