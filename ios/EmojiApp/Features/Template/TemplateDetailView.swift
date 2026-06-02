import SwiftUI

struct TemplateDetailView: View {
    @EnvironmentObject private var environment: AppEnvironment

    let templateID: String

    @State private var template: TemplateDetail?
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if isLoading && template == nil {
                ProgressView("Loading template detail...")
            } else if let template {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        AsyncImage(url: URL(string: template.previewUrl)) { image in
                            image
                                .resizable()
                                .scaledToFill()
                        } placeholder: {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color.gray.opacity(0.15))
                        }
                        .frame(height: 220)
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                        VStack(alignment: .leading, spacing: 8) {
                            Text(template.name)
                                .font(.title2.bold())
                            Text(template.description)
                                .foregroundStyle(.secondary)
                            Text("\(template.priceCredits) credits")
                                .font(.headline)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Supported Ratios")
                                .font(.headline)
                            Text(template.supportedAspectRatios.joined(separator: " / "))
                                .foregroundStyle(.secondary)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Sample Images")
                                .font(.headline)
                            if template.sampleImages.isEmpty {
                                Text("No sample images configured.")
                                    .foregroundStyle(.secondary)
                            } else {
                                ForEach(template.sampleImages, id: \.self) { imageURL in
                                    AsyncImage(url: URL(string: imageURL)) { image in
                                        image
                                            .resizable()
                                            .scaledToFill()
                                    } placeholder: {
                                        RoundedRectangle(cornerRadius: 8)
                                            .fill(Color.gray.opacity(0.15))
                                    }
                                    .frame(height: 140)
                                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                                }
                            }
                        }

                        NavigationLink {
                            UploadView(templateID: template.id, defaultFileName: "\(template.id).png")
                        } label: {
                            Text("Upload Image")
                                .font(.headline)
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(Color.blue)
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }

                        NavigationLink {
                            GenerationView(templateID: template.id)
                        } label: {
                            Text("Create Generation Task")
                                .font(.headline)
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(Color.blue)
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                    }
                    .padding()
                }
            } else if let errorMessage {
                ContentUnavailableView {
                    Label("Template Detail Failed", systemImage: "exclamationmark.triangle")
                } description: {
                    Text(errorMessage)
                } actions: {
                    Button("Retry") {
                        Task { await loadTemplate() }
                    }
                }
            }
        }
        .navigationTitle("Template Detail")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadTemplateIfNeeded()
        }
    }

    @MainActor
    private func loadTemplateIfNeeded() async {
        guard template == nil else { return }
        await loadTemplate()
    }

    @MainActor
    private func loadTemplate() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            template = try await environment.apiClient.get(APIEndpoint.templateDetail(templateID))
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
