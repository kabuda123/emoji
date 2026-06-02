import Foundation
import XCTest
@testable import EmojiApp

final class APIClientTests: XCTestCase {
    func testGetDecodesSuccessfulEnvelope() async throws {
        let transport = MockHTTPTransport(
            data: jsonData("""
            {
              "success": true,
              "data": { "value": "ok" },
              "error": null,
              "traceId": "trace-1",
              "timestamp": "2026-05-26T00:00:00Z"
            }
            """),
            statusCode: 200
        )
        let client = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)

        let response: SampleResponse = try await client.get(APIEndpoint.templates)

        XCTAssertEqual(response.value, "ok")
        XCTAssertEqual(transport.requests.first?.httpMethod, "GET")
        XCTAssertEqual(transport.requests.first?.url?.absoluteString, "http://localhost:8080/api/templates")
    }

    func testPostAddsAuthorizationAndCustomHeaders() async throws {
        let transport = MockHTTPTransport(
            data: jsonData("""
            {
              "success": true,
              "data": { "value": "created" },
              "error": null,
              "traceId": null,
              "timestamp": "2026-05-26T00:00:00Z"
            }
            """),
            statusCode: 200
        )
        let client = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)
        client.accessTokenProvider = { "access-token" }

        let response: SampleResponse = try await client.post(
            APIEndpoint.generations,
            body: SampleRequest(value: "input"),
            headers: ["Idempotency-Key": "idem-1"]
        )

        let request = try XCTUnwrap(transport.requests.first)
        XCTAssertEqual(response.value, "created")
        XCTAssertEqual(request.httpMethod, "POST")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access-token")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Idempotency-Key"), "idem-1")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "application/json")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Accept"), "application/json")
    }

    func testServerErrorEnvelopeThrowsPayload() async throws {
        let transport = MockHTTPTransport(
            data: jsonData("""
            {
              "success": false,
              "data": null,
              "error": {
                "code": "INSUFFICIENT_CREDITS",
                "message": "Not enough credits",
                "details": { "required": "2" }
              },
              "traceId": "trace-2",
              "timestamp": "2026-05-26T00:00:00Z"
            }
            """),
            statusCode: 400
        )
        let client = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)

        do {
            let _: SampleResponse = try await client.get(APIEndpoint.creditBalance)
            XCTFail("Expected server error")
        } catch APIClientError.server(let payload) {
            XCTAssertEqual(payload.code, "INSUFFICIENT_CREDITS")
            XCTAssertEqual(payload.message, "Not enough credits")
            XCTAssertEqual(payload.details?["required"], "2")
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testSuccessfulEnvelopeWithoutDataThrowsMissingData() async throws {
        let transport = MockHTTPTransport(
            data: jsonData("""
            {
              "success": true,
              "data": null,
              "error": null,
              "traceId": null,
              "timestamp": "2026-05-26T00:00:00Z"
            }
            """),
            statusCode: 200
        )
        let client = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)

        do {
            let _: SampleResponse = try await client.get(APIEndpoint.templates)
            XCTFail("Expected missing data error")
        } catch APIClientError.missingData {
            XCTAssertTrue(true)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }

    func testDecodesISO8601DatesInPayload() async throws {
        let transport = MockHTTPTransport(
            data: jsonData("""
            {
              "success": true,
              "data": { "createdAt": "2026-05-26T08:30:00Z" },
              "error": null,
              "traceId": null,
              "timestamp": "2026-05-26T00:00:00Z"
            }
            """),
            statusCode: 200
        )
        let client = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)

        let response: DateResponse = try await client.get(APIEndpoint.history)

        let expectedDate = ISO8601DateFormatter().date(from: "2026-05-26T08:30:00Z")
        XCTAssertEqual(response.createdAt, expectedDate)
    }
}

private final class MockHTTPTransport: HTTPTransport {
    private let data: Data
    private let statusCode: Int
    private(set) var requests: [URLRequest] = []

    init(data: Data, statusCode: Int) {
        self.data = data
        self.statusCode = statusCode
    }

    func data(for request: URLRequest) async throws -> (Data, URLResponse) {
        requests.append(request)
        let response = HTTPURLResponse(
            url: request.url!,
            statusCode: statusCode,
            httpVersion: nil,
            headerFields: nil
        )!
        return (data, response)
    }
}

private struct SampleRequest: Encodable {
    let value: String
}

private struct SampleResponse: Decodable {
    let value: String
}

private struct DateResponse: Decodable {
    let createdAt: Date
}

private func jsonData(_ string: String) -> Data {
    Data(string.utf8)
}
