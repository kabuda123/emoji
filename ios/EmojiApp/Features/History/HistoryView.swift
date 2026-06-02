import SwiftUI

struct HistoryView: View {
    @EnvironmentObject private var environment: AppEnvironment

    @State private var historyItems: [HistoryItem] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if isLoading && historyItems.isEmpty {
                ProgressView("Loading history...")
            } else if let errorMessage, historyItems.isEmpty {
                ContentUnavailableView {
                    Label("History Load Failed", systemImage: "exclamationmark.triangle")
                } description: {
                    Text(errorMessage)
                } actions: {
                    Button("Retry") {
                        Task { await loadHistory() }
                    }
                }
            } else if historyItems.isEmpty {
                ContentUnavailableView("No history yet", systemImage: "photo.on.rectangle")
            } else {
                List {
                    ForEach(historyItems) { item in
                        HStack(spacing: 12) {
                            HistoryThumbnail(urlString: item.coverUrl)

                            VStack(alignment: .leading, spacing: 8) {
                                Text(item.templateName)
                                    .font(.headline)
                                Text(item.status.displayName)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                Text(item.createdAt.formatted(date: .abbreviated, time: .shortened))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .swipeActions {
                            Button("Delete", role: .destructive) {
                                Task { await deleteHistory(itemID: item.taskId) }
                            }
                        }
                    }
                }
                .refreshable {
                    await loadHistory()
                }
            }
        }
        .navigationTitle("History")
        .task {
            await loadHistoryIfNeeded()
        }
    }

    @MainActor
    private func loadHistoryIfNeeded() async {
        guard historyItems.isEmpty else { return }
        await loadHistory()
    }

    @MainActor
    private func loadHistory() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            historyItems = try await environment.apiClient.get(APIEndpoint.history)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func deleteHistory(itemID: String) async {
        errorMessage = nil

        do {
            let _: DeleteHistoryResponse = try await environment.apiClient.delete(APIEndpoint.historyItem(itemID))
            historyItems.removeAll { $0.taskId == itemID }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

private struct HistoryThumbnail: View {
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
        .frame(width: 64, height: 64)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private var placeholder: some View {
        Image(systemName: "photo")
            .foregroundStyle(.secondary)
    }
}
