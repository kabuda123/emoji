import Foundation
import XCTest
@testable import EmojiApp

final class UploadFlowTests: XCTestCase {
    func testUploadRequestsPolicyThenPutsImageDataAndReturnsObjectKey() async throws {
        let policyTransport = RecordingHTTPTransport(
            responses: [
                .init(
                    data: jsonData("""
                    {
                      "success": true,
                      "data": {
                        "objectKey": "emoji/user/input.png",
                        "uploadUrl": "https://upload.example.com/object",
                        "method": "PUT",
                        "headers": {
                          "Content-Type": "image/png",
                          "x-upload-token": "token-1"
                        },
                        "expiresInSeconds": 600
                      },
                      "error": null,
                      "traceId": "trace-upload",
                      "timestamp": "2026-05-26T00:00:00Z"
                    }
                    """),
                    statusCode: 200
                )
            ]
        )
        let uploadTransport = RecordingHTTPTransport(
            responses: [
                .init(data: Data(), statusCode: 200)
            ]
        )
        let apiClient = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: policyTransport)
        let service = ImageUploadService(apiClient: apiClient, uploadTransport: uploadTransport)

        let result = try await service.uploadImage(
            ImageUploadInput(
                data: Data([0x01, 0x02, 0x03]),
                fileName: "input.png",
                contentType: "image/png"
            )
        )

        let policyRequest = try XCTUnwrap(policyTransport.requests.first)
        let policyBody = try decodeJSONBody(policyRequest)
        XCTAssertEqual(result.objectKey, "emoji/user/input.png")
        XCTAssertEqual(policyRequest.httpMethod, "POST")
        XCTAssertEqual(policyRequest.url?.absoluteString, "http://localhost:8080/api/upload/policy")
        XCTAssertEqual(policyBody["fileName"] as? String, "input.png")
        XCTAssertEqual(policyBody["contentType"] as? String, "image/png")

        let uploadRequest = try XCTUnwrap(uploadTransport.requests.first)
        XCTAssertEqual(uploadRequest.httpMethod, "PUT")
        XCTAssertEqual(uploadRequest.url?.absoluteString, "https://upload.example.com/object")
        XCTAssertEqual(uploadRequest.value(forHTTPHeaderField: "Content-Type"), "image/png")
        XCTAssertEqual(uploadRequest.value(forHTTPHeaderField: "x-upload-token"), "token-1")
        XCTAssertEqual(uploadRequest.httpBody, Data([0x01, 0x02, 0x03]))
    }

    func testUploadThrowsWhenPutFails() async throws {
        let policyTransport = RecordingHTTPTransport(
            responses: [
                .init(
                    data: jsonData("""
                    {
                      "success": true,
                      "data": {
                        "objectKey": "emoji/user/input.png",
                        "uploadUrl": "https://upload.example.com/object",
                        "method": "PUT",
                        "headers": {},
                        "expiresInSeconds": 600
                      },
                      "error": null,
                      "traceId": "trace-upload",
                      "timestamp": "2026-05-26T00:00:00Z"
                    }
                    """),
                    statusCode: 200
                )
            ]
        )
        let uploadTransport = RecordingHTTPTransport(
            responses: [
                .init(data: Data(), statusCode: 500)
            ]
        )
        let apiClient = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: policyTransport)
        let service = ImageUploadService(apiClient: apiClient, uploadTransport: uploadTransport)

        do {
            _ = try await service.uploadImage(
                ImageUploadInput(
                    data: Data([0x01]),
                    fileName: "input.png",
                    contentType: "image/png"
                )
            )
            XCTFail("Expected upload failure")
        } catch ImageUploadError.uploadFailed(let statusCode) {
            XCTAssertEqual(statusCode, 500)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }
}

private final class RecordingHTTPTransport: HTTPTransport {
    struct Response {
        let data: Data
        let statusCode: Int
    }

    private var responses: [Response]
    private(set) var requests: [URLRequest] = []

    init(responses: [Response]) {
        self.responses = responses
    }

    func data(for request: URLRequest) async throws -> (Data, URLResponse) {
        requests.append(request)
        let response = responses.removeFirst()
        let httpResponse = HTTPURLResponse(
            url: request.url!,
            statusCode: response.statusCode,
            httpVersion: nil,
            headerFields: nil
        )!
        return (response.data, httpResponse)
    }
}

private func decodeJSONBody(_ request: URLRequest) throws -> [String: Any] {
    let body = try XCTUnwrap(request.httpBody)
    return try XCTUnwrap(JSONSerialization.jsonObject(with: body) as? [String: Any])
}

private func jsonData(_ string: String) -> Data {
    Data(string.utf8)
}
