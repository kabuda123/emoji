import XCTest
@testable import EmojiApp

final class EmojiAppTests: XCTestCase {
    @MainActor
    func testAppEnvironmentStartsWithoutSessionOrBootstrapConfig() {
        let environment = AppEnvironment()

        XCTAssertNil(environment.bootstrapConfig)
        XCTAssertFalse(environment.isLoadingBootstrap)
        XCTAssertNil(environment.bootstrapErrorMessage)
        XCTAssertNil(environment.sessionStore.currentUserId)
    }
}
