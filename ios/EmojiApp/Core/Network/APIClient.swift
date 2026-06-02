import Foundation

struct APIEnvelope<T: Decodable>: Decodable {
    let success: Bool
    let data: T?
    let error: APIErrorPayload?
    let traceId: String?
    let timestamp: Date?
}

final class APIClient {
    var baseURL: URL
    var accessTokenProvider: (() -> String?)?

    private let transport: HTTPTransport
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    init(baseURL: URL = URL(string: "http://localhost:8080")!, transport: HTTPTransport = URLSession.shared) {
        self.baseURL = baseURL
        self.transport = transport

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        self.decoder = decoder

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        self.encoder = encoder
    }

    func get<T: Decodable>(_ path: String) async throws -> T {
        try await request(path: path, method: "GET", body: Optional<EmptyRequest>.none)
    }

    func post<Body: Encodable, T: Decodable>(_ path: String, body: Body, headers: [String: String] = [:]) async throws -> T {
        try await request(path: path, method: "POST", body: body, headers: headers)
    }

    func delete<T: Decodable>(_ path: String) async throws -> T {
        try await request(path: path, method: "DELETE", body: Optional<EmptyRequest>.none)
    }

    private func request<Body: Encodable, T: Decodable>(
        path: String,
        method: String,
        body: Body?,
        headers: [String: String] = [:]
    ) async throws -> T {
        guard let url = URL(string: path, relativeTo: baseURL) else {
            throw APIClientError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        if let token = accessTokenProvider?(), !token.isEmpty {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        headers.forEach { key, value in
            request.setValue(value, forHTTPHeaderField: key)
        }

        if let body {
            request.httpBody = try encoder.encode(body)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        do {
            let (data, response) = try await transport.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIClientError.invalidResponse
            }

            let envelope = try decoder.decode(APIEnvelope<T>.self, from: data)
            if httpResponse.statusCode >= 400 || !envelope.success {
                throw APIClientError.server(envelope.error ?? APIErrorPayload(code: "UNKNOWN", message: "Unknown server error", details: nil))
            }

            guard let payload = envelope.data else {
                throw APIClientError.missingData
            }

            return payload
        } catch let error as APIClientError {
            throw error
        } catch {
            throw APIClientError.transport(error)
        }
    }
}

private struct EmptyRequest: Encodable {}
