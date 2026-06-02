import SwiftUI

struct TemplateListView: View {
    @EnvironmentObject private var environment: AppEnvironment

    @State private var templates: [TemplateSummary] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if isLoading && templates.isEmpty {
                ProgressView("Loading templates...")
            } else if let errorMessage, templates.isEmpty {
                ContentUnavailableView {
                    Label("Template Load Failed", systemImage: "exclamationmark.triangle")
                } description: {
                    Text(errorMessage)
                } actions: {
                    Button("Retry") {
                        Task { await loadTemplates() }
                    }
                }
            } else if templates.isEmpty {
                ContentUnavailableView {
                    Label("No Templates", systemImage: "square.grid.2x2")
                } description: {
                    Text("No enabled templates are available from the backend.")
                } actions: {
                    Button("Reload") {
                        Task { await loadTemplates() }
                    }
                }
            } else {
                List(templates) { template in
                    NavigationLink {
                        TemplateDetailView(templateID: template.id)
                    } label: {
                        HStack(spacing: 12) {
                            AsyncImage(url: URL(string: template.previewUrl)) { image in
                                image
                                    .resizable()
                                    .scaledToFill()
                            } placeholder: {
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(Color.gray.opacity(0.15))
                            }
                            .frame(width: 68, height: 68)
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                            VStack(alignment: .leading, spacing: 6) {
                                Text(template.name)
                                    .font(.headline)
                                Text(template.styleCode)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                Text("\(template.priceCredits) credits")
                                    .font(.subheadline)
                            }

                            Spacer()

                            if !template.enabled {
                                Text("Disabled")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                .refreshable {
                    await loadTemplates()
                }
            }
        }
        .navigationTitle("Templates")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink("Login") {
                    AuthView()
                }
            }
        }
        .task {
            await environment.loadBootstrapIfNeeded()
            await loadTemplatesIfNeeded()
        }
    }

    @MainActor
    private func loadTemplatesIfNeeded() async {
        guard templates.isEmpty else { return }
        await loadTemplates()
    }

    @MainActor
    private func loadTemplates() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            templates = try await environment.apiClient.get(APIEndpoint.templates)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
