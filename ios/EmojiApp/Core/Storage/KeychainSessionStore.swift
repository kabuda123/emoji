import Foundation
import Security

final class KeychainSessionStore: SessionPersistence {
    private let service: String
    private let account: String
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder

    init(
        service: String = Bundle.main.bundleIdentifier ?? "com.kabuda.emojiios",
        account: String = "auth-session"
    ) {
        self.service = service
        self.account = account

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        self.encoder = encoder

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        self.decoder = decoder
    }

    func loadStoredSession() throws -> StoredAuthSession? {
        var query = baseQuery()
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        if status == errSecItemNotFound {
            return nil
        }
        guard status == errSecSuccess else {
            throw KeychainSessionStoreError.unhandledStatus(status)
        }
        guard let data = result as? Data else {
            throw KeychainSessionStoreError.invalidStoredData
        }

        return try decoder.decode(StoredAuthSession.self, from: data)
    }

    func save(storedSession: StoredAuthSession) throws {
        let data = try encoder.encode(storedSession)
        var query = baseQuery()
        query[kSecValueData as String] = data
        query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

        let status = SecItemAdd(query as CFDictionary, nil)
        if status == errSecSuccess {
            return
        }
        if status == errSecDuplicateItem {
            var attributesToUpdate: [String: Any] = [:]
            attributesToUpdate[kSecValueData as String] = data
            attributesToUpdate[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

            let updateStatus = SecItemUpdate(baseQuery() as CFDictionary, attributesToUpdate as CFDictionary)
            guard updateStatus == errSecSuccess else {
                throw KeychainSessionStoreError.unhandledStatus(updateStatus)
            }
            return
        }

        throw KeychainSessionStoreError.unhandledStatus(status)
    }

    func clear() throws {
        let status = SecItemDelete(baseQuery() as CFDictionary)
        if status == errSecSuccess || status == errSecItemNotFound {
            return
        }
        throw KeychainSessionStoreError.unhandledStatus(status)
    }

    private func baseQuery() -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }
}

enum KeychainSessionStoreError: LocalizedError {
    case invalidStoredData
    case unhandledStatus(OSStatus)

    var errorDescription: String? {
        switch self {
        case .invalidStoredData:
            return "Stored session data is invalid."
        case .unhandledStatus(let status):
            return "Keychain operation failed with status \(status)."
        }
    }
}
