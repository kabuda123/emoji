import AuthenticationServices
import Foundation

final class AppleSignInService {
    func makeLoginRequest(from authorization: ASAuthorization) throws -> AppleLoginRequest {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            throw AppleSignInError.unsupportedCredential
        }

        return try makeLoginRequest(
            identityTokenData: credential.identityToken,
            authorizationCodeData: credential.authorizationCode
        )
    }

    func makeLoginRequest(identityTokenData: Data?, authorizationCodeData: Data?) throws -> AppleLoginRequest {
        guard let identityTokenData,
              let identityToken = String(data: identityTokenData, encoding: .utf8),
              !identityToken.isEmpty else {
            throw AppleSignInError.missingIdentityToken
        }

        let authorizationCode = authorizationCodeData.flatMap {
            String(data: $0, encoding: .utf8)
        }

        return AppleLoginRequest(
            identityToken: identityToken,
            authorizationCode: authorizationCode
        )
    }
}

enum AppleSignInError: LocalizedError {
    case unsupportedCredential
    case missingIdentityToken

    var errorDescription: String? {
        switch self {
        case .unsupportedCredential:
            return "Apple sign in returned an unsupported credential."
        case .missingIdentityToken:
            return "Apple sign in did not return an identity token."
        }
    }
}
