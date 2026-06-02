import Foundation

struct ImageUploadInput {
    let data: Data
    let fileName: String
    let contentType: String
}

struct UploadedImage {
    let objectKey: String
    let uploadPolicy: UploadPolicy
}

enum ImageUploadError: LocalizedError {
    case invalidUploadURL
    case unsupportedMethod(String)
    case invalidResponse
    case uploadFailed(statusCode: Int)

    var errorDescription: String? {
        switch self {
        case .invalidUploadURL:
            return "The upload URL is invalid."
        case .unsupportedMethod(let method):
            return "Unsupported upload method: \(method)."
        case .invalidResponse:
            return "The upload response is invalid."
        case .uploadFailed(let statusCode):
            return "Image upload failed with status code \(statusCode)."
        }
    }
}

final class ImageUploadService {
    private let apiClient: APIClient
    private let uploadTransport: HTTPTransport

    init(apiClient: APIClient, uploadTransport: HTTPTransport = URLSession.shared) {
        self.apiClient = apiClient
        self.uploadTransport = uploadTransport
    }

    func uploadImage(_ input: ImageUploadInput) async throws -> UploadedImage {
        let policy: UploadPolicy = try await apiClient.post(
            APIEndpoint.uploadPolicy,
            body: UploadPolicyRequest(fileName: input.fileName, contentType: input.contentType)
        )

        let method = policy.method.uppercased()
        guard method == "PUT" else {
            throw ImageUploadError.unsupportedMethod(policy.method)
        }

        guard let uploadURL = URL(string: policy.uploadUrl) else {
            throw ImageUploadError.invalidUploadURL
        }

        var request = URLRequest(url: uploadURL)
        request.httpMethod = method
        request.httpBody = input.data

        policy.headers.forEach { key, value in
            request.setValue(value, forHTTPHeaderField: key)
        }

        let (_, response) = try await uploadTransport.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw ImageUploadError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            throw ImageUploadError.uploadFailed(statusCode: httpResponse.statusCode)
        }

        return UploadedImage(objectKey: policy.objectKey, uploadPolicy: policy)
    }
}
