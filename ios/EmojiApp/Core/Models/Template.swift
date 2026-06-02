import Foundation

struct TemplateSummary: Decodable, Identifiable, Hashable {
    let id: String
    let name: String
    let styleCode: String
    let previewUrl: String
    let priceCredits: Int
    let enabled: Bool
}

struct TemplateDetail: Decodable, Identifiable, Hashable {
    let id: String
    let name: String
    let styleCode: String
    let description: String
    let previewUrl: String
    let sampleImages: [String]
    let priceCredits: Int
    let enabled: Bool
    let supportedAspectRatios: [String]
}