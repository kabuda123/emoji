import Foundation

struct BootstrapConfig: Decodable {
    let productName: String
    let iosReviewMode: Bool
    let iapEnabled: Bool
    let supportedLoginMethods: [String]
    let legalDocuments: [LegalDocument]
    let generation: GenerationPolicy
}

struct LegalDocument: Decodable, Identifiable {
    let type: String
    let title: String
    let url: String

    var id: String { type }
}

struct GenerationPolicy: Decodable {
    let minImages: Int
    let maxImages: Int
    let defaultPollSeconds: Int
}