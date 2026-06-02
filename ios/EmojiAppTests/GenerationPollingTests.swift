import Foundation
import XCTest
@testable import EmojiApp

final class GenerationPollingTests: XCTestCase {
    func testPollContinuesUntilTerminalStatus() async throws {
        let transport = RecordingHTTPTransport(
            responses: [
                .init(
                    data: jsonData("""
                    {
                      "success": true,
                      "data": {
                        "taskId": "task-1",
                        "status": "RUNNING",
                        "progressPercent": 45,
                        "previewUrls": ["https://cdn.example.com/preview.png"],
                        "resultUrls": [],
                        "failedReason": null,
                        "pollAfterSeconds": 3
                      },
                      "error": null,
                      "traceId": "trace-poll-1",
                      "timestamp": "2026-05-26T00:00:00Z"
                    }
                    """),
                    statusCode: 200
                ),
                .init(
                    data: jsonData("""
                    {
                      "success": true,
                      "data": {
                        "taskId": "task-1",
                        "status": "SUCCESS",
                        "progressPercent": 100,
                        "previewUrls": [],
                        "resultUrls": ["https://cdn.example.com/result.png"],
                        "failedReason": null,
                        "pollAfterSeconds": 3
                      },
                      "error": null,
                      "traceId": "trace-poll-2",
                      "timestamp": "2026-05-26T00:00:00Z"
                    }
                    """),
                    statusCode: 200
                )
            ]
        )
        let client = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)
        var requestedDelays: [Int] = []
        let service = GenerationPollingService(apiClient: client) { seconds in
            requestedDelays.append(seconds)
        }

        var updates: [GenerationDetail] = []
        try await service.poll(taskID: "task-1", initialPollAfterSeconds: 1) { detail in
            updates.append(detail)
        }

        XCTAssertEqual(requestedDelays, [1, 3])
        XCTAssertEqual(transport.requests.compactMap { $0.url?.path }, ["/api/generations/task-1", "/api/generations/task-1"])
        XCTAssertEqual(updates.map(\.status), [.running, .success])
        XCTAssertTrue(GenerationStatus.success.isTerminal)
        XCTAssertFalse(GenerationStatus.running.isTerminal)
    }

    func testPollStopsAfterFirstTerminalStatus() async throws {
        let transport = RecordingHTTPTransport(
            responses: [
                .init(
                    data: jsonData("""
                    {
                      "success": true,
                      "data": {
                        "taskId": "task-2",
                        "status": "FAILED",
                        "progressPercent": 60,
                        "previewUrls": [],
                        "resultUrls": [],
                        "failedReason": "moderation failed",
                        "pollAfterSeconds": 10
                      },
                      "error": null,
                      "traceId": "trace-poll-3",
                      "timestamp": "2026-05-26T00:00:00Z"
                    }
                    """),
                    statusCode: 200
                )
            ]
        )
        let client = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)
        var requestedDelays: [Int] = []
        let service = GenerationPollingService(apiClient: client) { seconds in
            requestedDelays.append(seconds)
        }

        var updates: [GenerationDetail] = []
        try await service.poll(taskID: "task-2", initialPollAfterSeconds: 5) { detail in
            updates.append(detail)
        }

        XCTAssertEqual(requestedDelays, [5])
        XCTAssertEqual(transport.requests.count, 1)
        XCTAssertEqual(updates.first?.status, .failed)
        XCTAssertEqual(updates.first?.failedReason, "moderation failed")
    }

    func testPollCanBeCancelledBeforeNextRequest() async throws {
        let sleepStarted = expectation(description: "sleep started")
        let transport = RecordingHTTPTransport(responses: [])
        let client = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)
        let service = GenerationPollingService(apiClient: client) { _ in
            sleepStarted.fulfill()
            while !Task.isCancelled {
                try await Task.sleep(nanoseconds: 1_000_000)
            }
            throw CancellationError()
        }

        let pollingTask = Task {
            try await service.poll(taskID: "task-3", initialPollAfterSeconds: 10) { _ in }
        }

        await fulfillment(of: [sleepStarted], timeout: 1)
        pollingTask.cancel()

        do {
            try await pollingTask.value
            XCTFail("Expected polling task cancellation")
        } catch is CancellationError {
            XCTAssertTrue(true)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }

        XCTAssertTrue(transport.requests.isEmpty)
    }

    func testPollPropagatesAPIErrors() async throws {
        let transport = RecordingHTTPTransport(
            responses: [
                .init(
                    data: jsonData("""
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": "GENERATION_NOT_FOUND",
                        "message": "Generation task not found",
                        "details": null
                      },
                      "traceId": "trace-poll-4",
                      "timestamp": "2026-05-26T00:00:00Z"
                    }
                    """),
                    statusCode: 404
                )
            ]
        )
        let client = APIClient(baseURL: URL(string: "http://localhost:8080")!, transport: transport)
        var requestedDelays: [Int] = []
        let service = GenerationPollingService(apiClient: client) { seconds in
            requestedDelays.append(seconds)
        }

        do {
            try await service.poll(taskID: "missing-task", initialPollAfterSeconds: 1) { _ in }
            XCTFail("Expected API error")
        } catch APIClientError.server(let payload) {
            XCTAssertEqual(payload.code, "GENERATION_NOT_FOUND")
        } catch {
            XCTFail("Unexpected error: \(error)")
        }

        XCTAssertEqual(requestedDelays, [1])
        XCTAssertEqual(transport.requests.count, 1)
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

private func jsonData(_ string: String) -> Data {
    Data(string.utf8)
}
