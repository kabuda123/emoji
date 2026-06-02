import SwiftUI

struct GenerationView: View {
    @EnvironmentObject private var environment: AppEnvironment

    let templateID: String

    @State private var inputObjectKey: String
    @State private var count = 2
    @State private var createdResponse: CreateGenerationResponse?
    @State private var generationDetail: GenerationDetail?
    @State private var isSubmitting = false
    @State private var isRefreshing = false
    @State private var isPolling = false
    @State private var pollingTask: Task<Void, Never>?
    @State private var errorMessage: String?

    init(templateID: String, initialInputObjectKey: String = "emoji/demo/input.png") {
        self.templateID = templateID
        _inputObjectKey = State(initialValue: initialInputObjectKey)
    }

    var body: some View {
        Form {
            Section("Create Task") {
                TextField("Template ID", text: .constant(templateID))
                    .disabled(true)
                TextField("Input Object Key", text: $inputObjectKey)
                    .textInputAutocapitalization(.never)
                Stepper("Image Count: \(count)", value: $count, in: generationRange)

                Button(isSubmitting ? "Creating..." : "Create Generation Task") {
                    Task { await createGeneration() }
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(isSubmitting || inputObjectKey.isEmpty)
            }

            if let createdResponse {
                Section("Task Summary") {
                    LabeledContent("Task ID", value: createdResponse.taskId)
                    LabeledContent("Status", value: createdResponse.status.displayName)
                    LabeledContent("Poll Interval", value: "\(createdResponse.pollAfterSeconds) sec")

                    if isPolling {
                        ProgressView("Polling task detail...")
                    }

                    HStack {
                        Button(isRefreshing ? "Refreshing..." : "Refresh Now") {
                            Task { await refreshDetail(taskID: createdResponse.taskId) }
                        }
                        .disabled(isRefreshing)

                        Button("Stop Polling", role: .destructive) {
                            stopPolling()
                        }
                        .disabled(!isPolling)
                    }
                }
            }

            if let generationDetail {
                Section("Task Detail") {
                    LabeledContent("Status", value: generationDetail.status.displayName)
                    LabeledContent("Progress", value: "\(generationDetail.progressPercent)%")

                    RemoteImageStrip(title: "Previews", urls: generationDetail.previewUrls)
                    RemoteImageStrip(title: "Results", urls: generationDetail.resultUrls)

                    if let failedReason = generationDetail.failedReason, !failedReason.isEmpty {
                        Text(failedReason)
                            .foregroundStyle(.red)
                    }
                }
            }

            if let errorMessage {
                Section("Error") {
                    Text(errorMessage)
                        .foregroundStyle(.red)

                    if let taskId = createdResponse?.taskId {
                        Button("Retry Refresh") {
                            Task { await refreshDetail(taskID: taskId) }
                        }
                        .disabled(isRefreshing)
                    }
                }
            }
        }
        .navigationTitle("Generation")
        .onDisappear {
            stopPolling()
        }
    }

    private var generationRange: ClosedRange<Int> {
        let minCount = environment.bootstrapConfig?.generation.minImages ?? 2
        let maxCount = environment.bootstrapConfig?.generation.maxImages ?? 4
        return minCount...maxCount
    }

    @MainActor
    private func createGeneration() async {
        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        do {
            let request = CreateGenerationRequest(templateId: templateID, inputObjectKey: inputObjectKey, count: count)
            let headers = ["Idempotency-Key": UUID().uuidString]
            let response: CreateGenerationResponse = try await environment.apiClient.post(
                APIEndpoint.generations,
                body: request,
                headers: headers
            )
            createdResponse = response
            generationDetail = nil
            startPolling(taskID: response.taskId, initialPollAfterSeconds: response.pollAfterSeconds)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func refreshDetail(taskID: String) async {
        isRefreshing = true
        errorMessage = nil
        defer { isRefreshing = false }

        do {
            generationDetail = try await environment.apiClient.get(APIEndpoint.generationDetail(taskID))
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func startPolling(taskID: String, initialPollAfterSeconds: Int) {
        stopPolling()
        isPolling = true

        let service = GenerationPollingService(apiClient: environment.apiClient)
        pollingTask = Task {
            do {
                try await service.poll(taskID: taskID, initialPollAfterSeconds: initialPollAfterSeconds) { detail in
                    await MainActor.run {
                        generationDetail = detail
                    }
                }
                await finishPolling()
            } catch is CancellationError {
                await finishPolling()
            } catch {
                await MainActor.run {
                    errorMessage = error.localizedDescription
                    finishPollingState()
                }
            }
        }
    }

    @MainActor
    private func stopPolling() {
        pollingTask?.cancel()
        finishPollingState()
    }

    @MainActor
    private func finishPolling() {
        finishPollingState()
    }

    @MainActor
    private func finishPollingState() {
        isPolling = false
        pollingTask = nil
    }
}

private struct RemoteImageStrip: View {
    let title: String
    let urls: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)

            if urls.isEmpty {
                Text("No \(title.lowercased()) available yet.")
                    .foregroundStyle(.secondary)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(urls, id: \.self) { urlString in
                            RemoteGenerationImage(urlString: urlString)
                        }
                    }
                }
            }
        }
    }
}

private struct RemoteGenerationImage: View {
    let urlString: String

    var body: some View {
        Group {
            if let url = URL(string: urlString), !urlString.isEmpty {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    case .failure:
                        placeholder
                    case .empty:
                        ProgressView()
                    @unknown default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(width: 96, height: 96)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private var placeholder: some View {
        Image(systemName: "photo")
            .foregroundStyle(.secondary)
    }
}
