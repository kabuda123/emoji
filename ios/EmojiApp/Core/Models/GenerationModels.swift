import Foundation

enum GenerationStatus: String, Codable, CaseIterable {
    case created = "CREATED"
    case auditing = "AUDITING"
    case readyToDispatch = "READY_TO_DISPATCH"
    case running = "RUNNING"
    case postProcessing = "POST_PROCESSING"
    case success = "SUCCESS"
    case failed = "FAILED"
    case refunded = "REFUNDED"

    var displayName: String {
        switch self {
        case .created: return "Created"
        case .auditing: return "Auditing"
        case .readyToDispatch: return "Ready to Dispatch"
        case .running: return "Running"
        case .postProcessing: return "Post Processing"
        case .success: return "Success"
        case .failed: return "Failed"
        case .refunded: return "Refunded"
        }
    }

    var isTerminal: Bool {
        switch self {
        case .success, .failed, .refunded:
            return true
        case .created, .auditing, .readyToDispatch, .running, .postProcessing:
            return false
        }
    }
}

struct CreateGenerationRequest: Encodable {
    let templateId: String
    let inputObjectKey: String
    let count: Int
}

struct CreateGenerationResponse: Decodable {
    let taskId: String
    let status: GenerationStatus
    let pollAfterSeconds: Int
}

struct GenerationDetail: Decodable {
    let taskId: String
    let status: GenerationStatus
    let progressPercent: Int
    let previewUrls: [String]
    let resultUrls: [String]
    let failedReason: String?
    let pollAfterSeconds: Int?
}

struct HistoryItem: Decodable, Identifiable {
    let taskId: String
    let templateName: String
    let status: GenerationStatus
    let coverUrl: String
    let createdAt: Date

    var id: String { taskId }
}

struct DeleteHistoryResponse: Decodable {
    let deleted: Bool
    let historyId: String
}
