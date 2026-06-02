import Foundation

struct AuthSession: Codable {
    let userId: String
    let accessToken: String
    let refreshToken: String
    let expiresIn: Int
    let isNewUser: Bool
}

struct AppleLoginRequest: Encodable {
    let identityToken: String
    let authorizationCode: String?
}

struct EmailSendCodeRequest: Encodable {
    let email: String
    let scene: String
}

struct EmailSendCodeResponse: Decodable {
    let cooldownSeconds: Int
    let maskedDestination: String
}

struct EmailLoginRequest: Encodable {
    let email: String
    let code: String
}