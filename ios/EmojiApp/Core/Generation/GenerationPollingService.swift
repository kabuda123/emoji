import Foundation

final class GenerationPollingService {
    typealias SleepAction = (Int) async throws -> Void
    typealias UpdateHandler = (GenerationDetail) async -> Void

    private let apiClient: APIClient
    private let sleep: SleepAction

    init(
        apiClient: APIClient,
        sleep: @escaping SleepAction = GenerationPollingService.defaultSleep
    ) {
        self.apiClient = apiClient
        self.sleep = sleep
    }

    func poll(
        taskID: String,
        initialPollAfterSeconds: Int,
        onUpdate: @escaping UpdateHandler
    ) async throws {
        var nextPollAfterSeconds = Self.normalizedDelay(initialPollAfterSeconds)

        while !Task.isCancelled {
            try await sleep(nextPollAfterSeconds)
            try Task.checkCancellation()

            let detail: GenerationDetail = try await apiClient.get(APIEndpoint.generationDetail(taskID))
            await onUpdate(detail)

            if detail.status.isTerminal {
                return
            }

            nextPollAfterSeconds = Self.normalizedDelay(detail.pollAfterSeconds ?? nextPollAfterSeconds)
        }

        throw CancellationError()
    }

    private static func defaultSleep(_ seconds: Int) async throws {
        let nanoseconds = UInt64(normalizedDelay(seconds)) * 1_000_000_000
        try await Task.sleep(nanoseconds: nanoseconds)
    }

    private static func normalizedDelay(_ seconds: Int) -> Int {
        max(0, seconds)
    }
}
