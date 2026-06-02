import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var environment: AppEnvironment

    var body: some View {
        List {
            Section("Account") {
                NavigationLink("Login Settings") {
                    AuthView()
                }
                NavigationLink("Purchase Settings") {
                    PaymentView()
                }
            }

            Section("App Config") {
                if environment.isLoadingBootstrap {
                    ProgressView("Loading config...")
                } else if let bootstrap = environment.bootstrapConfig {
                    LabeledContent("Product", value: bootstrap.productName)
                    LabeledContent("Login Methods", value: bootstrap.supportedLoginMethods.joined(separator: ", "))
                    LabeledContent("IAP", value: bootstrap.iapEnabled ? "Enabled" : "Disabled")
                    LabeledContent("Review Mode", value: bootstrap.iosReviewMode ? "Enabled" : "Disabled")
                    LabeledContent("Generation Range", value: "\(bootstrap.generation.minImages)-\(bootstrap.generation.maxImages) images")
                } else {
                    Text("Config is not loaded yet.")
                        .foregroundStyle(.secondary)
                }

                if let bootstrapErrorMessage = environment.bootstrapErrorMessage {
                    Text(bootstrapErrorMessage)
                        .foregroundStyle(.red)
                }

                Button("Reload Config") {
                    Task { await environment.reloadBootstrap() }
                }
            }

            Section("Legal") {
                if let bootstrap = environment.bootstrapConfig {
                    if bootstrap.legalDocuments.isEmpty {
                        Text("No legal documents configured.")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(bootstrap.legalDocuments) { document in
                            if let url = externalURL(from: document.url) {
                                Link(document.title, destination: url)
                            } else {
                                LabeledContent(document.title, value: document.url)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                } else if environment.isLoadingBootstrap {
                    ProgressView("Loading legal documents...")
                } else {
                    Text("Legal documents are unavailable until config loads.")
                        .foregroundStyle(.secondary)
                }
            }

            Section("Session") {
                if let userId = environment.sessionStore.currentUserId {
                    LabeledContent("Current User", value: userId)
                } else {
                    Text("Not signed in")
                        .foregroundStyle(.secondary)
                }

                if let loginMethod = environment.sessionStore.currentLoginMethod {
                    LabeledContent("Login Method", value: loginMethod.rawValue)
                }

                if let sessionExpiresAt = environment.sessionStore.sessionExpiresAt {
                    LabeledContent("Expires", value: sessionExpiresAt.formatted(date: .abbreviated, time: .shortened))
                }

                Button("Clear Local Session", role: .destructive) {
                    environment.sessionStore.clear()
                }
            }
        }
        .navigationTitle("Settings")
        .task {
            await environment.loadBootstrapIfNeeded()
        }
    }

    private func externalURL(from rawValue: String) -> URL? {
        guard let url = URL(string: rawValue),
              let scheme = url.scheme?.lowercased(),
              ["http", "https"].contains(scheme),
              url.host != nil else {
            return nil
        }
        return url
    }
}
