import Foundation

enum APIEndpoint {
    static let bootstrapConfig = "/api/config/bootstrap"
    static let emailSendCode = "/api/auth/email/send-code"
    static let emailLogin = "/api/auth/email/login"
    static let appleLogin = "/api/auth/apple/login"
    static let templates = "/api/templates"
    static let uploadPolicy = "/api/upload/policy"
    static let generations = "/api/generations"
    static let history = "/api/history"
    static let creditBalance = "/api/credits/balance"
    static let iapVerify = "/api/iap/verify"

    static func templateDetail(_ templateID: String) -> String {
        "/api/templates/\(templateID)"
    }

    static func generationDetail(_ taskID: String) -> String {
        "/api/generations/\(taskID)"
    }

    static func historyItem(_ itemID: String) -> String {
        "/api/history/\(itemID)"
    }
}
