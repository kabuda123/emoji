import Foundation

enum LoginMethod: String, Codable {
    case email = "Email"
    case apple = "Apple"
}

struct StoredAuthSession: Codable {
    let session: AuthSession
    let loginMethod: LoginMethod
    let savedAt: Date
}

protocol SessionPersistence {
    func loadStoredSession() throws -> StoredAuthSession?
    func save(storedSession: StoredAuthSession) throws
    func clear() throws
}

@MainActor
final class SessionStore: ObservableObject {
    @Published private(set) var currentSession: AuthSession?
    @Published private(set) var currentLoginMethod: LoginMethod?
    @Published private(set) var sessionExpiresAt: Date?

    private let persistence: SessionPersistence
    private let now: () -> Date

    var accessToken: String? {
        currentSession?.accessToken
    }

    var currentUserId: String? {
        currentSession?.userId
    }

    init(
        persistence: SessionPersistence = KeychainSessionStore(),
        now: @escaping () -> Date = Date.init
    ) {
        self.persistence = persistence
        self.now = now

        guard let storedSession = try? persistence.loadStoredSession() else {
            return
        }
        restore(storedSession: storedSession)
    }

    func update(session: AuthSession, loginMethod: LoginMethod) {
        let savedAt = now()
        currentSession = session
        currentLoginMethod = loginMethod
        sessionExpiresAt = expirationDate(for: session, from: savedAt)

        let storedSession = StoredAuthSession(
            session: session,
            loginMethod: loginMethod,
            savedAt: savedAt
        )
        try? persistence.save(storedSession: storedSession)
    }

    func clear() {
        currentSession = nil
        currentLoginMethod = nil
        sessionExpiresAt = nil
        try? persistence.clear()
    }

    private func restore(storedSession: StoredAuthSession) {
        currentSession = storedSession.session
        currentLoginMethod = storedSession.loginMethod
        sessionExpiresAt = expirationDate(for: storedSession.session, from: storedSession.savedAt)
    }

    private func expirationDate(for session: AuthSession, from startDate: Date) -> Date {
        startDate.addingTimeInterval(TimeInterval(session.expiresIn))
    }
}
