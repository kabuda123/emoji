import Foundation
import Security
import XCTest
@testable import EmojiApp

@MainActor
final class SessionStoreTests: XCTestCase {
    func testUpdateSavesSessionAndLoginMethod() {
        let persistence = InMemorySessionPersistence()
        let store = SessionStore(persistence: persistence, now: fixedNow)
        let session = makeSession(expiresIn: 3600)

        store.update(session: session, loginMethod: .email)

        XCTAssertEqual(store.currentSession?.userId, "user-1")
        XCTAssertEqual(store.currentLoginMethod, .email)
        XCTAssertEqual(store.sessionExpiresAt, fixedNow().addingTimeInterval(3600))
        XCTAssertEqual(persistence.storedSession?.session.userId, "user-1")
        XCTAssertEqual(persistence.storedSession?.loginMethod, .email)
    }

    func testInitializationRestoresPersistedSession() {
        let persistence = InMemorySessionPersistence(
            storedSession: StoredAuthSession(
                session: makeSession(expiresIn: 120),
                loginMethod: .apple,
                savedAt: fixedNow()
            )
        )

        let store = SessionStore(persistence: persistence, now: fixedNow)

        XCTAssertEqual(store.currentSession?.accessToken, "access-token")
        XCTAssertEqual(store.currentLoginMethod, .apple)
        XCTAssertEqual(store.sessionExpiresAt, fixedNow().addingTimeInterval(120))
    }

    func testClearRemovesCurrentAndPersistedSession() {
        let persistence = InMemorySessionPersistence()
        let store = SessionStore(persistence: persistence, now: fixedNow)
        store.update(session: makeSession(expiresIn: 3600), loginMethod: .email)

        store.clear()

        XCTAssertNil(store.currentSession)
        XCTAssertNil(store.currentLoginMethod)
        XCTAssertNil(store.sessionExpiresAt)
        XCTAssertTrue(persistence.didClear)
        XCTAssertNil(persistence.storedSession)
    }

    func testInitializationIgnoresCorruptedStoredSession() {
        let persistence = InMemorySessionPersistence(loadError: TestPersistenceError.corruptedData)

        let store = SessionStore(persistence: persistence, now: fixedNow)

        XCTAssertNil(store.currentSession)
        XCTAssertNil(store.currentLoginMethod)
    }
}

final class KeychainSessionStoreTests: XCTestCase {
    private var storesToClear: [KeychainSessionStore] = []

    override func tearDown() {
        for store in storesToClear {
            try? store.clear()
        }
        storesToClear.removeAll()
        super.tearDown()
    }

    func testSaveLoadAndClearStoredSession() throws {
        let store = makeKeychainStore()
        let storedSession = StoredAuthSession(
            session: makeSession(expiresIn: 240),
            loginMethod: .apple,
            savedAt: fixedNow()
        )

        try store.save(storedSession: storedSession)

        let loadedSession = try XCTUnwrap(try store.loadStoredSession())
        XCTAssertEqual(loadedSession.session.userId, "user-1")
        XCTAssertEqual(loadedSession.session.accessToken, "access-token")
        XCTAssertEqual(loadedSession.loginMethod, .apple)
        XCTAssertEqual(loadedSession.savedAt, fixedNow())

        try store.clear()
        XCTAssertNil(try store.loadStoredSession())
    }

    private func makeKeychainStore() -> KeychainSessionStore {
        let store = KeychainSessionStore(
            service: "com.kabuda.emojiios.tests.\(UUID().uuidString)",
            account: "auth-session"
        )
        storesToClear.append(store)
        return store
    }
}

final class AppleSignInServiceTests: XCTestCase {
    func testBuildsLoginRequestFromIdentityTokenAndAuthorizationCodeData() throws {
        let service = AppleSignInService()

        let request = try service.makeLoginRequest(
            identityTokenData: Data("identity-token".utf8),
            authorizationCodeData: Data("authorization-code".utf8)
        )

        XCTAssertEqual(request.identityToken, "identity-token")
        XCTAssertEqual(request.authorizationCode, "authorization-code")
    }

    func testRejectsMissingIdentityToken() {
        let service = AppleSignInService()

        XCTAssertThrowsError(
            try service.makeLoginRequest(identityTokenData: nil, authorizationCodeData: nil)
        ) { error in
            XCTAssertTrue(error is AppleSignInError)
        }
    }
}

private final class InMemorySessionPersistence: SessionPersistence {
    var storedSession: StoredAuthSession?
    var didClear = false
    private let loadError: Error?

    init(storedSession: StoredAuthSession? = nil, loadError: Error? = nil) {
        self.storedSession = storedSession
        self.loadError = loadError
    }

    func loadStoredSession() throws -> StoredAuthSession? {
        if let loadError {
            throw loadError
        }
        return storedSession
    }

    func save(storedSession: StoredAuthSession) throws {
        self.storedSession = storedSession
    }

    func clear() throws {
        didClear = true
        storedSession = nil
    }
}

private enum TestPersistenceError: Error {
    case corruptedData
}

private func makeSession(expiresIn: Int) -> AuthSession {
    AuthSession(
        userId: "user-1",
        accessToken: "access-token",
        refreshToken: "refresh-token",
        expiresIn: expiresIn,
        isNewUser: false
    )
}

private func fixedNow() -> Date {
    ISO8601DateFormatter().date(from: "2026-05-26T08:00:00Z")!
}
